package com.pman.distributedurlshortener;

import java.io.FileInputStream;
import java.io.IOException;
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
        properties.entrySet().forEach(e -> printFormatted(e.getKey().toString(), e.getValue().toString()));

        String hostport = properties.getProperty("zookeeper.hostport");
        int timeout = Integer.parseInt(properties.getProperty("zookeeper.sessiontimeout.ms"));
        String port = properties.getProperty("http.server.port");
        String redirectHostname = properties.getProperty("app.hostname", "http://localhost") + ":" + port;

        ZooKeeperClient zooKeeperClient = new ZooKeeperClient(hostport, timeout, port);
        WebServer httpServer = new WebServer(port, redirectHostname, zooKeeperClient);

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

    public static Properties getPostgres() {
        Properties dbProperties = new Properties();
        properties.entrySet().stream().filter(key -> key.toString().startsWith("sql."))
                .forEach(item -> dbProperties.put(item.getKey(), item.getValue()));
        return dbProperties;
    }

    private static void printFormatted(String key, String val) {
        val = key.toString().endsWith("password") ? "[redacted]" : val;
        System.out.println(String.format("%-36s", key) + ": " + val);
    }
}
