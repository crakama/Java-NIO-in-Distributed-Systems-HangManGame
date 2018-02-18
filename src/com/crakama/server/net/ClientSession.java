package com.crakama.server.net;


import com.crakama.common.ConstantValues;
import com.crakama.common.MsgProcessor;
import com.crakama.common.MsgType;
import com.crakama.server.service.ServerInterface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.StringJoiner;

/**
 * Holds client sessions
 */
public class ClientSession {
    private final Queue<ByteBuffer> queueGameStatus = new ArrayDeque();
    public final ClientCommHandler commHandler;
    public final SocketChannel channel;
    private ServerInterface serverInterface;

    public ClientSession(SocketChannel socketChannel, ClientCommHandler clientCommHandler,
                         ServerInterface serverInterface) throws
            IOException, ClassNotFoundException {
        this.commHandler = clientCommHandler;
        this.channel = socketChannel;
        this.serverInterface = serverInterface;
        initQueue(serverInterface);
    }


    /**
     * Always check for new status from model and service layer
     * @throws IOException
     * @throws ClassNotFoundException
     * @param serverInterface
     */
    public void initQueue(ServerInterface serverInterface) throws IOException, ClassNotFoundException {

        synchronized (queueGameStatus){
            queueGameStatus.add(dataToBytes(serverInterface.initialiseGame()));
        }

    }
    public void sendToClient() throws IOException{
        synchronized (queueGameStatus){
            ByteBuffer msg;
            while(queueGameStatus.isEmpty()) {
                try {
                    queueGameStatus.wait();
                } catch (InterruptedException e) {
                }
            }
            msg = queueGameStatus.peek();
            commHandler.sendMsg(msg);
            queueGameStatus.remove();
        }
    }
    /**
     * Convert data to bytes
     * @param gameStatus
     * @return
     */
    private ByteBuffer dataToBytes(String gameStatus) {
        StringJoiner joiner = new StringJoiner(ConstantValues.MSG_TYPE_DELIMETER);
        joiner.add(MsgType.RESPONSE.toString());
        joiner.add(gameStatus);
        String addMsgHeader = MsgProcessor.appendLenHeader(joiner.toString());
        return ByteBuffer.wrap(addMsgHeader.getBytes());
    }

    public void addToQueue(String gameGame) {
        synchronized (queueGameStatus){
            queueGameStatus.add(dataToBytes(gameGame));
        }
    }
}