package com.crakama.server.service;

import java.nio.channels.SelectionKey;

public interface GameStatusListener {
    /**
     * Called when a new game status message at the model layer is ready to be sent to client.
     * @param clientSession
     * @param status new game status.
     */

    void gameStatus(SelectionKey clientSession, String status);
}
