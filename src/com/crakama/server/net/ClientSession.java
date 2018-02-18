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
            System.out.println("BEFORE processQueue New data to LOCAL QUEUE ");
            queueGameStatus.add(dataToBytes(serverInterface.initialiseGame()));
            System.out.println("AFTER processQueue New data to LOCAL QUEUE ");
        }

    }
    public void sendToClient() throws IOException, ClassNotFoundException {
        System.out.println("sendToClient ");
        synchronized (queueGameStatus){
            ByteBuffer msg;
            while(queueGameStatus.isEmpty()) {
                try {
                    System.out.println("isEmpty ");
                    queueGameStatus.wait();
                } catch (InterruptedException e) {
                }
            }
            msg = queueGameStatus.peek();
            System.out.println("sendToClient msg.peek"+ msg.toString());
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
            System.out.println("add to LQueue"+ gameGame);
            queueGameStatus.add(dataToBytes(gameGame));
        }
    }
}