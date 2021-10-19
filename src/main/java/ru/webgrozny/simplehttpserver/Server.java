package ru.webgrozny.simplehttpserver;

import javax.net.ServerSocketFactory;
import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
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

    public void start() {
        if(!started) {
            started = true;
            try {
                if(serverSettings.getJksKey() != null && serverSettings.getJksPass() != null) {
                    String jksFile = serverSettings.getJksKey();
                    String jksPass = serverSettings.getJksPass();
                    try {
                        KeyStore ks = KeyStore.getInstance("JKS");
                        ks.load(new FileInputStream(jksFile), jksPass.toCharArray());
                        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                        kmf.init(ks, jksPass.toCharArray());
                        sslContext = SSLContext.getInstance("TLS");
                        sslContext.init(kmf.getKeyManagers(), null, null);
                    } catch (KeyStoreException e) {
                        e.printStackTrace();
                    } catch (CertificateException e) {
                        e.printStackTrace();
                    } catch (UnrecoverableKeyException e) {
                        e.printStackTrace();
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    } catch (KeyManagementException e) {
                        e.printStackTrace();
                    }
                }
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
