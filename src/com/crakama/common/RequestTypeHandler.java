package com.crakama.common;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class RequestTypeHandler {
    public SocketChannel socketChannel;
    public int opsType;
    public int ops;

    public RequestTypeHandler(SocketChannel socketChannel, int opsType, int ops){
        this.socketChannel = socketChannel;
        this.opsType = opsType;
        this.ops = ops;
    }
}
