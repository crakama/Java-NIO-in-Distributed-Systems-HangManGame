package com.crakama.Server_ThreadedBlocking.net;

import com.crakama.common.MsgType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientCommHandler {
    private ObjectOutputStream toClient;
    private ObjectInputStream fromClient;

    public ClientCommHandler(Socket clientSocket) throws IOException {
        this.toClient = new ObjectOutputStream(clientSocket.getOutputStream());
        this.fromClient = new ObjectInputStream(clientSocket.getInputStream());
    }

    public void sendResponse(String response) throws IOException {
        MsgProtocol msgProtocol = new MsgProtocol(MsgType.RESPONSE,response);
        toClient.writeObject(msgProtocol);
        toClient.flush();
        toClient.reset();
    }

    public MsgProtocol readRequest() throws IOException, ClassNotFoundException {
        MsgProtocol msg = (MsgProtocol) fromClient.readObject();
       return msg;
    }
}
