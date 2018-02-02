package com.crakama.Server_ThreadedBlocking.net;

import com.crakama.Server_ThreadedBlocking.controller.Controller;
import com.crakama.Server_ThreadedBlocking.service.ServerInterface;
import com.crakama.common.ConstantValues;
import com.crakama.common.MsgProcessor;
import com.crakama.common.MsgType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import java.util.StringJoiner;

public class Server {
    private final Controller controller = new Controller();
    private Selector selector;

    /**
     * Main server thread that handles incoming client requests
     * @param args
     */
    public static void main(String[] args) {
        new Server().processRequests();
    }

    public void processRequests() {
        try{
        this.selector = initialiseSelector();
        initialiseServerChannel();

        while (true){
            selector.select();
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while(iterator.hasNext()){
                SelectionKey key = iterator.next();
                iterator.remove();
                if(!key.isValid()){ continue; }
                if(key.isAcceptable()){ acceptHandler(key);
                }else if(key.isReadable()){ requestHandler(key);
                }else if(key.isWritable()){ responseHandler(key); }
            }
            }
        }catch (ClassNotFoundException|IOException e){

        }

    }
    private void acceptHandler(SelectionKey key) throws IOException, ClassNotFoundException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        ClientCommHandler clientCommHandler = new ClientCommHandler(this,socketChannel);
        serverSocketChannel.register(selector,SelectionKey.OP_WRITE,
                new Client(clientCommHandler,controller.initGameStaus()));
    }
    private void requestHandler(SelectionKey key) throws IOException {
        try {
            Client client = (Client) key.attachment();
            client.commHandler.receiveMsg();
        }catch (IOException clientDisconnected){
            removeClient(key);
        }
    }

    private void responseHandler(SelectionKey key) throws IOException {
        Client client = (Client) key.attachment();
        try {
        client.sendToClient();
        key.interestOps(SelectionKey.OP_READ);
        }catch (IOException clientDisconnected){
            removeClient(key);
        }

    }
    private void removeClient(SelectionKey key) throws IOException {
        Client client = (Client) key.attachment();
        client.commHandler.disConnect();
    }

    private void initialiseServerChannel() throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(ConstantValues.PORT_NUMBER));
        serverSocketChannel.register(this.selector, SelectionKey.OP_ACCEPT);
    }

    public Selector initialiseSelector() throws IOException{
        return SelectorProvider.provider().openSelector();
    }

    private class Client{
       private final Queue<ByteBuffer> queueGameStatus = new ArrayDeque();
        private final ClientCommHandler commHandler;

        private Client(ClientCommHandler clientCommHandler, String gameStatus){
            this.commHandler = clientCommHandler;
            queueGameStatus.add(dataToBytes(gameStatus));
        }


        public void sendToClient() throws IOException {
            synchronized (queueGameStatus){
                ByteBuffer msg;
                while (!queueGameStatus.isEmpty()){
                    msg = queueGameStatus.peek();
                    commHandler.sendMsg(msg);
                    queueGameStatus.remove();
                }
            }
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
