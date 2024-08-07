package com.pman.distributedurlshortener.zk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ByteArraySerde {

	public static byte[] mapToBytes(State state) {
	    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
	    try {
	    	ObjectOutputStream objStream = new ObjectOutputStream(byteStream);
			objStream.writeObject(state);
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}

	    return byteStream.toByteArray();
	}

	@SuppressWarnings("unchecked")
	public static State bytesToMap(byte[] bytes) {
	    ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);

	    try {
	    	ObjectInputStream objStream = new ObjectInputStream(byteStream);
			return (State) objStream.readObject();
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}
}
