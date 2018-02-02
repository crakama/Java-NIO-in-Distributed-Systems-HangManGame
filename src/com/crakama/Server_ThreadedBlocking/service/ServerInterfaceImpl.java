package com.crakama.Server_ThreadedBlocking.service;

import com.crakama.Server_ThreadedBlocking.controller.Controller;
import com.crakama.Server_ThreadedBlocking.model.FileModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ServerInterfaceImpl  implements ServerInterface {

    private int failedAttempts = 0;
    private int score = 0; // +=1 when user score and -=1 when server score
    private String currentWord;
    private String currentGuess;
    private String hiddenWord = new String();
    private LinkedList<String> guesses= new LinkedList<String>();
    private final List<Controller> controllers = new ArrayList<>();
    public ServerInterfaceImpl(){

    }
    /**
     * Randomly pick a word from a file and put in a variable wordPicked, use output stream to send it to client.
     * @throws
     */


    @Override
    public void initialiseGame(){
        String welcomeMessage = "Welcome to HangMan Game. I will pick a word and you will try to guess it character by character.\n" +
                "If you guess wrong 6 times...I WIN! If you get the word before hand...YOU WIN!.\n" +
                "Every time you guess a character incorrectly, the number of trials will reduce by one \n" +
                "Every time you guess a character correctly, the letter will be filled in all its positions in the word\n +" +
                "Type START to begin!\n";

        sendResponse(welcomeMessage+"\nInitial Game Set Up" + informationMessage());
    }



    @Override
    public void playGame() throws IOException {
        generateNewWord();
        String s = "\n\nEnter a character that you think is in the word";
        sendResponse(":::Current Game Status:::" + informationMessage()+"\n" +
                "Current word picked is::::" + currentWord + s);
        //function that returns something or guess
        while (true) {
            String msg;
            synchronized (guesses){
                while (guesses.isEmpty()){
                    try {
                        guesses.wait();
                    }catch (InterruptedException e){
                        System.out.println("GUESS QUEUE IS EMPTY");
                    }
                }
                //If guesse's queue has data
                msg = guesses.peek();
            }

            if (msg.length() == 1) {

            //guesses.add(msg);
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
                    generateNewWord();
                    sendResponse("You win with " + failedAttempts + " number of fail attempts"+informationMessage());

                } else {// default presentation
                    sendResponse(informationMessage() + "\n Enter a character that you think is in the word ");
                }

            } else { // Wrong characther guess
                if (++failedAttempts > currentWord.length()) {
                    sendResponse("You loose, the correct word was " + currentWord + " ");

                    --this.score;//decrease score counter

                    generateNewWord();

                    //sends hidden word
                    sendResponse(informationMessage());
                } else {
                    sendResponse(informationMessage());

                }
            }
        }else{
                System.out.println("Full Word guessed");
            }
    }//while
    }

    @Override
    public void addController(Controller gameStatus) {
        controllers.add(gameStatus);
    }

    @Override
    public void getGuess(String msgBody) {
            guesses.add(msgBody);


    }

    public void sendResponse(String response){
        controllers.get(0).updateGameStatus(response);
    }
    /**
     * Generates a word that client shall guess on
     */
    private void generateNewWord() throws IOException {
        FileModel fileModel = new FileModel();
        guesses.clear(); //Empty guesses
        fileModel.readFile();
        failedAttempts = 0;
        currentWord = fileModel.pickWord();
        System.out.println("thread with id : "  + " get word: " + currentWord);

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
        for(String str : guesses){
            g.append(str + " ");
        }
        return "\nWord : " + hiddenWord + " \n(Length=" + hiddenWord.length()  + ")"+
                "\nFailed Attempts: " + this.failedAttempts +
                "\nScore: " + this.score +
                "\nGuesses: " + g.toString();
    }

}


