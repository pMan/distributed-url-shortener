package com.pman.distributedurlshortener;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.zookeeper.KeeperException;

import com.pman.distributedurlshortener.server.WebServer;
import com.pman.distributedurlshortener.zk.ZooKeeperClient;

public class Application {

    private static Properties properties = new Properties();

    static {
        try {
            properties.load(Application.class.getClassLoader().getResourceAsStream("application.properties"));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) throws IOException, KeeperException, InterruptedException {

        if (args.length == 1) {
            try {
                System.out.println("Loading properties file: " + args[0]);
                properties.load(new FileInputStream(args[0]));
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        System.out.println(
                "Starting Distributed URL shortener!\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        for (Entry<Object, Object> entry : properties.entrySet()) {
            System.out.println(String.format("%-36s", entry.getKey()) + ": " + entry.getValue());
        }

        String hostport = properties.getProperty("zookeeper.hostport");
        int timeout = Integer.parseInt(properties.getProperty("zookeeper.sessiontimeout.ms"));
        String port = properties.getProperty("http.server.port");

        ZooKeeperClient zooKeeperClient = new ZooKeeperClient(hostport, timeout, port);
        WebServer httpServer = new WebServer(port, zooKeeperClient);

        zooKeeperClient.init();
        httpServer.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                httpServer.stop();
                zooKeeperClient.close();
                System.out.println("App shutdown gracefully!");
            }
        });

        zooKeeperClient.waitTillDisconnected();

    }

    public static Properties getProperties() {
        return Application.properties;
    }
}
