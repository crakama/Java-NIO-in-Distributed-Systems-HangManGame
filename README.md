### Threaded-Client-with-Blocking-Server-Observer Pattern

A simple java system that uses TCP sockets for communication. Its consists of several parts:

* Multithreaded commandline clientSession app
  - Socket Channels
  - Selector Keys
  - Interactive commandline interface
  - Customized Communication protocol(Enums and Serializable classes for messages and commands)
  - Layered architecture(Clear separation of concerns between classes) with **_Observer pattern_**

* Customized application specific communication protocol  
* Threaded Non Blocking Server
  - Socket Channels
  - Selector Keys
  
  ![Threaded Blocking Server](https://github.com/crakama/Java-NIO-in-Distributed-Systems-HangManGame/blob/master/src/com/crakama/images/ServerConnection.PNG)
  ![Threaded Blocking Server](https://github.com/crakama/Java-NIO-in-Distributed-Systems-HangManGame/blob/master/src/com/crakama/images/Multithreading.PNG)
   ![Threaded Blocking Server](https://github.com/crakama/Java-NIO-in-Distributed-Systems-HangManGame/blob/master/src/com/crakama/images/loose.PNG)
    ![Threaded Blocking Server](https://github.com/crakama/Java-NIO-in-Distributed-Systems-HangManGame/blob/master/src/com/crakama/images/win.PNG)
    ![Threaded Blocking Server](https://github.com/crakama/Java-NIO-in-Distributed-Systems-HangManGame/blob/master/src/com/crakama/images/MultipleClients.png)




