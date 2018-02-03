package com.crakama.client.net;

import com.crakama.client.view.CmdType;
import com.crakama.common.ConstantValues;
import com.crakama.common.MsgProcessor;
import com.crakama.common.MsgType;
import com.crakama.common.ChangeInterestOPs;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

public class ServerCommHandler  implements Runnable{
    private InetSocketAddress serverAddress;
    private Map<SocketChannel, Queue<ByteBuffer>> pendindDataToSend = new HashMap();
    private final MsgProcessor msgProcessor = new MsgProcessor();
    private ByteBuffer bufferedServerMsg = ByteBuffer.allocate(ConstantValues.BUFFER_SIZE);
    private List<OutputHandler> commListeners = new ArrayList<>();
    private List pendingChanges = new LinkedList();
    private boolean connected = false;
    private SocketChannel socketChannel;
    private Selector selector;

    public ServerCommHandler() {

    }

    /**
     * Process any pending data first if available and change or register interest OPS
     */
    @Override
    public void run(){
        try {
            this.selector = initiateSelector();
            initialiseConnection();
            while(connected){

                synchronized (this.pendingChanges) {
                    Iterator changes = this.pendingChanges.iterator();
                    while (changes.hasNext()) {
                        ChangeInterestOPs changeInterestOPs = (ChangeInterestOPs) changes.next();

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
                    this.pendingChanges.clear();
                }
                System.out.println("Selector client");
                this.selector.select();
                Iterator selectedKeys = this.selector.selectedKeys().iterator();
                while (selectedKeys.hasNext()){
                    SelectionKey key = (SelectionKey) selectedKeys.next();
                    selectedKeys.remove();

                    if(!key.isValid()){
                        continue;
                    }
                    if(key.isConnectable()){
                        finishConnection(key);
                    }else if(key.isReadable()){
                        System.out.println("Selector client isReadable");
                        receiveMsg(key);
                    }else if(key.isWritable()){
                        System.out.println("Selector client isWritable");
                        sendMsg(key);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }catch (Throwable connectionFailure){
           // outputHandler.handleErrorResponse(connectionFailure);
        }
    }

    /**
     * Finish client Connection and make it ready for write operations
     * @param key
     */
    private void finishConnection(SelectionKey key){
        SocketChannel socketChannel = (SocketChannel) key.channel();
        try {
            socketChannel.finishConnect();
            key.interestOps(SelectionKey.OP_READ);
            InetSocketAddress remoteAddress = (InetSocketAddress) socketChannel.getRemoteAddress();
            notifyUser(remoteAddress);
        } catch (IOException e) {
            System.out.println(ConstantValues.FAILED_CONNECTION);
        }
        //key.interestOps(SelectionKey.OP_WRITE);
    }

    private void notifyUser(InetSocketAddress inetSocketAddress){
        Executor forkPool = ForkJoinPool.commonPool();
        for(OutputHandler listener: commListeners){
            forkPool.execute(() -> listener.notifyUser(inetSocketAddress));
        }
    }
    private void receiveMsg(SelectionKey key) throws IOException {
       SocketChannel socketChannel = (SocketChannel) key.channel();
       int bytesRead = socketChannel.read(bufferedServerMsg);
       if(bytesRead == -1){
           //pendindDataToSend.remove(socketChannel);
           //return;
           System.out.println("bytesRead == -1");
       }
       if(bytesRead > 0){
           System.out.println("bytesRead > 0");
           String receivedData = readBufferData();
           System.out.println("bytesRead > 0"+receivedData);
           msgProcessor.appendRecvdString(receivedData);
           System.out.println("msgProcessor.receivedData"+ receivedData);
           while (msgProcessor.hasMsg()){
               System.out.println("msgProcessor.hasMsg()");
              String msg = msgProcessor.nextMsg();
               System.out.println("msgProcessor.hasMsg()"+msgProcessor.msgBody(msg));
              displayMsg(msgProcessor.msgBody(msg));
           }
       }
    }

    private void displayMsg(String msg) {
        System.out.println("Display Msg" + msg);
        Executor forkPool = ForkJoinPool.commonPool();
        for(OutputHandler listener: commListeners){
            forkPool.execute(() -> listener.handleServerResponse(msg));
        }
    }

    /**
     * Prepare buffer for reading
     * @return
     */
    private String readBufferData() {
        bufferedServerMsg.flip();
        byte[] bytes = new byte[bufferedServerMsg.remaining()];
        bufferedServerMsg.get(bytes);
        return new String(bytes);
    }


    /**
     * Retrieve message from buffer and write to channel
     * @param key
     * @value written,
     * @return if the socket's buffer fills up
     */

    public void sendMsg(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel)key.channel();
        synchronized (pendindDataToSend){
            Queue<ByteBuffer> queue = this.pendindDataToSend.get(socketChannel);
            if(queue.isEmpty()){
                key.interestOps(SelectionKey.OP_READ);
            }
            while (!queue.isEmpty()){
                ByteBuffer dataBuf = queue.peek();
                int written = socketChannel.write(dataBuf);
                System.out.println("send data to server" + dataBuf.toString());
                if(written == -1){
                    pendindDataToSend.remove(socketChannel);
                    socketChannel.close();
                }
                if(dataBuf.hasRemaining()){
                    return;
                }else {
                    queue.remove();
                }
            }
        }
    }
    public void connect(String host, int port,OutputHandler outputHandler) throws IOException {
        this.serverAddress = new InetSocketAddress(host,port);
        this.commListeners.add(outputHandler);
        new Thread(this).start();

    }
    public Selector initiateSelector() throws IOException {
        return SelectorProvider.provider().openSelector();
    }
    public void initialiseConnection() throws IOException {
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(serverAddress);
        synchronized (this.pendingChanges){
            pendingChanges.add(new ChangeInterestOPs(socketChannel,
                    ConstantValues.REGISTER,SelectionKey.OP_CONNECT) );
        }
        connected = true;
    }

    public void initialiseGame() throws IOException {
        System.out.println("Initialise game");
        encodeMsg(MsgType.START.toString(),null);
        System.out.println("Selector is awake");
    }
    public void sendGuess(String guess) throws IOException {
        encodeMsg(MsgType.GUESS.toString(),guess);
    }
    public void playGame(CmdType cmd) throws IOException {
        if(cmd.equals("START")){
            encodeMsg(MsgType.START.toString(),null);
        }else{
            encodeMsg(MsgType.PLAY.toString(),null);
        }

    }

    /**
     * Save Encoded message to buffer
     * @param msgParts
     * @throws IOException
     */
    public void encodeMsg(String... msgParts) throws IOException {
        System.out.println("Encode message Sting joiner ");
        StringJoiner joiner = new StringJoiner(ConstantValues.MSG_TYPE_DELIMETER);
        for(String part: msgParts){
            joiner.add(part);
        }
        String encodedMsg = MsgProcessor.appendLenHeader(joiner.toString());
        addToBuffer(encodedMsg);
        selector.wakeup();
    }

    public void addToBuffer(String encodedMsg){
        synchronized (pendindDataToSend){
            System.out.println("pending data to send");
            Queue<ByteBuffer> fromQueue,toQueue;
            fromQueue =  this.pendindDataToSend.get(socketChannel);
            if(fromQueue == null){
                System.out.println("Queue empty");
                fromQueue = new ArrayDeque<>();
                this.pendindDataToSend.put(socketChannel,fromQueue);
            }
            System.out.println("Before add to queue" + encodedMsg);
            fromQueue.add(ByteBuffer.wrap(encodedMsg.getBytes()));
            System.out.println("after add to queue" + encodedMsg);
        }
        synchronized (this.pendingChanges){
            //socketChannel Might result to null pointer
            pendingChanges.add(new ChangeInterestOPs(socketChannel,
                    ConstantValues.WRITE,SelectionKey.OP_WRITE));
            System.out.println("Data saved to buffer");
        }
    }
    //TO DO Wake up the selector and check variable

}
