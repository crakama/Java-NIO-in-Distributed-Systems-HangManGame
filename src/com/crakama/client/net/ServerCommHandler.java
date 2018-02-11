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
    //private Map<SocketChannel, Queue<ByteBuffer>> pendindDataToSend = new HashMap();
    private final Queue<ByteBuffer> pendindDataToSend = new ArrayDeque<>();
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
                    System.out.println("Pending Changes ");
                    Iterator changes = this.pendingChanges.iterator();
                    while (changes.hasNext()) {
                        System.out.println("Pending Changes hasNext()");
                        ChangeInterestOPs changeInterestOPs = (ChangeInterestOPs) changes.next();

                        switch (changeInterestOPs.opsType) {
                            case ConstantValues.READ_OR_WRITE:
                                SelectionKey key = changeInterestOPs.socketChannel.keyFor(selector);
                                key.interestOps(changeInterestOPs.ops);
                                System.out.println("ConstantValues.WRITE: changeInterestOPs CHANGED");
                                break;
                            case ConstantValues.REGISTER:
                                changeInterestOPs.socketChannel.register(this.selector, changeInterestOPs.ops);
                                break;
                        }
                    }
                    this.pendingChanges.clear();
                }
                System.out.println("Selector client BEFORE ");
                selector.select();
                System.out.println("Selector client AFTER");
                Iterator selectedKeys = selector.selectedKeys().iterator();
                System.out.println("Selector client hasNext() BEFORE");
                while (selectedKeys.hasNext()){
                    System.out.println("Selector client hasNext() AFTER");
                    SelectionKey key = (SelectionKey) selectedKeys.next();
                    selectedKeys.remove();

                    if(!key.isValid()){
                        System.out.println("Selector client isValid");
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
                        //readInterest();
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
        bufferedServerMsg.clear();
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
        socketChannel = (SocketChannel)key.channel();
        ByteBuffer msg;
        synchronized (pendindDataToSend) {
            while ((msg = pendindDataToSend.peek()) != null) {
                socketChannel.write(msg);
                if (msg.hasRemaining()) {
                    return;
                }
                pendindDataToSend.remove();
            }
            key.interestOps(SelectionKey.OP_READ);
        }

//            Queue<ByteBuffer> queue = this.pendindDataToSend.get(socketChannel);
//            ByteBuffer dataBuf = queue.peek();
//        synchronized (pendindDataToSend){
//            while (dataBuf != null){
//                System.out.println("In Loop");
//
//                int written = socketChannel.write(dataBuf);
//                System.out.println("send data to server" + dataBuf.toString());
//                if(written == -1){
//                    System.out.println("written == -1,socket to close" + dataBuf.toString());
//                    socketChannel.close();
//                    pendindDataToSend.remove(socketChannel);
//                    return;
//                }
//                if(dataBuf.hasRemaining()){
//                    System.out.println("dataBuf.hasRemaining()" + dataBuf.toString());
//                    return;
//                }
//                queue.remove();
//
//            }
//        }

    }

    //    private void sendToServer(SelectionKey key) throws IOException {
//        ByteBuffer msg;
//        synchronized (messagesToSend) {
//            while ((msg = messagesToSend.peek()) != null) {
//                socketChannel.write(msg);
//                if (msg.hasRemaining()) {
//                    return;
//                }
//                messagesToSend.remove();
//            }
//            key.interestOps(SelectionKey.OP_READ);
//        }
//    }
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
     * Interest OPs can only be changed by selecting thread , thus the need to have stand alone methods
     * to do the work.
     */
    public void readInterest(){
        synchronized (this.pendingChanges){
            //socketChannel Might result to null pointer
            pendingChanges.add(new ChangeInterestOPs(socketChannel,
                    ConstantValues.READ_OR_WRITE,SelectionKey.OP_READ));
            System.out.println("Data saved to buffer, ConstantValues changed to READ ");
        }
        selector.wakeup();
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

            System.out.println("Before add to queue" + encodedMsg);
            pendindDataToSend.add(ByteBuffer.wrap(encodedMsg.getBytes()));
            System.out.println("after add to queue" + encodedMsg);
        }
        synchronized (this.pendingChanges){
            //socketChannel Might result to null pointer
            pendingChanges.add(new ChangeInterestOPs(socketChannel,
                    ConstantValues.READ_OR_WRITE,SelectionKey.OP_WRITE));
            System.out.println("Data saved to buffer, ConstantValues changed to WRITE ");
        }
    }
    //TO DO Wake up the selector and check variable

}
