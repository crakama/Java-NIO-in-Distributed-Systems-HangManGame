package com.crakama.client.net;

import java.net.InetSocketAddress;

public interface OutputHandler {
    void handleServerResponse(String msg);

    void handleErrorResponse(Throwable connectionFailure);

    void notifyUser(InetSocketAddress inetSocketAddress);

    String informUser();
}
