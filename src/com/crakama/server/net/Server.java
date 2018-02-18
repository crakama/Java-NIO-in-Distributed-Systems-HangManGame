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
    Map<SelectionKey, String> gameStatusUpdate = new ConcurrentHashMap<>();
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
        this.listeners = new ConcurrentLinkedQueue<>();
        try{
            this.selector = initialiseSelector();
            initialiseServerChannel();

            while (true){
                serverInterface.addGameStatusListener(listeners, new GameOutPut());
                if(newGameStatus){
                    SelectionKey interestOPsKey = updateInterestOPS.poll();//TODO: gameStatusUpdate.poll()
                    updateClientQueue(interestOPsKey); //TODO: Save session and value in the same queue
                    prepareWrite(interestOPsKey);
                    newGameStatus = false;
                }

               selector.select();
                // processSelectorActions(selectorActions);
                System.out.println("Selector Block");
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

    private void prepareWrite(SelectionKey interestOPsKey) {
        interestOPsKey.interestOps(SelectionKey.OP_WRITE);
    }

    /**
     * Status already in global Queue, interestOps is WRITE and selector is up
     * @param selectionKey retrieves Clients session OBJ
     *  Append data from global queue to client local queue
     */
    private void updateClientQueue(SelectionKey selectionKey) {
        ClientSession cSession = (ClientSession) selectionKey.attachment();
        String gameGame = gameStatusUpdate.get(selectionKey);
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
            clientSession.commHandler.receiveMsg(key);
            System.out.println("serverInterestOPs.opsType" + clientSession);
        }catch (IOException clientDisconnected){
            removeClient(key);
        }
    }

    private void responseHandler(SelectionKey key) throws IOException, ClassNotFoundException {
        ClientSession clientSession = (ClientSession) key.attachment();
        try {
            clientSession.sendToClient();
            key.interestOps(SelectionKey.OP_READ);
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
        public void gameStatus(SelectionKey clientSeckey, String status) {
            System.out.println("Listener if found: " +clientSeckey+"gstatus >>"+status);

            gameStatusUpdate.put(clientSeckey,status);
            newGameStatus = true;
           // synchronized (updateInterestOPS){

                updateInterestOPS.add(clientSeckey );
                selector.wakeup();
           // }
//            selectorActions.add(() -> clientSeckey.interestOps(SelectionKey.OP_WRITE));
//            clientSeckey.selector().wakeup();
        }
    }

    private static void processSelectorActions(Queue<Runnable> selectorActions) {
        Runnable action;
        while((action = selectorActions.poll()) != null) {
            action.run();
        }
    }

}
