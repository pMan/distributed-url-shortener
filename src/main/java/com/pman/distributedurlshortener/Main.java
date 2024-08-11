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

        String hostport = props.getProperty("zk.hostport");
        int timeout = (int) props.get("zk.session.timeout");
        String port = (String) props.get("http.server.port");

        ZooKeeperClient zooKeeperClient = new ZooKeeperClient(hostport, timeout, port);
        WebServer httpServer = new WebServer(port, zooKeeperClient);

        zooKeeperClient.initZKClient();
        httpServer.start();
        zooKeeperClient.waitTillDisconnected();
    }

}
