package com.crakama.Server_ThreadedBlocking.net;

import com.crakama.Server_ThreadedBlocking.controller.Controller;
import com.crakama.Server_ThreadedBlocking.service.ServerInterface;
import com.crakama.common.ConstantValues;
import com.crakama.common.MsgProcessor;
import com.crakama.common.MsgType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.StringJoiner;

/**
 * Holds client sessions
 */
class ClientSession {
    private final Queue<ByteBuffer> queueGameStatus = new ArrayDeque();
    public final ClientCommHandler commHandler;
    private final Controller contr;
    public final SocketChannel channel;

    public ClientSession(SocketChannel socketChannel, ClientCommHandler clientCommHandler,
                         Controller contr, ServerInterface serverInterface) throws
                  IOException, ClassNotFoundException {
        this.commHandler = clientCommHandler;
        this.contr=contr;
        this.channel = socketChannel;
        serverInterface.addController(contr);
        processQueue();
    }


    /**
     * Always check for new status from model and service layer
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void processQueue() throws IOException, ClassNotFoundException {

        synchronized (queueGameStatus){
            System.out.println("BEFORE processQueue New data to LOCAL QUEUE ");
            String getStatus = contr.gameStatus();
            System.out.println("MIDDLE processQueue New data to LOCAL QUEUE "+getStatus);
            queueGameStatus.add(dataToBytes(getStatus));
            System.out.println("AFTER processQueue New data to LOCAL QUEUE ");
        }

    }
    public void sendToClient() throws IOException, ClassNotFoundException {
        System.out.println("sendToClient ");
        processQueue();
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
            System.out.println("sendToClient "+ msg.toString());
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

}
