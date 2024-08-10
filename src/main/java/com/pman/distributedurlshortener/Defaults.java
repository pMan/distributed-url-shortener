package com.pman.distributedurlshortener;

import java.util.Properties;

public class Defaults {

    /**
     * comma separated zookeeper hostnames
     */
    static final String ZOOKEEPER_HOST_PORT = "localhost:2181";

    /**
     * this is how long a leader re-election will take
     */
    static final int SESSION_TIMEOUT_MS = 1000;

    /**
     * web server port
     */
    static final String HTTP_SERVER_PORT = "8090";

    public static final String DUS_NAMESPACE = "/DUS";
    public static final String ELECTION_NAMESPACE = DUS_NAMESPACE + "/election";
    public static final String GLOBAL_STATE_ZNODE_NAME = "global_state";
    public static final String STATE_STORE_ZNODE = DUS_NAMESPACE + "/" + GLOBAL_STATE_ZNODE_NAME;
    public static final String ZNODE_PREFIX = ELECTION_NAMESPACE + "/app_";

    public static Properties loadDefault() {
        Properties props = new Properties();

        props.put("zk.hostport", ZOOKEEPER_HOST_PORT);
        props.put("zk.session.timeout", SESSION_TIMEOUT_MS);
        props.put("http.server.port", HTTP_SERVER_PORT);

        return props;
    }
}
