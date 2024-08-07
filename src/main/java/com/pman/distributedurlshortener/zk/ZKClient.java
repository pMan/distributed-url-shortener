package com.pman.distributedurlshortener.zk;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.LockSupport;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

public class ZKClient implements Watcher {

	private static final String DUS_NAMESPACE = "/DUS";
	private static final String ELECTION_NAMESPACE = DUS_NAMESPACE + "/election";
	private static final String LEADER_ZNODE_NAME = "leader";
	private static final String LEADER_NODE = DUS_NAMESPACE + "/" + LEADER_ZNODE_NAME;
	private static final String znodePrefix = ELECTION_NAMESPACE + "/app_";
	
	private ZooKeeper zooKeeper;
	private String zkHostPort;
	private int sessionTimeout;
	
	private static String webServerPort; 
	
	private String nodeName;
	private State state;
	private boolean isLeader;
	
	private final int BATCH_SIZE = 100;
	private long[] hashRange = new long[] {0, 0};
	
	public ZKClient(String zkHostPort, int timeout, int webServerPort) throws IOException, KeeperException, InterruptedException {
		this.zkHostPort = zkHostPort;
		this.sessionTimeout = timeout;
		this.webServerPort = "" + webServerPort;
		
		isLeader = false;

		zooKeeper = new ZooKeeper(this.zkHostPort, this.sessionTimeout, this);
		
		state = createState();
		createZnode();
		electLeader();

		hashRange = getNewRange();
	}

	/**
	 * create a znode for this instance
	 * 
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public void createZnode() throws KeeperException, InterruptedException {
		
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
		String znodeFullPath = zooKeeper.create(znodePrefix, data, ZooDefs.Ids.OPEN_ACL_UNSAFE,
				CreateMode.EPHEMERAL_SEQUENTIAL);

		System.out.println("znode name " + znodeFullPath);
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
				System.out.println("I am the leader");
				isLeader = true;
				initLeaderRoles();
				return;
			} else {
				isLeader = false;
				System.out.println("I am not the leader");
				int predecessorIndex = Collections.binarySearch(children, nodeName) - 1;
				predecessorZnodeName = children.get(predecessorIndex);
				predecessorStat = zooKeeper.exists(ELECTION_NAMESPACE + "/" + predecessorZnodeName, this);
			}
		}
		System.out.println("Watching znode " + predecessorZnodeName);
	}

	private void initLeaderRoles() throws KeeperException, InterruptedException {
		createLeaderZnode();
	}
	
	/**
	 * creates a leader ZNode.
	 * 
	 * @return Stat stat
	 * @throws InterruptedException 
	 * @throws KeeperException if node already exists
	 */
	Stat createLeaderZnode() throws KeeperException, InterruptedException {
		LeaderState leaderState = new LeaderState(0L);
		String rootNode = zooKeeper.create(LEADER_NODE, leaderState.toBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE,
				CreateMode.PERSISTENT);
		System.out.println("Leader node created: " + rootNode);
		return nodeExists(LEADER_NODE);
	}
	
	/**
	 * only leader will respond to calls to this method. clients should find out the leader
	 * and invoke this method on leader.
	 * 
	 * TODO: this task should be exposed by a RESTful endpoint on leader node and all non-leader nodes should 
	 * invoke an http request to get the next range
	 * 
	 * @return
	 */
	public long[] getNewRange() {
		long[] newRange = new long[] { 0, 0 };
		Stat stat = nodeExists(LEADER_NODE);
		boolean updateSucceeded = false;
		do {
			try {
				byte[] data = zooKeeper.getData(LEADER_NODE, false, stat);
				LeaderState leaderState = LeaderState.fromBytes(data);
				newRange = leaderState.next(BATCH_SIZE);

				zooKeeper.setData(LEADER_NODE, leaderState.toBytes(), stat.getVersion());
				updateSucceeded = true;
				System.out.println("Update succes, next available range: " + newRange[0] + " - " + newRange[1]);
			} catch (KeeperException e) {
				System.out.println("Error: " + e.code().toString());
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			if (!updateSucceeded)
				LockSupport.parkNanos(10);

		} while (updateSucceeded == false);
		return newRange;
	}
	
	private Stat nodeExists(String path) {
		try {
			return zooKeeper.exists(path, false);
		} catch (KeeperException | InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public State getNodeState() {
		return state;
	}
	
	/**
	 * get all ZNodes registered under the leader election namespace
	 * 
	 * @return
	 */
	public List<State> znodes() {
		try {
			List<State> list = new ArrayList<>();
			
			List<String> children = zooKeeper.getChildren(ELECTION_NAMESPACE, false);
			for (String child: children) {
				System.out.println("Child node: " + child);
				
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
	
	@Override
	public void process(WatchedEvent event) {
		{
			switch (event.getType()) {
			case None:
				if (event.getState() == Event.KeeperState.SyncConnected) {
					System.out.println("Successfully connected to Zookeeper");
				} else {
					synchronized (zooKeeper) {
						this.state = null;
						System.out.println("Disconnected from Zookeeper event");
						zooKeeper.notifyAll();
					}
				}
				break;
			case NodeDeleted:
				try {
					electLeader();
				} catch (InterruptedException e) {
				} catch (KeeperException e) {
				}

			default:
				System.out.println(event.getPath() + " is not handled in switch case");
				break;
			}
		}
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
