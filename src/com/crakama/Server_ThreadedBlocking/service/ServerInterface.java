package com.crakama.Server_ThreadedBlocking.service;

import com.crakama.Server_ThreadedBlocking.controller.Controller;
import com.crakama.Server_ThreadedBlocking.net.ClientCommHandler;

import java.io.IOException;
import java.net.Socket;

public interface ServerInterface {

    void initialiseGame() throws IOException, ClassNotFoundException;

    void playGame() throws IOException, ClassNotFoundException;

    void addController(Controller gameStatus);

    void getGuess(String msgBody);
}
