package com.crakama.server.service;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.concurrent.ConcurrentLinkedQueue;

public interface ServerInterface {

    String initialiseGame() throws IOException, ClassNotFoundException;

    void playGame(SelectionKey poll) throws IOException, ClassNotFoundException;


    void getGuess(SelectionKey poll, String msgBody);

    void addGameStatusListener( ConcurrentLinkedQueue<GameStatusListener> cq,GameStatusListener gameOutPut);

}
