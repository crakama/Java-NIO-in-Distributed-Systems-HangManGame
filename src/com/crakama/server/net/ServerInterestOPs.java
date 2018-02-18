package com.crakama.server.net;

import java.nio.channels.SelectionKey;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ServerInterestOPs {
    public ClientSession cSession;
    Queue<SelectionKey> sessions = new ConcurrentLinkedQueue();
    Queue<Integer> ops = new ConcurrentLinkedQueue();
    public int opsType;
   // public int ops;

    public ServerInterestOPs(SelectionKey key, int opsType, int ops){
        sessions.add(key);
        this.opsType = opsType;
        this.ops.add(ops);
    }
    public  Integer getOPs(){
        return ops.poll();
    }
    //?Use runnable approach
    public ClientSession getSession(){
       SelectionKey selectionKey = sessions.poll();
       ClientSession clientSession = (ClientSession) selectionKey.attachment();
        return clientSession;
    }
//    public SelectionKey getKey(){
//
//        return
//    }

}
