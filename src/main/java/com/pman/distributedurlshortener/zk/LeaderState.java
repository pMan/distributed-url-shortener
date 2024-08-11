package com.pman.distributedurlshortener.zk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class LeaderState implements Serializable {

    private static final long serialVersionUID = 1L;

    long nextAvailable;

    /**
     * initialize with start, end and batch size
     * 
     * @param initialValue initialValue
     */
    public LeaderState(long initialValue) {
        nextAvailable = initialValue;
    }

    /**
     * update the start value of the range and return the range [start, end)
     * 
     * @return
     */
    public long next(int batchSize) {
        long curStart = nextAvailable;
        nextAvailable += batchSize;

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
