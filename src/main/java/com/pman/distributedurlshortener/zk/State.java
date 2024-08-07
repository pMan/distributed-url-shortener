package com.pman.distributedurlshortener.zk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class State implements Serializable {

	private static final long serialVersionUID = 1L;
	
	String ip, port, path;

	public State(String ip, String port) {
		this.ip = ip;
		this.port = port;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
	
	public byte[] toBytes() {
	    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
	    try {
	    	ObjectOutputStream objStream = new ObjectOutputStream(byteStream);
			objStream.writeObject(this);
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}
	    return byteStream.toByteArray();
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
