package com.crakama.server.net;

import com.crakama.server.net.ClientSession;

import java.nio.channels.SocketChannel;

public class ServerInterestOPs {
    public ClientSession cSession;
    public int opsType;
    public int ops;

    public ServerInterestOPs(ClientSession cSession, int opsType, int ops){
        this.cSession = cSession;
        this.opsType = opsType;
        this.ops = ops;
    }
}
