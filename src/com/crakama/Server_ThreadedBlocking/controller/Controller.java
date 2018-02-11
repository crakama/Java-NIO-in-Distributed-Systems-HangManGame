package com.crakama.Server_ThreadedBlocking.controller;

import com.crakama.Server_ThreadedBlocking.net.Server;
import com.crakama.Server_ThreadedBlocking.service.ServerInterface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

public class Controller {
    /**
     * @thhis, passed to Impl class in order to access methods defined here
     */

    private final Queue<ByteBuffer> status = new ArrayDeque();
   // private final List<String> status = Collections.synchronizedList(new ArrayList<>());
    private ServerInterface serverInterface;
    public Controller(ServerInterface serverInterface) {
        this.serverInterface = serverInterface;
    }

    public String gameStatus() throws IOException, ClassNotFoundException {
        System.out.println("CONTROLLER STATUS" +status.toString());
        if(status.isEmpty()){
            return serverInterface.initialiseGame();
        }
        String gStatus = String.valueOf(status);
        status.remove();
        return gStatus;
    }

    public void updateGameStatus(String gameStatus){
        status.add(ByteBuffer.wrap(gameStatus.getBytes()));

        //server.send(gameStatus);
       // System.out.println("gameStatus" +gameStatus);
    }

}
