package com.learn.java;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class LoadBalancer {
    private final List<Server> servers;
    private final Random random;

    public LoadBalancer(List<Server> servers) {
        this.servers = servers;
        this.random = new Random();
    }

    public Optional<Server> getServer() {
        return servers.stream()
            .min(Comparator.comparingInt(Server::getCurrentConnections));
    }

    public Optional<Server> getRandomServer() {
        if (servers.isEmpty()) {
            return Optional.empty();
        }
        int index = random.nextInt(servers.size());
        return Optional.of(servers.get(index));
    }

    public void connect() {
        Optional<Server> serverOpt = getServer();
        serverOpt.ifPresent(server -> {
            System.out.println("Connecting to server: " + server.getIp() + server.getPort());
            server.incrementConnections();
        });
    }

    public void disconnect(Server server) {
        System.out.println("Disconnecting from server: " + server.getIp() + server.getPort());
        server.decrementConnections();
    }
}
