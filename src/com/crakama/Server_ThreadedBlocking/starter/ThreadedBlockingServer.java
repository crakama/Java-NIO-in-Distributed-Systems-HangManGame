package com.crakama.Server_ThreadedBlocking.starter;

import com.crakama.Server_ThreadedBlocking.net.ClientCommHandler;
import com.crakama.Server_ThreadedBlocking.net.RequestHandler;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ThreadedBlockingServer {

    private  int DEFAULT_PORT = 2123;
    private ClientCommHandler clientCommHandler;
    /**
     * Allows many client sockets to connect because server operations
     * Are handled by a different thread
     * @clientSocket is never null because the server is always listening
     */
    public static void main(String[] args){ new ThreadedBlockingServer().incomingRequests(); }

    public void incomingRequests(){
        try {
            ServerSocket serverSocket = new ServerSocket(DEFAULT_PORT);
            while (true){
                Socket clientSocket = serverSocket.accept();
                clientCommHandler = new ClientCommHandler(clientSocket);
                clientCommHandler.sendResponse("Server Accepted Connection!!!.");
                startRequestHandler(clientCommHandler,clientSocket);
                //new Thread(()-> new RequestHandler(clientCommHandler, clientSocket));
            }
        }catch (IOException serverconnectionfailed){
            throw new UncheckedIOException(serverconnectionfailed);
        }
    }
     public void startRequestHandler(ClientCommHandler clientCommHandler, Socket s){
        RequestHandler requestHandler = new RequestHandler(clientCommHandler,s);
        Thread workThread = new Thread(requestHandler);
        workThread.start();

     }
}

