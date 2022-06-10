package ru.webgrozny.simplehttpserver;

import ru.webgrozny.simplehttpserver.exceptions.ServerBindException;
import ru.webgrozny.simplehttpserver.exceptions.SslInitException;

import javax.net.ServerSocketFactory;
import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.security.*;
import java.security.cert.CertificateException;

public class Server {
    private int port;
    private ServerSocket serverSocket;
    private boolean started = false;
    private ProviderGenerator providerGenerator;
    private ServerSettings serverSettings;
    private SSLContext sslContext = null;

    public Server(ServerSettings serverSettings) {
        this.serverSettings = serverSettings;
        this.providerGenerator = serverSettings.getProviderGenerator();
        this.port = serverSettings.getPort();
    }

    public void start() throws ServerBindException, SslInitException {
        if (!started) {
            started = true;
            try {
                if (serverSettings.getJksKey() != null && serverSettings.getJksPass() != null) {
                    String jksFile = serverSettings.getJksKey();
                    String jksPass = serverSettings.getJksPass();
                    try {
                        KeyStore ks = KeyStore.getInstance("JKS");
                        ks.load(new FileInputStream(jksFile), jksPass.toCharArray());
                        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                        kmf.init(ks, jksPass.toCharArray());
                        sslContext = SSLContext.getInstance("TLS");
                        sslContext.init(kmf.getKeyManagers(), null, null);
                    } catch (Exception e) {
                        throw new SslInitException();
                    }
                }
                if (serverSettings.getBindTo().equals("0.0.0.0")) {
                    serverSocket = new ServerSocket(port, serverSettings.getBackLog());
                } else {
                    serverSocket = new ServerSocket(port, serverSettings.getBackLog(), InetAddress.getByName(serverSettings.getBindTo()));
                }
                while (started) {
                    try {
                        appendClient(serverSocket.accept());
                    } catch (SocketException e) {

                    }
                }
            } catch (BindException e) {
                throw new ServerBindException(serverSettings.getBindTo(), serverSettings.getPort());
            } catch (IOException e) {
                System.out.println("Can't start server");
            }
        }
    }

    public void stop() {
        if (started) {
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
                if (contentProvider == null) {
                    contentProvider = new ContentProvider() {
                        @Override
                        public void execute() {

                        }
                    };
                }
                try {
                    contentProvider.start(socket, serverSettings, sslContext);
                } catch (Exception e) {
                    System.out.println("Server error: " + e.getMessage());
                }

                try {
                    socket.close();
                } catch (IOException e) {

                }
                System.gc();
            }
        };
        new Thread(thread).start();
    }
}
