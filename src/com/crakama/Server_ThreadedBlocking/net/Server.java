package com.crakama.Server_ThreadedBlocking.net;

import com.crakama.Server_ThreadedBlocking.controller.Controller;
import com.crakama.Server_ThreadedBlocking.service.ServerInterface;
import com.crakama.Server_ThreadedBlocking.service.ServerInterfaceImpl;
import com.crakama.common.ChangeInterestOPs;
import com.crakama.common.ConstantValues;

import java.io.IOException;
import java.net.InetSocketAddress;
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
                        case ConstantValues.READ_OR_WRITE:
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
                new ClientSession(socketChannel,clientCommHandler,controller,serverInterface));
    }
    private void requestHandler(SelectionKey key) throws IOException {
        try {
            ClientSession clientSession = (ClientSession) key.attachment();
            clientSession.commHandler.receiveMsg();
            key.interestOps(SelectionKey.OP_WRITE);
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
                updateInterestOPS.add(new ChangeInterestOPs(clientSession.channel,
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

    public void send(String gameStatus) {
        //Include client object
        //Call  method(updateQueue)to add data to client's queue
        //use change ops to change interest ops to write
    }

    public void wakeUpSelector() {
        //call pending changes and chane operation to write
        //Wake up selector
        selector.wakeup();
    }




}
