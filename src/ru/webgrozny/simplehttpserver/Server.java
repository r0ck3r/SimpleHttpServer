package ru.webgrozny.simplehttpserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class Server {
    private int port;
    private ServerSocket serverSocket;
    private boolean started = false;
    private ProviderGenerator providerGenerator;
    private ServerSettings serverSettings;

    public Server(ServerSettings serverSettings) {
        this.serverSettings = serverSettings;
        this.providerGenerator = serverSettings.getProviderGenerator();
        this.port = serverSettings.getPort();
    }

    public void start() {
        if(!started) {
            started = true;
            try {
                serverSocket = new ServerSocket(port);
                while (started) {
                    try {
                        appendClient(serverSocket.accept());
                    } catch (SocketException e) {

                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        if(started) {
            started = false;
            try {
                serverSocket.close();
            } catch (IOException e) {

            }
        }
    }

    private void appendClient(Socket socket) {
        Runnable thread = new Runnable() {
            @Override
            public void run() {
                ContentProvider contentProvider = providerGenerator.generate();
                if(contentProvider == null) {
                    contentProvider = new ContentProvider() {
                        @Override
                        public void execute() {

                        }
                    };
                }
                contentProvider.start(socket, serverSettings);
                try {
                    socket.close();
                } catch (IOException e) {

                }
            }
        };
        new Thread(thread).start();
    }
}
