package com.crakama.server.service;

import com.crakama.server.model.FileModel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ServerInterfaceImpl  implements ServerInterface {

    private int failedAttempts = 0;
    private int score = 0; // +=1 when user score and -=1 when server score
    private String currentWord;
    private String hiddenWord = new String();
    private LinkedList<String> currentGuess= new LinkedList<String>();
   Map<SelectionKey,String> guess=new ConcurrentHashMap<>();
    private Queue<GameStatusListener> slisteners;
    public ServerInterfaceImpl(){

    }
    /**
     * Randomly pick a word from a file and put in a variable wordPicked, use output stream to send it to client.
     * @throws
     */



    @Override
    public String initialiseGame(){
        String welcomeMessage = "Welcome to HangMan Game. It will pick a word and you will try to guess it character by character.\n" +
                "If you guess wrongly the number of times equal to the length of the word...you loose. An incorrect character guess will reduce the number of trials by one \n" +
                "A correct guess, will be filled in all its positions in the word\n\n" +
                "Type START to begin!\n";
        return welcomeMessage;
    }


    @Override
    public void playGame(SelectionKey clientSessionKey) throws IOException {
        generateNewWord();
        String s = "\n\nEnter a character that you think is in the word";
        updateGameStatus(clientSessionKey,":::Current Game Status:::" + informationMessage()+"\n" +
                "Current word picked is::::" + currentWord + s );
        String msg;
        while (true) {

            if ((msg = guess.get(clientSessionKey))!= null) {
                guess.remove(clientSessionKey);
            if (msg.length() == 1) {

                currentGuess.add(msg);
                if (currentWord.contains(msg.toUpperCase()) || currentWord.contains(msg.toLowerCase())) { // Hit on characther
                    StringBuilder str = new StringBuilder();
                    for (int i = 0; i < currentWord.length(); i++) {

                        //If i character is matching at index position
                        if (currentWord.substring(i, i + 1).equalsIgnoreCase(msg.substring(0, 1))) {

                            str.append(msg.substring(0, 1).toLowerCase());
                        } else {//No char at position i+1 after 1st round of loop
                            if (hiddenWord.charAt(i) != '-') {
                                str.append(hiddenWord.charAt(i));
                            } else {
                                str.append("-");
                            }
                        }
                    }
                    hiddenWord = str.toString();
                    if (!hiddenWord.contains("-")) {
                        ++this.score;
                        //generateNewWord();
                        updateGameStatus(clientSessionKey, "You win with " + failedAttempts + " number of fail attempts" + informationMessage());

                    } else {// default presentation
                        updateGameStatus(clientSessionKey, informationMessage() + "\n Enter a character that you think is in the word ");
                    }

                } else { // Wrong characther guess
                    if (++failedAttempts > currentWord.length()) {
                        updateGameStatus(clientSessionKey, "You loose, the correct word was " + currentWord + " ");

                        --this.score;//decrease score counter

                        generateNewWord();

                        //sends hidden word
                        updateGameStatus(clientSessionKey, informationMessage());
                    } else {
                        updateGameStatus(clientSessionKey, informationMessage());

                    }
                }

            } else {
                System.out.println("Full Word guessed");
            }
        }
        else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
               // System.out.println("No Guesses found");
            }
        }//while
    }

    private void updateGameStatus(SelectionKey cKey, String gStatus){
        GameStatusListener lis;
        if( (lis = slisteners.peek())!= null){
            lis.gameStatus(cKey,gStatus);
        }
    }

    @Override
    public void getGuess(SelectionKey guessKey, String msgBody) {
            this.guess.put(guessKey,msgBody);

    }


    @Override
    public void addGameStatusListener(ConcurrentLinkedQueue<GameStatusListener> listeners, GameStatusListener gameOutPut) {
        this.slisteners= listeners;
        this.slisteners.add(gameOutPut);
    }


    /**
     * Generates a word that client shall guess on
     */
    private void generateNewWord(){
        FileModel fileModel = new FileModel();
        currentGuess.clear(); //Empty guesses
        fileModel.readFile();
        failedAttempts = 0;
        currentWord = fileModel.pickWord();
        System.out.println("thread with id : " + Thread.currentThread().getId() +
                " fetched word: " + currentWord);

        /*** Hide characters in word***/
        StringBuilder str = new StringBuilder();
        for(int i=0;i<currentWord.length(); i++){
            str.append("-");
        }
        hiddenWord = str.toString();
        /***                         ***/
    }

    /**
     *
     * @return - A suitible string to display in client console
     */
    private String informationMessage(){
        StringBuilder g = new StringBuilder();
        for(String str : currentGuess){
            g.append(str + " ");
        }
        return "\nWord : " + hiddenWord + " \n(Length=" + hiddenWord.length()  + ")"+
                "\nFailed Attempts: " + this.failedAttempts +
                "\nScore: " + this.score +
                "\nGuesses: " + g.toString();
    }

}


