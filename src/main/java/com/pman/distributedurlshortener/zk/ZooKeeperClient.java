package com.pman.distributedurlshortener.zk;

import static com.pman.distributedurlshortener.Defaults.DUS_NAMESPACE;
import static com.pman.distributedurlshortener.Defaults.ELECTION_NAMESPACE;
import static com.pman.distributedurlshortener.Defaults.STATE_STORE_ZNODE;
import static com.pman.distributedurlshortener.Defaults.ZNODE_PREFIX;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

public class ZooKeeperClient implements IZKClient {

    private ZooKeeper zooKeeper;
    private DusWatcher watcher;
    private String webServerPort;

    private String nodeName;
    private NodeState state;

    private final int BATCH_SIZE = 100;
    private AtomicLong hashNumber = new AtomicLong(0L);
    private long upperLimitOfHash;

    private ReentrantLock lock = new ReentrantLock();

    public ZooKeeperClient(String zkHostPort, int sessionTimeout, String webServerPort)
            throws IOException, KeeperException, InterruptedException {
        watcher = new DusWatcher(this);
        zooKeeper = new ZooKeeper(zkHostPort, sessionTimeout, watcher);
        this.webServerPort = webServerPort;
    }

    /**
     * initializes this node
     * 
     * @throws KeeperException
     * @throws InterruptedException
     */
    public void initZKClient() throws KeeperException, InterruptedException {
        state = createState();
        createZnode();
        electLeader();

        updateCurrentHash();
        upperLimitOfHash = hashNumber.get() + BATCH_SIZE;
    }

    /**
     * create a znode for this instance
     * 
     * @throws KeeperException
     * @throws InterruptedException
     */
    private void createZnode() throws KeeperException, InterruptedException {

        if (null == zooKeeper.exists(DUS_NAMESPACE, false)) {
            String rootNode = zooKeeper.create(DUS_NAMESPACE, new byte[] {}, ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.PERSISTENT);
            System.out.println("Root znode created: " + rootNode);
        }

        if (null == zooKeeper.exists(ELECTION_NAMESPACE, false)) {
            String electionNode = zooKeeper.create(ELECTION_NAMESPACE, new byte[] {}, ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.PERSISTENT);
            System.out.println("Election namespace created: " + electionNode);
        }

        byte[] data = state.toBytes();
        String znodeFullPath = zooKeeper.create(ZNODE_PREFIX, data, ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL_SEQUENTIAL);

        System.out.println("znode created: " + znodeFullPath);
        nodeName = znodeFullPath.replace(ELECTION_NAMESPACE + "/", "");
    }

    /**
     * leader is the znode with smallest id. every node is watching the predecessor
     * znode except the leader.
     * 
     * @throws KeeperException
     * @throws InterruptedException
     */
    @Override
    public void electLeader() throws KeeperException, InterruptedException {
        Stat predecessorStat = null;
        String predecessorZnodeName = "";
        while (predecessorStat == null) {
            List<String> children = zooKeeper.getChildren(ELECTION_NAMESPACE, false);

            Collections.sort(children);
            String smallestChild = children.get(0);

            if (smallestChild.equals(nodeName)) {
                System.out.println("I've been selected as the leader");
                initLeaderRoles();
                return;
            } else {
                int predecessorIndex = Collections.binarySearch(children, nodeName) - 1;
                predecessorZnodeName = children.get(predecessorIndex);
                predecessorStat = zooKeeper.exists(ELECTION_NAMESPACE + "/" + predecessorZnodeName, watcher);
                System.out.println("Watching znode " + predecessorZnodeName);
            }
        }
    }

    /**
     * initialize leader responsibilities
     * 
     * @throws KeeperException
     * @throws InterruptedException
     */
    private void initLeaderRoles() throws KeeperException, InterruptedException {
        createLeaderStateZnode();
    }

    /**
     * creates a leader znode if not present already.
     * 
     * @return Stat stat
     * @throws InterruptedException
     * @throws KeeperException      if node already exists
     */
    private Stat createLeaderStateZnode() throws KeeperException, InterruptedException {
        Stat stat = znodeExists(STATE_STORE_ZNODE);
        if (null != stat)
            return stat;

        LeaderState leaderState = new LeaderState(0L);
        String rootNode = zooKeeper.create(STATE_STORE_ZNODE, leaderState.toBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT);
        System.out.println("Leader node created: " + rootNode);
        return znodeExists(STATE_STORE_ZNODE);
    }

    /**
     * 
     * TODO: this task should be exposed by a RESTful endpoint on leader instance
     * and all non-leader nodes should invoke an http request to get the next range
     * 
     * only leader should respond to calls to this method. Currently, all instances
     * can invoke this method on them, but with a synchronized state update of the
     * leader znode on zookeeper cluster
     * 
     * @return long number
     */
    private void updateCurrentHash() {
        long next = getGlobalHash();
        this.hashNumber.set(next);
        this.upperLimitOfHash = next + BATCH_SIZE;
    }

    /**
     * find the next available long that's unique across all apps in the cluster.
     * leader znode keeps track of it
     * 
     * @return
     */
    private long getGlobalHash() {
        Stat stat = znodeExists(STATE_STORE_ZNODE);
        boolean updateSucceeded = false;
        long newNext = 0L;
        do {
            try {
                byte[] data = zooKeeper.getData(STATE_STORE_ZNODE, false, stat);
                LeaderState leaderState = LeaderState.fromBytes(data);
                newNext = leaderState.next(BATCH_SIZE);

                zooKeeper.setData(STATE_STORE_ZNODE, leaderState.toBytes(), stat.getVersion());
                updateSucceeded = true;
                System.out.println("Next available number: " + newNext);
            } catch (KeeperException e) {
                System.out.println("Error: " + e.code().toString());
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (!updateSucceeded)
                LockSupport.parkNanos(10);

        } while (updateSucceeded == false);
        return newNext;
    }

    /**
     * get the next long from the range of available numbers. if current number is
     * the last one in the given range, calculate next range of numbers and return
     * current.
     * 
     * @return long number
     */
    public long getCurrentHash() {
        long cur = hashNumber.getAndIncrement();
        if (cur < upperLimitOfHash)
            return cur;

        lock.lock();

        if (hashNumber.get() >= upperLimitOfHash)
            this.updateCurrentHash();

        lock.unlock();

        return getCurrentHash();
    }

    private Stat znodeExists(String path) {
        try {
            return zooKeeper.exists(path, false);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * get all znodes registered under the leader election namespace
     * 
     * @return
     */
    @Override
    public List<NodeState> getAllZnodes() {
        try {
            List<NodeState> list = new ArrayList<>();
            List<String> children = zooKeeper.getChildren(ELECTION_NAMESPACE, false);
            System.out.println("Number of active nodes: " + children.size());

            for (String child : children) {

                Stat stat = zooKeeper.exists(ELECTION_NAMESPACE + "/" + child, false);
                byte[] data = zooKeeper.getData(ELECTION_NAMESPACE + "/" + child, false, stat);

                NodeState savedState = NodeState.fromBytes(data);

                list.add(savedState);
            }
            return list;
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * State of the ZNode
     * 
     * @return
     */
    private NodeState createState() {
        String address = "localhost";

        try {
            address = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
        }
        return new NodeState(address, webServerPort);
    }

    @Override
    public NodeState getState() {
        return state;
    }

    @Override
    public ZooKeeper getZooKeeper() {
        return zooKeeper;
    }

    @Override
    public void close() {
        try {
            zooKeeper.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * main thread waits on zookeeper object until Disconnected Event
     * {@code Event.KeeperState.SyncConnected} is received from the zookeeper
     * cluster
     */
    public void waitTillDisconnected() {
        synchronized (zooKeeper) {
            while (true) {
                try {
                    zooKeeper.wait();
                } catch (InterruptedException e) {
                    continue;
                }
                break;
            }
        }
    }

}
