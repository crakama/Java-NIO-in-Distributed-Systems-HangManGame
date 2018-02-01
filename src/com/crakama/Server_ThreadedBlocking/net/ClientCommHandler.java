package com.crakama.Server_ThreadedBlocking.net;

import com.crakama.common.MsgType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ClientCommHandler implements Runnable{
    private SocketChannel socketChannel;

    public ClientCommHandler(RequestHandler requestHandler, SocketChannel socketChannel)  {
        this.socketChannel = socketChannel;
    }

    @Override
    public void run() {

    }

    public void sendMsg(ByteBuffer msg) throws IOException {
        socketChannel.write(msg);
        if(msg.hasRemaining()){
            throw new IOException("Data Not Sent to Client!!!");
        }
    }

    public void disConnect() throws IOException {
        socketChannel.close();
    }
}
