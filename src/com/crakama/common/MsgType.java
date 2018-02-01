package com.crakama.common;

public enum MsgType {

    /**
     * Start Command. A client sends this message to tell the game server its ready to play.
     */
    START,
    PLAY,
    GUESS,
    RESPONSE,

    /**
     * Stop Command. A client sends this message to tell the game server it wants to exit game.
     */
    STOP,

    /**
     * client is about to close, all server recourses related to the sending client should be
     * released.
     */
    DISCONNECT
}
