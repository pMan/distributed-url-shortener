package com.pman.distributedurlshortener.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;

import com.pman.distributedurlshortener.zk.ZooKeeperClient;
import com.sun.net.httpserver.HttpServer;

public class WebServer {

    private HttpServer httpServer;
    private int port;
    private String address;

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

        System.out.println("HTTP Server created");
        HttpResponseHandler httpResponseHandler = new HttpResponseHandler(ZooKeeperClient, this);

        address = "localhost";
        try {
            address = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
        }

        httpServer.createContext("/", httpResponseHandler);

        httpServer.setExecutor(Executors.newFixedThreadPool(4));
    }

    public String getAddress() {
        return "http://" + address + ":" + port;
    }

    public void stop() {
        System.out.println("stopping Http Server");
        httpServer.stop(3);
    }

    public void start() {
        httpServer.start();
        System.out.println("Http server started, accepting connections at " + getAddress());
    }
}
