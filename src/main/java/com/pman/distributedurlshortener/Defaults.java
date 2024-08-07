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
    
    public static Properties loadDefault() {
		Properties props = new Properties();
		
		props.put("zkHostPort", ZOOKEEPER_HOST_PORT);
		props.put("zkSessionTimeOut", SESSION_TIMEOUT_MS);
		props.put("httpServerPort", HTTP_SERVER_PORT);
		
		return props;
	}
}
