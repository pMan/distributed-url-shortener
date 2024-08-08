package com.pman.distributedurlshortener.zk;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

public class State extends Ser {

	private static final long serialVersionUID = 1L;
	
	String ip, port, path;

	/**
	 * constructor
	 * 
	 * @param ip
	 * @param port
	 */
	public State(String ip, String port) {
		this.ip = ip;
		this.port = port;
	}

	public String getIp() {
		return ip;
	}

	public String getPort() {
		return port;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public static State fromBytes(byte[] bytes) {
		ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);

	    try {
	    	ObjectInputStream objStream = new ObjectInputStream(byteStream);
			return (State) objStream.readObject();
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}
}
