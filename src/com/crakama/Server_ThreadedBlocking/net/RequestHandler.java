package com.crakama.Server_ThreadedBlocking.net;

import com.crakama.Server_ThreadedBlocking.service.ServeInterface;
import com.crakama.Server_ThreadedBlocking.service.ServerInterfaceImpl;

import java.io.IOException;
import java.net.Socket;

public class RequestHandler implements Runnable{
    private Socket clientSocket;
    private ClientCommHandler clientCommHandler;
    private ServeInterface serveInterface;
    private boolean gameInitialised = false;

    public RequestHandler(ClientCommHandler threadedBlockingServer, Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.clientCommHandler = threadedBlockingServer;
        this.serveInterface = new ServerInterfaceImpl();
    }
    @Override
    public void run() {
        while (clientSocket.isConnected()){
            try{
                switch (clientCommHandler.readRequest().getMsgType()){

                    case START:
                        serveInterface.initialiseGame(clientCommHandler,clientSocket);
                        gameInitialised = true;
                        break;
                    case PLAY:
                        if(gameInitialised){
                            serveInterface.playGame(clientCommHandler);
                        }
                        break;
                    case STOP:
                        break;

                }
            }catch (ClassNotFoundException|IOException e){

            }finally {
            }
        }

    }

}
