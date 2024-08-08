package com.pman.distributedurlshortener.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import com.pman.distributedurlshortener.zk.ZooKeeperClient;
import com.sun.net.httpserver.HttpServer;

public class WebServer {

	static final String HOME = "/";
	static final String SERVER_STATUS = "/status";
	static final String ZNODE_INFO = "/info";
	static final String LIST_ALL_ZNODES = "/nodes";
	static final String NEW_NEXT = "/next";
	static final String SHORTEN = "/shorten";

	private HttpServer httpServer;
	private int port;
	private ZooKeeperClient ZooKeeperClient;
	
	/**
	 * constructor
	 * 
	 * @param port
	 * @param ZooKeeperClient
	 * @throws IOException
	 */
	public WebServer(String port, ZooKeeperClient ZooKeeperClient) throws IOException {
		this.port = Integer.parseInt(port);
		httpServer = HttpServer.create(new InetSocketAddress(this.port), 0);
		
		this.ZooKeeperClient = ZooKeeperClient;
		HttpResponseHandler httpResponseHandler = new HttpResponseHandler(ZooKeeperClient);
		
		httpServer.createContext(HOME, httpResponseHandler);
		httpServer.createContext(SERVER_STATUS, httpResponseHandler);
		httpServer.createContext(ZNODE_INFO, httpResponseHandler);
		httpServer.createContext(LIST_ALL_ZNODES, httpResponseHandler);
		httpServer.createContext(NEW_NEXT, httpResponseHandler);
		httpServer.createContext(SHORTEN, httpResponseHandler);
		
		httpServer.setExecutor(Executors.newFixedThreadPool(4));
	}

	public void start() {
		httpServer.start();
		String serverAddress = "http://" + this.ZooKeeperClient.getState().getIp() + ":" + port;
		System.out.println("Http server started, accepting requests at " + serverAddress);
	}
}
