package com.crakama.Server_ThreadedBlocking.net;

import com.crakama.Server_ThreadedBlocking.service.ServerInterface;
import com.crakama.Server_ThreadedBlocking.service.ServerInterfaceImpl;
import com.crakama.common.ConstantValues;
import com.crakama.common.MsgProcessor;
import com.crakama.common.MsgType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ForkJoinPool;

public class ClientCommHandler implements Runnable{
    private SocketChannel socketChannel;
    ByteBuffer bufferedClientMsg = ByteBuffer.allocateDirect(ConstantValues.BUFFER_SIZE);
    private final MsgProcessor msgProcessor = new MsgProcessor();
    private boolean gameInitialised = false;
    private ServerInterface serveInterface;
    public ClientCommHandler(Server server, SocketChannel socketChannel)  {
        this.socketChannel = socketChannel;
        this.serveInterface = new ServerInterfaceImpl();
    }

    @Override
    public void run() {
        while (msgProcessor.hasMsg()){
            MsgDecoder msg = new MsgDecoder(msgProcessor.nextMsg());
            try{
                switch (msg.msgType){

                    case START:
                        gameInitialised = true;
                        break;
                    case PLAY:
                        if(gameInitialised){
                            serveInterface.playGame();
                        }
                        break;
                    case GUESS:
                        serveInterface.getGuess(msg.msgBody);
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
            throw new IOException("Data Not Sent to Client!!!");
        }
    }

    public void disConnect() throws IOException {
        socketChannel.close();
    }

    public void receiveMsg() throws IOException {
       bufferedClientMsg.clear();
       int data = socketChannel.read(bufferedClientMsg);
       while(data == -1){
           throw new IOException("Server was unable Read From Client Socket");
       }
       String receivedMsg = readBufferData();
       msgProcessor.appendRecvdString(receivedMsg);
        ForkJoinPool.commonPool().execute(this);
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
