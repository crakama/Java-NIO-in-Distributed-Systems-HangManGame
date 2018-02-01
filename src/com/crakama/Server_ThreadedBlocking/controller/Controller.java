package com.crakama.Server_ThreadedBlocking.controller;

import com.crakama.Server_ThreadedBlocking.service.ServerInterface;
import com.crakama.Server_ThreadedBlocking.service.ServerInterfaceImpl;

import java.io.IOException;

public class Controller {

   private final ServerInterface serverInterface = new ServerInterfaceImpl();
    public String initGameStaus() throws IOException, ClassNotFoundException {

        return serverInterface.initialiseGame();
    }
}
