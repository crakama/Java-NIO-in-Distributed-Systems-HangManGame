package com.crakama.client.net;

import java.nio.channels.SocketChannel;

public class ClientInterestOPs {
    public SocketChannel socketChannel;
    public int opsType;
    public int ops;

    public ClientInterestOPs(SocketChannel socketChannel, int opsType, int ops){
        this.socketChannel = socketChannel;
        this.opsType = opsType;
        this.ops = ops;
    }
}
