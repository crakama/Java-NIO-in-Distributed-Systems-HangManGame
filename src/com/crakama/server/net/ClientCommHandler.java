package com.crakama.server.net;

import com.crakama.common.ConstantValues;
import com.crakama.common.MsgProcessor;
import com.crakama.common.MsgType;
import com.crakama.server.service.ServerInterface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

public class ClientCommHandler implements Runnable{
    ByteBuffer bufferedClientMsg = ByteBuffer.allocateDirect(ConstantValues.BUFFER_SIZE);
    private final MsgProcessor msgProcessor = new MsgProcessor();
    Queue<SelectionKey> sessionkeys = new ConcurrentLinkedQueue<>();
    private SocketChannel socketChannel;
    private ServerInterface serveInterface;
    public ClientCommHandler(ServerInterface serverInterface, SocketChannel socketChannel)  {
        this.socketChannel = socketChannel;
        this.serveInterface = serverInterface;

    }

    @Override
    public void run() {
        while (msgProcessor.hasMsg()){
            MsgDecoder msg = new MsgDecoder(msgProcessor.nextMsg());
            try{
                switch (msg.msgType){

                    case START: case PLAY:
                        SelectionKey ky = sessionkeys.poll();
                        serveInterface.playGame(ky);
                        break;
                    case GUESS:
                        SelectionKey kyg = sessionkeys.poll();
                        serveInterface.getGuess(kyg,msg.msgBody);

                    case STOP:
                        break;

                }
            }catch (ClassNotFoundException|IOException e){

            }finally {
            }
        }

    }

    public void sendMsg(ByteBuffer msg) throws IOException {
        socketChannel.write(msg);
        if(msg.hasRemaining()){
            throw new IOException("Data Not Sent to ClientSession!!!");
        }
    }

    public void disConnect() throws IOException {
        socketChannel.close();
    }

    public void receiveMsg(ExecutorService pool, SelectionKey key) throws IOException {
        sessionkeys.add(key);
        bufferedClientMsg.clear();
        int data = socketChannel.read(bufferedClientMsg);

        while(data == -1){
            throw new IOException("Server was unable Read From ClientSession Socket");
        }
        String receivedMsg = readBufferData();
        msgProcessor.appendRecvdString(receivedMsg);

        pool.execute(this);
    }
    /**
     * Prepare buffer for reading
     * @return
     */
    private String readBufferData() {
        bufferedClientMsg.flip();
        byte[] bytes = new byte[bufferedClientMsg.remaining()];
        bufferedClientMsg.get(bytes);
        return new String(bytes);
    }

    private static class MsgDecoder{
        private MsgType msgType;
        private String msgBody;

        MsgDecoder(String receivedMsg){
            decodeMsg(receivedMsg);
        }

        private void decodeMsg(String parts){
            String[] msgParts =  parts.split(ConstantValues.MSG_TYPE_DELIMETER);
            if(msgParts.length > 1){
                msgType = MsgType.valueOf(msgParts[ConstantValues.MSG_TYPE_INDEX].toUpperCase());
                msgBody = msgParts[ConstantValues.MSG_BODY_INDEX].trim();
            }

        }
    }
}