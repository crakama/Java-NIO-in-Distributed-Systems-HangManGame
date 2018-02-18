package com.crakama.server.service;

import com.crakama.server.net.ClientSession;

import java.io.IOException;
import java.util.Queue;

public interface ServerInterface {

    String initialiseGame() throws IOException, ClassNotFoundException;

    void playGame(ClientSession poll) throws IOException, ClassNotFoundException;


    void getGuess(String msgBody);

    void addGameStatusListener(Queue<GameStatusListener> listeners, GameStatusListener gameOutPut);
}
