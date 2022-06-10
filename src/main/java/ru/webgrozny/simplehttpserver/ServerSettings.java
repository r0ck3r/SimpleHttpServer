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
    private boolean documentRootEnabled = true;
    private String jksKey;
    private String jksPass;
    private int maxPostSize = 4;
    private PlainOnSSL operation;
    private String bindTo = "0.0.0.0";
    private int backLog = 64;

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

    public ServerSettings setMaxPostSize(int valueMB) {
        maxPostSize = valueMB;
        return this;
    }

    public ServerSettings setBindTo(String bindTo) {
        this.bindTo = bindTo;
        return this;
    }

    public ServerSettings setBacklog(int count) {
        this.backLog = count;
        return this;
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

    private void setDocumentRootEnabled(boolean enabled) {
        documentRootEnabled = enabled;
    }

    public ServerSettings disableDocumentRoot() {
        setDocumentRootEnabled(false);
        return this;
    }

    public ServerSettings setJKSKey(String key, String password) {
        jksKey = key;
        jksPass = password;
        return this;
    }

    public ServerSettings setPlainOnSSLOperation(PlainOnSSL operation) {
        this.operation = operation;
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

    public boolean isDocumentRootEnabled() {
        return documentRootEnabled;
    }

    public String getJksKey() {
        return jksKey;
    }

    public String getJksPass() {
        return jksPass;
    }

    public int getMaxPostSize() {
        return maxPostSize;
    }

    public PlainOnSSL getPlainOnSSLOperation() {
        return operation;
    }

    public String getBindTo() {
        return bindTo;
    }

    public int getBackLog() {
        return backLog;
    }
}
