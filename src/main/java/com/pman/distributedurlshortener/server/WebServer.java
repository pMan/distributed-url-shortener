package com.pman.distributedurlshortener.server;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.pman.distributedurlshortener.zk.ZKClient;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
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

	public WebServer(int port, ZKClient zkClient) throws IOException {
		httpServer = HttpServer.create(new InetSocketAddress(port), 0);
		this.port = port;

		httpServer.createContext(HOME, new HttpResponseHandler(zkClient, HOME));
		httpServer.createContext(SERVER_STATUS, new HttpResponseHandler(zkClient, SERVER_STATUS));
		httpServer.createContext(ZNODE_INFO, new HttpResponseHandler(zkClient, ZNODE_INFO));
		httpServer.createContext(LIST_ALL_ZNODES, new HttpResponseHandler(zkClient, LIST_ALL_ZNODES));
		httpServer.createContext(NEW_NEXT, new HttpResponseHandler(zkClient, NEW_NEXT));
		httpServer.createContext(SHORTEN, new HttpResponseHandler(zkClient, SHORTEN));
	}

	public HttpContext addContext(String path, HttpHandler handler) {
		return httpServer.createContext(path, handler);
	}

	public void start() {
		httpServer.start();
		System.out.println("Server started. Accepting requests on " + port);
	}
}
