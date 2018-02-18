package com.crakama.server.net;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ServerInterestOPs {
    public ClientSession cSession;
    Queue<ClientSession> sessions = new ConcurrentLinkedQueue();
    Queue<Integer> ops = new ConcurrentLinkedQueue();
    public int opsType;
   // public int ops;

    public ServerInterestOPs(ClientSession cSession, int opsType, int ops){
        sessions.add(cSession);
        this.opsType = opsType;
        this.ops.add(ops);
    }
    public  Integer getOPs(){
        return ops.poll();
    }
    public ClientSession getSession(){
        return sessions.poll();
    }
}
