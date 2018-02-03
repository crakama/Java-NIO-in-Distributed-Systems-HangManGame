package com.crakama.Server_ThreadedBlocking.controller;

import com.crakama.Server_ThreadedBlocking.net.Server;
import com.crakama.Server_ThreadedBlocking.service.ServerInterface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Controller {
    /**
     * @thhis, passed to Impl class in order to access methods defined here
     */


    private final List<String> status = Collections.synchronizedList(new ArrayList<>());
    private ServerInterface serverInterface;
    private Server server = new Server();
    public Controller(ServerInterface serverInterface) {
        this.serverInterface = serverInterface;
    }

    public String gameStatus() throws IOException, ClassNotFoundException {
        if(status.isEmpty()){
            return serverInterface.initialiseGame();
        }
        return String.valueOf(status);
    }

    public void updateGameStatus(String gameStatus){
        //status.add(gameStatus);
        server.send(gameStatus);
        System.out.println("gameStatus" +gameStatus);
    }

}
