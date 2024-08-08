package com.pman.distributedurlshortener.zk;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public abstract class Ser implements Serializable {

	private static final long serialVersionUID = 1L;

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
}
