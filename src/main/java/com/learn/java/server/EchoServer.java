package com.learn.java.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EchoServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(EchoServer.class);
    private final int port;
    private final int delay;

    public EchoServer(int port, int delay) {
        this.port = port;
        this.delay = delay;
    }

    public void start() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new StringDecoder());
                        p.addLast(new StringEncoder());
                        p.addLast(new EchoServerHandler(delay));
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = b.bind(port).sync();
            LOGGER.info("Server started and listening on {}", f.channel().localAddress());
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // 启动三个不同负载的服务器
        new Thread(() -> {
            try {
                new EchoServer(8081, 100).start(); // small
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }).start();

        new Thread(() -> {
            try {
                new EchoServer(8082, 50).start(); // medium
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }).start();

        new Thread(() -> {
            try {
                new EchoServer(8083, 33).start(); // large
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }).start();
    }
}
