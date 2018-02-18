package com.crakama.server.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class FileModel implements ReadFileInterface{
    private static FileReader fileReader;
    private static BufferedReader bufferedFileReader;
    ArrayList<String> dictionary = new ArrayList<>();

    /**
     * A class to server data base queries(DAO,Firebase e.t.c)
     */

    @Override
    public void readFile() {
        try {
            File inFile = new File("words.txt");
            fileReader = new FileReader(inFile);
            bufferedFileReader = new BufferedReader(fileReader);
            String currentLine = bufferedFileReader.readLine();
            while (currentLine != null) {
                dictionary.add(currentLine);
                currentLine = bufferedFileReader.readLine();
            }
            bufferedFileReader.close();
            fileReader.close();
        } catch(IOException e) {
            System.out.println("Could not Read From File");
        }
    }

    public String pickWord(){
        Random rand = new Random();
        int wordIndex = Math.abs(rand.nextInt()) % dictionary.size();
        return dictionary.get(wordIndex);
    }
}
