package com.crakama.client.net;

import com.crakama.common.ConstantValues;
import com.crakama.common.MsgProcessor;
import com.crakama.common.MsgType;
import com.crakama.common.RequestTypeHandler;

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
    private ByteBuffer bufferedServerMsg = ByteBuffer.allocate(8192);
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
                        RequestTypeHandler opsTypeHandler = (RequestTypeHandler) changes.next();

                        switch (opsTypeHandler.opsType) {
                            case ConstantValues.CHANGEINTOPS:
                                SelectionKey key = opsTypeHandler.socketChannel.keyFor(this.selector);
                                key.interestOps(opsTypeHandler.ops);
                                break;
                            case ConstantValues.REGISTERINTOPS:
                                opsTypeHandler.socketChannel.register(this.selector, opsTypeHandler.ops);
                                break;
                        }
                    }
                    this.pendingChanges.clear();
                }
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
                        receiveMsg(key);
                    }else if(key.isWritable()){
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
        } catch (IOException e) {
            System.out.println(ConstantValues.FAILED_CONNECTION);
            key.channel();
            return;
        }
        key.interestOps(SelectionKey.OP_WRITE);
    }
    private void receiveMsg(SelectionKey key) throws IOException {
       SocketChannel socketChannel = (SocketChannel) key.channel();
       int bytesRead = socketChannel.read(bufferedServerMsg);
       if(bytesRead == -1){
           pendindDataToSend.remove(socketChannel);
           return;
       }
       if(bytesRead > 0){
           String receivedData = readBufferData();
           msgProcessor.appendRecvdString(receivedData);
           while (msgProcessor.hasMsg()){
              String msg = msgProcessor.nextMsg();
              displayMsg(MsgProcessor.msgBody(msg));
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
        SocketChannel socketChannel = (SocketChannel)key.channel();
        synchronized (pendindDataToSend){
            Queue<ByteBuffer> queue = this.pendindDataToSend.get(socketChannel);
            while (!queue.isEmpty()){
                ByteBuffer dataBuf = queue.peek();
                int written = socketChannel.write(dataBuf);
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
            if(queue.isEmpty()){
                key.interestOps(SelectionKey.OP_READ);
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
            pendingChanges.add(new RequestTypeHandler(socketChannel,
                    ConstantValues.REGISTERINTOPS,SelectionKey.OP_CONNECT) );
        }
        connected = true;
    }

    public void initialiseGame() throws IOException {
        encodeMsg(MsgType.START.toString());
    }
    public void sendGuess(String guess) throws IOException {
        encodeMsg(MsgType.GUESS.toString(),guess);
    }
    public void playGame() throws IOException {
        encodeMsg(MsgType.PLAY.toString(),null);
    }

    /**
     * Save Encoded message to buffer
     * @param msgParts
     * @throws IOException
     */
    public void encodeMsg(String... msgParts) throws IOException {
        StringJoiner joiner = new StringJoiner(ConstantValues.MSG_TYPE_DELIMETER);
        for(String part: msgParts){
            joiner.add(part);
        }
        String encodedMsg = MsgProcessor.appendLenHeader(joiner.toString());
       synchronized (pendindDataToSend){
           Queue<ByteBuffer> dataFromQueue,dataToQueue;
           dataFromQueue =  this.pendindDataToSend.get(socketChannel);
           if(dataFromQueue == null){
               dataToQueue = new ArrayDeque<>();
               this.pendindDataToSend.put(socketChannel,dataToQueue);
           }
           dataFromQueue.add(ByteBuffer.wrap(encodedMsg.getBytes()));
       }
    }

}
