package com.crakama.server.service;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.Queue;

public interface ServerInterface {

    String initialiseGame() throws IOException, ClassNotFoundException;

    void playGame(SelectionKey poll) throws IOException, ClassNotFoundException;


    void getGuess(String msgBody);

    void addGameStatusListener(Queue<GameStatusListener> listeners, GameStatusListener gameOutPut);
}
