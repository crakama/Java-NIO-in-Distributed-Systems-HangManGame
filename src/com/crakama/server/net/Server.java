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
import java.util.concurrent.ConcurrentLinkedQueue;

public class Server {

    private final ServerInterface serverInterface = new ServerInterfaceImpl();
    Queue<String> gameStatusUpdate = new ConcurrentLinkedQueue<>();
    Queue<SelectionKey> updateInterestOPS = new ConcurrentLinkedQueue<>();
    private volatile boolean newGameStatus = false;
    private Selector selector;


    /**
     * Main server thread that handles incoming client requests
     * @param args
     */
    public static void main(String[] args) {

        new Server().processRequests();
    }

    public void processRequests() {
        ///this.listeners = new ConcurrentLinkedQueue<>();
        try{
            this.selector = initialiseSelector();
            initialiseServerChannel();
            while (true){
                if(newGameStatus){
                    updateClientQueue();
                    newGameStatus = false;
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
                        requestHandler(key);
                    }else if(key.isWritable()){
                        responseHandler(key); }
                }
            }
        }catch (ClassNotFoundException|IOException e){

        }

    }

    /**
     * Status already in global Queue, interestOps is WRITE and selector is up
     * selectionKey retrieves Clients session OBJ
     *  Append data from global queue to client local queue
     */
    private void updateClientQueue() {
        SelectionKey interestOPsKey = updateInterestOPS.poll();
        ClientSession cSession = (ClientSession) interestOPsKey.attachment();
        String gameGame = gameStatusUpdate.poll();
        cSession.addToQueue(gameGame);
        interestOPsKey.interestOps(SelectionKey.OP_WRITE);
    }

    private void acceptHandler(SelectionKey key) throws IOException, ClassNotFoundException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        ClientCommHandler clientCommHandler = new ClientCommHandler(serverInterface,socketChannel);
        socketChannel.register(selector,SelectionKey.OP_WRITE,
                new ClientSession(socketChannel,clientCommHandler,serverInterface));
    }
    private void requestHandler(SelectionKey key) throws IOException {
        try {
            ClientSession clientSession = (ClientSession) key.attachment();

            serverInterface.addGameStatusListener(new ConcurrentLinkedQueue<>(), new GameOutPut());
            clientSession.commHandler.receiveMsg(key);
        }catch (IOException clientDisconnected){
            removeClient(key);
        }
    }

    private void responseHandler(SelectionKey key) throws IOException{
        ClientSession clientSession = (ClientSession) key.attachment();
        try {
            clientSession.sendToClient();
            key.interestOps(SelectionKey.OP_READ);
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
        public void gameStatus(SelectionKey clientSeckey, String status) {
            gameStatusUpdate.add(status);
            newGameStatus = true;
            updateInterestOPS.add(clientSeckey );
            selector.wakeup();
        }
    }
}
    /*
        Queue<Runnable> selectorActions = new ConcurrentLinkedQueue<>();
        private static void interestOPs(Queue<Runnable> selectorActions) {
            Runnable action;
            while((action = interests.poll()) != null) {
                action.run();
            }
            selectorActions.add(() -> key.interestOps(SelectionKey.OP_WRITE));
        }
    */