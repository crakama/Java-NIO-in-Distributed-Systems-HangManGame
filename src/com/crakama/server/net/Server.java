package com.crakama.server.net;

import com.crakama.common.ConstantValues;
import com.crakama.server.service.GameStatusListener;
import com.crakama.server.service.ServerInterface;
import com.crakama.server.service.ServerInterfaceImpl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Server {

    private final ServerInterface serverInterface = new ServerInterfaceImpl();
    Queue<GameStatusListener> listeners;
    Map<ClientSession, String> gameStatusUpdate = new ConcurrentHashMap<>();
    private List updateInterestOPS = new LinkedList();
    private Selector selector;

    /**
     * Main server thread that handles incoming client requests
     * @param args
     */
    public static void main(String[] args) {

        new Server().processRequests();
    }

    public void processRequests() {
        this.listeners = new ConcurrentLinkedQueue<>();
        try{
            this.selector = initialiseSelector();
            initialiseServerChannel();

            while (true){
                serverInterface.addGameStatusListener(listeners, new GameOutPut());
                synchronized (this.updateInterestOPS) {
                    Iterator changedOPs = this.updateInterestOPS.iterator();
                    //? Save session to a list
                    while (changedOPs.hasNext()) {
                        ServerInterestOPs serverInterestOPs = (ServerInterestOPs) changedOPs.next();
                        ClientSession cSession = serverInterestOPs.getSession();
                        System.out.println("serverInterestOPs.opsType" + cSession);
                        updateClientQueue(cSession);
                        System.out.println("serverInterestOPs.opsType");
                        switch (serverInterestOPs.opsType) {
                            case ConstantValues.READ_OR_WRITE:
                                SelectionKey key = cSession.channel.keyFor(this.selector);
                                key.interestOps(serverInterestOPs.getOPs());
                                break;
                            case ConstantValues.REGISTER:
                                cSession.channel.register(this.selector, serverInterestOPs.getOPs());
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

    /**
     *
     * @param cSession retrieves data from global queue and appends to client local queue
     */
    private void updateClientQueue(ClientSession cSession) {
        String gameGame = gameStatusUpdate.get(cSession);
        System.out.println("Value retrieved from GQueue Successfully" +gameGame);
        cSession.addToQueue(gameGame);
    }

    private void acceptHandler(SelectionKey key) throws IOException, ClassNotFoundException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);

        ClientCommHandler clientCommHandler = new ClientCommHandler(serverInterface,socketChannel);
        System.out.println("acceptHandler");
        socketChannel.register(selector,SelectionKey.OP_WRITE,
                new ClientSession(socketChannel,clientCommHandler,serverInterface));
    }
    private void requestHandler(SelectionKey key) throws IOException {
        try {
            ClientSession clientSession = (ClientSession) key.attachment();
            clientSession.commHandler.receiveMsg(clientSession);
            System.out.println("serverInterestOPs.opsType" + clientSession);
        }catch (IOException clientDisconnected){
            removeClient(key);
        }
    }

    private void responseHandler(SelectionKey key) throws IOException, ClassNotFoundException {
        ClientSession clientSession = (ClientSession) key.attachment();
        try {
            clientSession.sendToClient();
            //key.interestOps(SelectionKey.OP_READ);
            synchronized (this.updateInterestOPS){
                updateInterestOPS.add(new ServerInterestOPs(clientSession,
                        ConstantValues.READ_OR_WRITE,SelectionKey.OP_READ) );
            }
            System.out.println("Server Operation changed to read");
        }catch (IOException clientDisconnected){
            removeClient(key);
        }

    }
    private void removeClient(SelectionKey key) throws IOException {
        ClientSession clientSession = (ClientSession) key.attachment();
        clientSession.commHandler.disConnect();
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


    private class GameOutPut implements GameStatusListener{

        //Updated by thread pool
        @Override
        public void gameStatus(ClientSession clientSession,String status) {
            System.out.println("Listener if found: " +clientSession+"gstatus >>"+status);
            gameStatusUpdate.put(clientSession,status);
            synchronized (updateInterestOPS){
                updateInterestOPS.add(new ServerInterestOPs(clientSession,
                        ConstantValues.READ_OR_WRITE,SelectionKey.OP_WRITE) );
            }
            selector.wakeup();
        }
    }

}
