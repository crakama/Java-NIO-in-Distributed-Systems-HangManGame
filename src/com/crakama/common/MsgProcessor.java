package com.crakama.common;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.StringJoiner;

public class MsgProcessor {
    private StringBuilder recvdMsgParts = new StringBuilder();
    private final Queue<String> completeMsgs = new ArrayDeque<>();
    private boolean var;

    /**
     *  Takes is string messages and adds a length header to prepare it for send
     */
    public static String appendLenHeader(String msg) {
        StringJoiner joiner = new StringJoiner(ConstantValues.MSG_LEN_DELIMETER);
        joiner.add(Integer.toString(msg.length()));
        joiner.add(msg);
        return joiner.toString();
    }


    public synchronized boolean hasMsg(){
        return !completeMsgs.isEmpty();
    }

    public synchronized String nextMsg(){
        return completeMsgs.poll();
    }
    /**
     * Appends a newly received string to previously received strings.
     * @param recvdString The received string.
     */
    public synchronized void appendRecvdString(String recvdString) {

        recvdMsgParts.append(recvdString);
        while(extractMsg());
    }

    /**
     *  Receives and extract contents to prepare for display to user
     */

    private boolean extractMsg() {
        String allrecvdMsgParts = recvdMsgParts.toString();
        String[] msgSplits = allrecvdMsgParts.split(ConstantValues.MSG_LEN_DELIMETER);
        if (msgSplits.length < 2) {
            return false;
        }
        String lengthHeader = msgSplits[0];
        System.out.println("lengthHeader" +msgSplits[0]);
        String actualMsg = msgSplits[1];

        int expectedMsgLen = Integer.parseInt(lengthHeader);
        System.out.println("expectedMsgLen" +expectedMsgLen);
        int receivedMsgLen = Integer.parseInt(String.valueOf(actualMsg.length()));
        System.out.println("receivedMsgLen" +receivedMsgLen);

        if (receivedMsgLen >= expectedMsgLen) {
            String completeMsg = actualMsg.substring(0,expectedMsgLen);
            System.out.println("completeMsg" +completeMsg);
            completeMsgs.add(completeMsg);
            recvdMsgParts.delete(0, lengthHeader.length()
                    + ConstantValues.MSG_LEN_DELIMETER.length() + expectedMsgLen);
            return true;
        }
        return false;
    }

    /**
     * Returns the type of the specified message.
     */
    public static MsgType msgType(String msg) {
        String[] msgParts = msg.split(ConstantValues.MSG_TYPE_DELIMETER);
        return MsgType.valueOf(msgParts[ConstantValues.MSG_TYPE_INDEX].toUpperCase());
    }

    /**
     * Returns the body of the specified message.
     */
    public static String msgBody(String msg) {
        String[] msgParts = msg.split(ConstantValues.MSG_TYPE_DELIMETER);
        return msgParts[ConstantValues.MSG_BODY_INDEX];
    }

}
