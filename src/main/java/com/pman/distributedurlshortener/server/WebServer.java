package com.pman.distributedurlshortener.server;

import java.io.IOException;
import java.net.InetSocketAddress;

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
	public WebServer(int port, ZooKeeperClient ZooKeeperClient) throws IOException {
		httpServer = HttpServer.create(new InetSocketAddress(port), 0);
		this.port = port;
		this.ZooKeeperClient = ZooKeeperClient;

		httpServer.createContext(HOME, new HttpResponseHandler(ZooKeeperClient, HOME));
		httpServer.createContext(SERVER_STATUS, new HttpResponseHandler(ZooKeeperClient, SERVER_STATUS));
		httpServer.createContext(ZNODE_INFO, new HttpResponseHandler(ZooKeeperClient, ZNODE_INFO));
		httpServer.createContext(LIST_ALL_ZNODES, new HttpResponseHandler(ZooKeeperClient, LIST_ALL_ZNODES));
		httpServer.createContext(NEW_NEXT, new HttpResponseHandler(ZooKeeperClient, NEW_NEXT));
		httpServer.createContext(SHORTEN, new HttpResponseHandler(ZooKeeperClient, SHORTEN));
	}

	public void start() {
		httpServer.start();
		String serverAddress = "http://" + this.ZooKeeperClient.getState().getIp() + ":" + port;
		System.out.println("Http server started, accepting requests at " + serverAddress);
	}
}
