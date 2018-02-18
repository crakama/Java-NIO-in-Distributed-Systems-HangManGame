package com.crakama.server.service;

import com.crakama.server.net.ClientSession;

public interface GameStatusListener {
    /**
     * Called when a new game status message at the model layer is ready to be sent to client.
     * @param status new game status.
     */

    void gameStatus(ClientSession clientSession,String status);
}
