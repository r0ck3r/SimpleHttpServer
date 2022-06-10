package ru.webgrozny;

import ru.webgrozny.example.ExampleBackend;
import ru.webgrozny.simplehttpserver.ContentProvider;
import ru.webgrozny.simplehttpserver.ProviderGenerator;
import ru.webgrozny.simplehttpserver.Server;
import ru.webgrozny.simplehttpserver.ServerSettings;
import ru.webgrozny.simplehttpserver.exceptions.ServerBindException;
import ru.webgrozny.simplehttpserver.exceptions.SslInitException;

public class Main {
    public static void main(String[] args) {

        //Running https server
        new Thread(() -> {
            ServerSettings serverSettings = ServerSettings.createDefaultConfig()
                    .setDocumentRoot("/var/www/html")
                    .setPostFileTempDir("/tmp/web")
                    .disableDocumentRoot()
                    .setJKSKey("/home/user/.keystore", "storepass")
                    .setPort(10443)
                    .setProviderGenerator(new ProviderGenerator() {
                        @Override
                        public ContentProvider generate() {
                            return new ExampleBackend();
                        }
                    });
            try {
                new Server(serverSettings).start();
            } catch (ServerBindException e) {
                System.out.println(e.getMesasge());;
            } catch (SslInitException e) {
                System.out.println("Can not init SSL key");
            }
        }).start();

        //Running http server
        ServerSettings serverSettings2 = ServerSettings.createDefaultConfig()
                .setDocumentRoot("/var/www/html")
                .setPostFileTempDir("/tmp/web")
                .setBindTo("127.0.0.1")
                .setPort(1080)
                .disableDocumentRoot()
                .setProviderGenerator(new ProviderGenerator() {
                    @Override
                    public ContentProvider generate() {
                        return new ExampleBackend();
                    }
                });
        try {
            new Server(serverSettings2).start();
        } catch (ServerBindException e) {
            System.out.println(e.getMesasge());
        } catch (SslInitException e) {
            System.out.println("Can not init SSL key");
        }
    }
}
