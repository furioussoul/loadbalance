package com.learn.java;

import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    private final String ip;
    private final int port;
    private final AtomicInteger currentConnections;

    public Server(String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.currentConnections = new AtomicInteger(0);
    }

    public String getAddress() {
        return ip + ":" + port;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public int getCurrentConnections() {
        return currentConnections.get();
    }

    public void incrementConnections() {
        currentConnections.incrementAndGet();
    }

    public void decrementConnections() {
        currentConnections.decrementAndGet();
    }
}
