package ru.webgrozny.simplehttpserver.exceptions;

public class ServerBindException extends Exception{
    private String bindTo;
    private int port;

    public ServerBindException(String bindTo, int port) {
        this.bindTo = bindTo;
        this.port = port;
    }

    public String getMesasge() {
        return "Can not bind to " + bindTo + ":" + port;
    }

    public String getBindTo() {
        return bindTo;
    }

    public int getPort() {
        return port;
    }
}
