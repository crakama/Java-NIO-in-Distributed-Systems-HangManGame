package com.crakama.client.view;

import com.crakama.client.net.OutputHandler;
import com.crakama.client.net.ServerCommHandler;
import com.crakama.common.ConstantValues;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;

public class CmdInterpreter implements Runnable{
    private boolean receivingCmds = false;
    private boolean isConnected = false;
    ServerCommHandler serverCommHandler;
    private BufferedReader bufferedReader;

    public void start(){
        receivingCmds = true;
        serverCommHandler = new ServerCommHandler();
        new Thread(this).start();
    }
    @Override
    public void run() {
        while (receivingCmds){
            try {
                CmdReader cmdReader = new CmdReader(requestHandler());
                switch (cmdReader.getCmd()){

                    case CONNECT:
                        serverCommHandler.connect(cmdReader.getParameters(1),
                                Integer.parseInt(cmdReader.getParameters(2)), new ServerResponse());
                        this.isConnected = true;
                        break;
                    case START:
                        if(this.isConnected = true){
                            serverCommHandler.initialiseGame();
                        }
                    case PLAY:
                        serverCommHandler.playGame();
                        break;
                    case QUIT:
                        break;
                    default:
                        serverCommHandler.sendGuess(cmdReader.getParameters(0));
                }

            }catch (Exception e){
            }
        }
    }
    /**
     * Handles user input from commandline
     */
    private String requestHandler() throws IOException {
            this.bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            String readBuffer = bufferedReader.readLine();
            return readBuffer;
        }


    private class ServerResponse implements OutputHandler {
        @Override
        public void handleServerResponse(String receivedMessage) {
            System.out.println(ConstantValues.MSG_START + receivedMessage+
                    ConstantValues.MSG_END);
        }

        @Override
        public void handleErrorResponse(Throwable connectionFailure) {
            System.out.println(ConstantValues.MSG_START + connectionFailure+
                    ConstantValues.MSG_END);
        }

        @Override
        public void notifyUser(InetSocketAddress inetSocketAddress) {
            System.out.println(ConstantValues.MSG_START +
                    "Connected to: " + inetSocketAddress.getHostName() +"on Port: " +
                    inetSocketAddress.getPort()+"\n"+ informUser()
                    + ConstantValues.MSG_END);
        }

        @Override
        public String informUser() {
            return ConstantValues.NEXT;
        }
    }
}