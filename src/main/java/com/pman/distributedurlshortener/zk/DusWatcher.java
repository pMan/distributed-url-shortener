package com.pman.distributedurlshortener.zk;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

public class DusWatcher implements Watcher {

	private ZooKeeperClient zkClient;
	
	public DusWatcher(ZooKeeperClient client) {
		zkClient = client;
	}
	
	@Override
	public void process(WatchedEvent event) {
		switch (event.getType()) {
		case None:
			if (event.getState() == Event.KeeperState.SyncConnected) {
				System.out.println("Connected to Zookeeper cluster");
			} else {
				synchronized (zkClient.getZooKeeper()) {
					System.out.println("Disconnected from Zookeeper cluster");
					zkClient.getZooKeeper().notifyAll();
				}
			}
			break;
		case NodeDeleted:
			try {
				zkClient.electLeader();
			} catch (InterruptedException e) {
			} catch (KeeperException e) {
			}
			break;
		default:
			System.out.println(event.getType() + " is not handled in switch case");
			break;
		}
	}

}
