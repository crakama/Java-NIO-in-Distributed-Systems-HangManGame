package com.crakama.Server_ThreadedBlocking.controller;

import com.crakama.Server_ThreadedBlocking.service.ServerInterface;
import com.crakama.Server_ThreadedBlocking.service.ServerInterfaceImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Controller {
    /**
     * @thhis, passed to Impl class in order to access methods defined here
     */


    private final List<String> status = Collections.synchronizedList(new ArrayList<>());

    public String gameStatus(){

        return String.valueOf(status);
    }

    public void updateGameStatus(String gameStatus){
        status.add(gameStatus);
    }

}
