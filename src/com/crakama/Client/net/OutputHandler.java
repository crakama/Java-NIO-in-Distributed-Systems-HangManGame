package com.crakama.Client.net;

public interface OutputHandler {
    void handleServerResponse(String msg);

    void handleErrorResponse(Throwable connectionFailure);


    void informUser();
}
