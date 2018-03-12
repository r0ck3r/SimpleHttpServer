package ru.webgrozny.simplehttpserver;

import java.util.Arrays;
import java.util.List;

public class ServerSettings {
    private static List<String> DEFAULT_HEADERS = Arrays.asList(new String[]{"Content-type: text/html; charset=utf8"});

    private String documentRoot;
    private String postFileTempDir;
    private String directoryIndex;
    private int port;
    private ProviderGenerator providerGenerator;
    private List<String> defaultHeaders;

    private ServerSettings(String documentRoot, String postFileTempDir, String directoryIndex, int port) {
        this.documentRoot = documentRoot;
        this.postFileTempDir = postFileTempDir;
        this.directoryIndex = directoryIndex;
        this.port = port;
        providerGenerator = () -> null;
        defaultHeaders = DEFAULT_HEADERS;
    }

    public static ServerSettings createDefaultConfig() {
        return new ServerSettings("/var/www/html", "/tmp", "index.html", 80);
    }

    public ServerSettings setDocumentRoot(String documentRoot) {
        this.documentRoot = documentRoot;
        return this;
    }

    public ServerSettings setDirectoryIndex(String directoryIndex) {
        this.directoryIndex = directoryIndex;
        return this;
    }

    public ServerSettings setPort(int port) {
        this.port = port;
        return this;
    }

    public ServerSettings setPostFileTempDir(String postFileTempDir) {
        this.postFileTempDir = postFileTempDir;
        return this;
    }

    public ServerSettings setProviderGenerator(ProviderGenerator providerGenerator) {
        this.providerGenerator = providerGenerator;
        return this;
    }

    public ServerSettings setDefaultHeaders(List<String> defaultHeaders) {
        this.defaultHeaders = defaultHeaders;
        return this;
    }

    public String getDirectoryIndex() {
        return directoryIndex;
    }

    public String getDocumentRoot() {
        return documentRoot;
    }

    public int getPort() {
        return port;
    }

    public String getPostFileTempDir() {
        return postFileTempDir;
    }

    public ProviderGenerator getProviderGenerator() {
        return providerGenerator;
    }

    public List<String> getDefaultHeaders() {
        return defaultHeaders;
    }
}
