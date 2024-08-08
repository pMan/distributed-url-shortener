package com.pman.distributedurlshortener.zk;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public class LeaderState extends Ser {

	private static final long serialVersionUID = 1L;
	
	AtomicLong nextAvailable;
	
	/**
	 * initialize with start, end and batch size
	 * 
	 * @param s start
	 * @param e end
	 * @param batch batch size
	 */
	public LeaderState(long initialValue) {
		nextAvailable = new AtomicLong(initialValue);
	}
	
	/**
	 * atomically update the start value of the range and return the range [start, end)
	 * 
	 * @return
	 */
	public long next(int size) {
		long curStart = nextAvailable.get();
		long newStart = curStart + size;
		
		while (!nextAvailable.compareAndSet(curStart, newStart)) {
			LockSupport.parkNanos(1);
			curStart = nextAvailable.get();
			newStart = curStart + size;
		}
		return curStart;
	}
	
	public static LeaderState fromBytes(byte[] bytes) {
		ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);

	    try {
	    	ObjectInputStream objStream = new ObjectInputStream(byteStream);
			return (LeaderState) objStream.readObject();
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}

}
