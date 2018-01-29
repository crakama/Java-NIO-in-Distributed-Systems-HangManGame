package com.crakama.Client.test;

import java.io.IOException;
import java.net.Socket;

public class testNumOfConnectedClients {
    public static void main(String[] args) throws InterruptedException {
        Socket[] sockets = new Socket[3000];
        for(int i = 0; i < sockets.length; i++){
            try {
                sockets[i] = new Socket("localhost",2123);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Thread.sleep(1000000000);
    }
}
