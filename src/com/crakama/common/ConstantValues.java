package com.crakama.common;

public class ConstantValues {
    /**
     * Separates a message length header.
     */
    public static final String MSG_LEN_DELIMETER = "#";
    /**
     * Separates a message type from the message body.
     */
    public static final String MSG_TYPE_DELIMETER = ",";


    public static final String NEXT = " Enter a command to proceed";

    public static final String MSG_START = "***-------------------------------------------------------------------------***\n\n";


    public static final String MSG_END = "\n\n***-------------------------------------------------------------------------***\n";


    public static final String NEWLINE = "\n";

    /**
     * The message type is the first token in a message.
     */
    public static final int MSG_TYPE_INDEX = 0;

    /**
     * The message body is the second token in a message.
     */
    public static final int MSG_BODY_INDEX = 1;

    public static final int REGISTERINTOPS = 1;
    public static final int CHANGEINTOPS = 2;
    public static final String FAILED_CONNECTION = "CONNECTION DID NOT FINISH!!!";

    public static int PORT_NUMBER = 8080;
}
