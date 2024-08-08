package com.pman.distributedurlshortener.zk;

import static com.pman.distributedurlshortener.Defaults.DUS_NAMESPACE;
import static com.pman.distributedurlshortener.Defaults.ELECTION_NAMESPACE;
import static com.pman.distributedurlshortener.Defaults.LEADER_NODE;
import static com.pman.distributedurlshortener.Defaults.ZNODE_PREFIX;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

public class ZooKeeperClient {

	private ZooKeeper zooKeeper;
	private DusWatcher watcher;
	private String webServerPort; 
	
	private String nodeName;
	private State state;
	
	private final int BATCH_SIZE = 100;
	private AtomicLong next= new AtomicLong(0L);
	private long max;
	
	public ZooKeeperClient(String zkHostPort, int sessionTimeout, String webServerPort) throws IOException, KeeperException, InterruptedException {
		watcher = new DusWatcher(this);
		zooKeeper = new ZooKeeper(zkHostPort, sessionTimeout, watcher);
		this.webServerPort =  webServerPort;
		
		state = createState();
		createZnode();
		electLeader();

		updateNext();
		max = next.get() + BATCH_SIZE;
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
		this.state.setPath(znodeFullPath);
		nodeName = znodeFullPath.replace(ELECTION_NAMESPACE + "/", "");
	}

	/**
	 * leader is the znode with smallest id. every node is watching the predecessor znode
	 * except the leader.
	 * 
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
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
		createLeaderZnode();
	}
	
	/**
	 * creates a leader znode if not present.
	 * 
	 * @return Stat stat
	 * @throws InterruptedException 
	 * @throws KeeperException if node already exists
	 */
	Stat createLeaderZnode() throws KeeperException, InterruptedException {
		Stat stat = nodeExists(LEADER_NODE);
		if (null != stat)
			return stat;
		
		LeaderState leaderState = new LeaderState(0L);
		String rootNode = zooKeeper.create(LEADER_NODE, leaderState.toBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE,
				CreateMode.PERSISTENT);
		System.out.println("Leader node created: " + rootNode);
		return nodeExists(LEADER_NODE);
	}
	
	/**
	 * 
	 * TODO: this task should be exposed by a RESTful endpoint on leader instance and all non-leader nodes
	 * should invoke an http request to get the next range
	 * 
	 * only leader should respond to calls to this method. Currently, all instances can invoke this method
	 * on them, but with a synchronized state update of the leader znode on zookeeper cluster
	 * 
	 * @return long number
	 */
	public void updateNext() {
		long next = getGlobalNext();
		this.next.set(next);
		this.max = next + BATCH_SIZE;
	}

	/**
	 * find the next available long that's unique across all apps in the cluster. leader znode keeps track of it
	 * 
	 * @return
	 */
	public long getGlobalNext() {
		Stat stat = nodeExists(LEADER_NODE);
		boolean updateSucceeded = false;
		long newNext = 0L;
		do {
			try {
				byte[] data = zooKeeper.getData(LEADER_NODE, false, stat);
				LeaderState leaderState = LeaderState.fromBytes(data);
				newNext = leaderState.next(BATCH_SIZE);

				zooKeeper.setData(LEADER_NODE, leaderState.toBytes(), stat.getVersion());
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
	 * get the next long from the range of available numbers. if current number is the last one
	 * in the given range, calculate next range of numbers and return current.
	 * 
	 * @return long number
	 */
	public long getNext() {
		long cur = next.getAndIncrement();
		if (cur < max)
			return cur;
		
		this.updateNext();
		return getNext();
	}

	private Stat nodeExists(String path) {
		try {
			return zooKeeper.exists(path, false);
		} catch (KeeperException | InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public State getState() {
		return state;
	}
	
	public ZooKeeper getZooKeeper() {
		return zooKeeper;
	}
	
	/**
	 * get all znodes registered under the leader election namespace
	 * 
	 * @return
	 */
	public List<State> znodes() {
		try {
			List<State> list = new ArrayList<>();
			List<String> children = zooKeeper.getChildren(ELECTION_NAMESPACE, false);
			System.out.println("Number of active nodes: " + children.size());
			
			for (String child: children) {
				
				Stat stat = zooKeeper.exists(ELECTION_NAMESPACE + "/" + child, false);
				byte[] data = zooKeeper.getData(ELECTION_NAMESPACE + "/" + child, false, stat);
				
				State savedState = State.fromBytes(data);
				savedState.setPath(ELECTION_NAMESPACE + "/" + child);
				
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
	private State createState() {
		String address = "localhost";
		
		try {
			address = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) { }
		return new State(address, webServerPort);
	}
	
	public void close() {
		try {
			zooKeeper.close();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * main thread waits on zookeeper object until Disconnected Event {@code Event.KeeperState.SyncConnected}
	 * is received from the zookeeper cluster
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
