package com.pman.distributedurlshortener.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import com.pman.distributedurlshortener.zk.ZooKeeperClient;
import com.sun.net.httpserver.HttpServer;

public class WebServer {

    private HttpServer httpServer;
    private int port;
    private String redirectHostname;

    /**
     * constructor
     * 
     * @param port
     * @param ZooKeeperClient
     * @throws IOException
     */
    public WebServer(String port, String redirectHostname, ZooKeeperClient ZooKeeperClient) throws IOException {
        this.port = Integer.parseInt(port);
        httpServer = HttpServer.create(new InetSocketAddress(this.port), 0);
        this.redirectHostname = redirectHostname;

        System.out.println("HTTP Server created");
        HttpResponseHandler httpResponseHandler = new HttpResponseHandler(ZooKeeperClient, redirectHostname);

        httpServer.createContext("/", httpResponseHandler);

        httpServer.setExecutor(Executors.newFixedThreadPool(4));
    }

    public void stop() {
        System.out.println("stopping Http Server");
        httpServer.stop(3);
    }

    public void start() {
        httpServer.start();
        System.out.println("Http server started, accepting connections at " + redirectHostname);
    }
}
