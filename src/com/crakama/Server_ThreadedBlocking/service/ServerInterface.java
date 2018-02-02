package com.crakama.Server_ThreadedBlocking.service;

import com.crakama.Server_ThreadedBlocking.net.ClientCommHandler;

import java.io.IOException;
import java.net.Socket;

public interface ServerInterface {

    String initialiseGame() throws IOException, ClassNotFoundException;

    void playGame() throws IOException, ClassNotFoundException;
}
