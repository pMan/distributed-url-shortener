package com.pman.distributedurlshortener;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.zookeeper.KeeperException;

import com.pman.distributedurlshortener.server.WebServer;
import com.pman.distributedurlshortener.zk.ZooKeeperClient;

public class Main {
    
	public static void main(String[] args) throws IOException, KeeperException, InterruptedException {

		Properties props = Defaults.loadDefault();
		
		if (args.length == 1) {
			try {
				System.out.println(args[0]);
				props.load(new FileInputStream(args[0]));
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}

		
		ZooKeeperClient zkClient = new ZooKeeperClient (props.getProperty("zkHostPort"), 
				(int) props.get("zkSessionTimeOut"),
				Integer.valueOf((String) props.get("httpServerPort")));

		WebServer httpServer = new WebServer(Integer.valueOf((String) props.get("httpServerPort")), zkClient);
		
		httpServer.start();
		
		zkClient.waitTillDisconnected();
	}

}
