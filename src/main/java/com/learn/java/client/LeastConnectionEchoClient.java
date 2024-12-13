package com.learn.java.client;

import com.learn.java.LoadBalancer;
import com.learn.java.Server;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class LeastConnectionEchoClient {
    private static final Logger logger = LoggerFactory.getLogger(LeastConnectionEchoClient.class);
    private final LoadBalancer loadBalancer;
    private final EventLoopGroup group;
    private final Bootstrap bootstrap;
    private final AtomicInteger requestCount = new AtomicInteger(0);

    public LeastConnectionEchoClient(List<Server> servers) {
        this.loadBalancer = new LoadBalancer(servers);
        this.group = new NioEventLoopGroup();
        this.bootstrap = new Bootstrap();
        bootstrap.group(group)
            .channel(NioSocketChannel.class)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) {
                    ch.pipeline().addLast(new StringDecoder(), new StringEncoder(), new EchoClientHandler());
                }
            });
    }

    public void start(int numRequests) {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CompletionService<Long> completionService = new ExecutorCompletionService<>(executor);

        for (int i = 0; i < numRequests; i++) {
            completionService.submit(() -> {
                long requestStartTime = System.currentTimeMillis();
                sendRequest();
                long requestEndTime = System.currentTimeMillis();
                return requestEndTime - requestStartTime;
            });
        }

        long totalTime = 0;
        try {
            for (int i = 0; i < numRequests; i++) {
                Future<Long> future = completionService.take();
                totalTime += future.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error during request execution", e);
        } finally {
            executor.shutdown();
            group.shutdownGracefully();
        }

        logger.info("Total time for {} requests: {} ms", numRequests, totalTime);
    }


    private void sendRequest() {
        Optional<Server> serverOpt = loadBalancer.getServer();
        if (!serverOpt.isPresent()) {
            logger.warn("No available servers.");
            return;
        }

        Server server = serverOpt.get();
        Channel channel = null;
        try {
            // Synchronously connect to the server
            channel = bootstrap.connect(server.getIp(), server.getPort()).sync().channel();
            server.incrementConnections();

            long startTime = System.currentTimeMillis();

            // Send the request and wait for the response synchronously
            ChannelFuture future = channel.writeAndFlush("Your request message").sync();

            // Wait until the connection is closed
            future.channel().closeFuture().sync();

            long endTime = System.currentTimeMillis();
            logger.info("Request to server {} took {} ms", server.getIp(), (endTime - startTime));
        } catch (Exception e) {
            logger.error("Connection error", e);
        } finally {
            if (channel != null) {
                server.decrementConnections();
            }
        }
    }

    public static void main(String[] args) {
        List<Server> servers = Arrays.asList(
            new Server("127.0.0.1", 8081),
            new Server("127.0.0.1", 8082),
            new Server("127.0.0.1", 8083)
        );

        LeastConnectionEchoClient client = new LeastConnectionEchoClient(servers);
        client.start(10000);
    }
}
