package com.pman.distributedurlshortener.zk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public record NodeState(String ip, String port) implements Serializable {

    public static NodeState fromBytes(byte[] bytes) {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);

        try {
            ObjectInputStream objStream = new ObjectInputStream(byteStream);
            return (NodeState) objStream.readObject();
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
            throw new RuntimeException(e);
        }
        return byteStream.toByteArray();
    }
}
