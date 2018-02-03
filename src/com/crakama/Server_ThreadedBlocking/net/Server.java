package com.crakama.Server_ThreadedBlocking.net;

import com.crakama.Server_ThreadedBlocking.controller.Controller;
import com.crakama.Server_ThreadedBlocking.service.ServerInterface;
import com.crakama.Server_ThreadedBlocking.service.ServerInterfaceImpl;
import com.crakama.common.ChangeInterestOPs;
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
import java.util.*;

public class Server {

    private final ServerInterface serverInterface = new ServerInterfaceImpl();
    private final Controller controller = new Controller(serverInterface);
    private Selector selector;
    private List updateInterestOPS = new LinkedList();


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

            synchronized (this.updateInterestOPS) {
                Iterator changedOPs = this.updateInterestOPS.iterator();
                while (changedOPs.hasNext()) {
                    ChangeInterestOPs changeInterestOPs = (ChangeInterestOPs) changedOPs.next();

                    switch (changeInterestOPs.opsType) {
                        case ConstantValues.WRITE:
                            SelectionKey key = changeInterestOPs.socketChannel.keyFor(this.selector);
                            key.interestOps(changeInterestOPs.ops);
                            break;
                        case ConstantValues.REGISTER:
                            changeInterestOPs.socketChannel.register(this.selector, changeInterestOPs.ops);
                            break;
                    }
                }
                this.updateInterestOPS.clear();
            }

            selector.select();
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while(iterator.hasNext()){
                SelectionKey key = iterator.next();
                iterator.remove();
                if(!key.isValid()){ continue; }
                if(key.isAcceptable()){
                    acceptHandler(key);
                }else if(key.isReadable()){
                    System.out.println("isReadable");
                    requestHandler(key);
                }else if(key.isWritable()){
                    System.out.println("isWritable");
                    responseHandler(key); }
            }
            }
        }catch (ClassNotFoundException|IOException e){

        }

    }
    private void acceptHandler(SelectionKey key) throws IOException, ClassNotFoundException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        ClientCommHandler clientCommHandler = new ClientCommHandler(controller,socketChannel);
        System.out.println("acceptHandler");
        socketChannel.register(selector,SelectionKey.OP_WRITE,
                new Client(clientCommHandler,controller));
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
        System.out.println("Server Operation changed to read");
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

    public void send(String gameStatus) {

    }

    private class Client{
       private final Queue<ByteBuffer> queueGameStatus = new ArrayDeque();
       private final ClientCommHandler commHandler;

        private Client(ClientCommHandler clientCommHandler, Controller contr) throws IOException, ClassNotFoundException {
            this.commHandler = clientCommHandler;
            System.out.println("sendToClient "+ contr.gameStatus());
            queueGameStatus.add(dataToBytes(contr.gameStatus()));
            serverInterface.addController(contr);
        }


        public void sendToClient() throws IOException {
            synchronized (queueGameStatus){
                ByteBuffer msg;
                while (!queueGameStatus.isEmpty()){
                    msg = queueGameStatus.peek();
                    System.out.println("sendToClient "+ msg.toString());
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
