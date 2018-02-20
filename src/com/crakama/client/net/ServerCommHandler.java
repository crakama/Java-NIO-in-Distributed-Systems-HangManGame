package com.crakama.client.net;

import com.crakama.client.view.CmdType;
import com.crakama.common.ConstantValues;
import com.crakama.common.MsgProcessor;
import com.crakama.common.MsgType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import static com.crakama.client.view.CmdType.PLAY;
import static com.crakama.client.view.CmdType.START;

public class ServerCommHandler  implements Runnable{
    private InetSocketAddress serverAddress;
    private final Queue<ByteBuffer> pendindDataToSend = new ConcurrentLinkedQueue<>();
    private final MsgProcessor msgProcessor = new MsgProcessor();
    private ByteBuffer bufferedServerMsg = ByteBuffer.allocate(ConstantValues.BUFFER_SIZE);
    private List<OutputHandler> commListeners = new ArrayList<>();
    private List updateInterestOPS = new LinkedList();

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
            initialiseConnection();
            initSelector();
            while(true){
                getInterestOPs();
                selector.select();
                Iterator selectedKeys = selector.selectedKeys().iterator();
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
        }
    }

    private void getInterestOPs() {
        synchronized (updateInterestOPS) {
            Iterator interests = updateInterestOPS.iterator();
            while (interests.hasNext()) {
                ClientInterestOPs clientInterestOPs = (ClientInterestOPs) interests.next();
                if (clientInterestOPs.opsType == ConstantValues.READ_OR_WRITE) {
                        SelectionKey key = clientInterestOPs.socketChannel.keyFor(selector);
                        key.interestOps(clientInterestOPs.ops);
                }
            }
            this.updateInterestOPS.clear();
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
            System.out.println("bytesRead == -1");
            //TODO: Handle EOF scenario
        }
        if(bytesRead > 0){
            String receivedData = readBufferData();
            msgProcessor.appendRecvdString(receivedData);
            while (msgProcessor.hasMsg()){
                String msg = msgProcessor.nextMsg();
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
            while ((msg = pendindDataToSend.peek()) != null) {
                socketChannel.write(msg);
                if (msg.hasRemaining()) {
                    return;
                }
                pendindDataToSend.remove();
            }
            key.interestOps(SelectionKey.OP_READ);

    }

    public void connect(String host, int port,OutputHandler outputHandler) throws IOException {
        this.serverAddress = new InetSocketAddress(host,port);
        this.commListeners.add(outputHandler);
        new Thread(this).start();

    }

    private void initSelector() throws IOException {
        selector = Selector.open();
        socketChannel.register(selector, SelectionKey.OP_CONNECT);
    }
    public void initialiseConnection() throws IOException {
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(serverAddress);
    }

    public void changeOPs(){
        synchronized (this.updateInterestOPS){
            updateInterestOPS.add(new ClientInterestOPs(socketChannel,
                    ConstantValues.READ_OR_WRITE,SelectionKey.OP_WRITE));
        }
        selector.wakeup();
    }
    public void sendGuess(String guess) throws IOException {
        String encodedMsg = encodeMsg(MsgType.GUESS.toString(),guess);
        addToBuffer(encodedMsg);
        changeOPs();
    }
    public void playGame(CmdType cmd) {
        if(cmd.equals(START)){
           String encodedMsg = encodeMsg(MsgType.START.toString(),null);
           addToBuffer(encodedMsg);
           changeOPs();

        }else if(cmd.equals(PLAY)){
            encodeMsg(MsgType.PLAY.toString(),null);
            changeOPs();
        }

    }


    /**
     * Save Encoded message to buffer
     * @param msgParts
     * @throws IOException
     */
    public String encodeMsg(String... msgParts){
        StringJoiner joiner = new StringJoiner(ConstantValues.MSG_TYPE_DELIMETER);
        for(String part: msgParts){
            joiner.add(part);
        }
        String encodedMsg = MsgProcessor.appendLenHeader(joiner.toString());
        return encodedMsg;
    }

    public void addToBuffer(String encodedMsg){
        synchronized (pendindDataToSend){
            pendindDataToSend.add(ByteBuffer.wrap(encodedMsg.getBytes()));
        }
    }
    private static void processSelectorActions(Queue<Runnable> selectorActions) {
        Runnable action;
        while ((action = selectorActions.poll()) != null) {
            action.run();
        }
    }

}
