package com.crakama.Server_ThreadedBlocking.service;

import com.crakama.Server_ThreadedBlocking.net.ClientCommHandler;

import java.io.IOException;
import java.net.Socket;

public interface ServeInterface {

    void initialiseGame(ClientCommHandler connectionHandler, Socket clientSocket) throws IOException, ClassNotFoundException;

    void playGame(ClientCommHandler connHandler) throws IOException, ClassNotFoundException;
}
