package com.crakama.Server_ThreadedBlocking.service;

import com.crakama.Server_ThreadedBlocking.controller.Controller;

import java.io.IOException;

public interface ServerInterface {

    String initialiseGame() throws IOException, ClassNotFoundException;

    void playGame(Controller contr) throws IOException, ClassNotFoundException;

    void addController(Controller gameStatus);

    void getGuess(String msgBody);
}
