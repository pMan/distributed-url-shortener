package com.pman.distributedurlshortener.zk;

import java.util.List;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;

public interface IZKClient {

    public void electLeader() throws KeeperException, InterruptedException;

    public List<LocalState> getAllZnodes();

    public LocalState getState();

    public ZooKeeper getZooKeeper();

    public void close();
}
