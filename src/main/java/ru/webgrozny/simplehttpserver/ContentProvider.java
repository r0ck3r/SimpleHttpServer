package ru.webgrozny.simplehttpserver;


import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public abstract class ContentProvider {
    private String queryString = null;
    private Socket plainSocket;
    private Socket usingSocket;
    private InputStream socketInputStream;
    private OutputStream socketOutputStream;
    private List<String> headers = new ArrayList<>();
    private Map<String, String> headersData = new HashMap<>();
    private String path;
    private HashMap<String, String> get = new HashMap<>();
    private List<String> responseHeaders = new ArrayList<>();
    private ServerStatus serverStatus = ServerStatus.OK;
    private RequestMethod requestMethod;
    private EncType encType = EncType.WWW_FORM;
    private String boundary = "";
    private int boundayLength = 0;
    private HashMap<String, String> post = new HashMap<>();
    private int postRest = 0;
    private Map<String, PostFile> postFiles = new HashMap<>();
    private boolean headersSent = false;
    private ServerSettings serverSettings;
    ByteArrayInputStream socketFirstByteInputStream;
    private boolean httpOnHttpsRequested = false;
    private String rawPost;

    private String documentRoot;
    private String fileUploadTemp;
    private String directoryIndex;

    public void start(Socket socketParameter, ServerSettings serverSettings, SSLContext sslContext) {
        this.serverSettings = serverSettings;

        try {
            documentRoot = serverSettings.getDocumentRoot();
            fileUploadTemp = serverSettings.getPostFileTempDir();
            directoryIndex = serverSettings.getDirectoryIndex();

            plainSocket = socketParameter;
            if (sslContext != null) {
                SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                int c = plainSocket.getInputStream().read();
                socketFirstByteInputStream = new ByteArrayInputStream(new byte[]{(byte) c});
                if (c == 22) { //SSL Connection is starting
                    usingSocket = sslSocketFactory.createSocket(plainSocket, socketFirstByteInputStream, true);
                    socketInputStream = usingSocket.getInputStream();
                } else {
                    usingSocket = plainSocket;
                    socketInputStream = new SequenceInputStream(socketFirstByteInputStream, usingSocket.getInputStream());
                    httpOnHttpsRequested = true;
                }
            } else {
                usingSocket = socketParameter;
                socketInputStream = usingSocket.getInputStream();
            }

            socketOutputStream = usingSocket.getOutputStream();
            usingSocket.setSoTimeout(10000);
            parseHeaders();
            usingSocket.setSoTimeout(60000);
            if (headers.size() > 0) {
                parseServerData();
                parseGet();
                if (httpOnHttpsRequested) {
                    plainOnSSLHelper();
                } else {
                    parsePost();
                    execute();
                }

                if (responseHeaders.size() > 0) {
                    sendHeaders();
                }

                if (!headersSent) {
                    if (serverSettings.isDocumentRootEnabled()) {
                        sendFile();
                    } else {
                        setAnswer(ServerStatus.SERVER_ERROR);
                        echo("Server error");
                    }
                }

                removePostFiles();
            }
        } catch (IOException e) {
            //can't work with stream!
        } catch (NullPointerException e) {
            //Socket probably is already closed
        } finally {
            try {
                socketOutputStream.close();
            } catch (Exception e) {
                System.out.println("Already closed socketOutputStream");
            }
            try {
                socketInputStream.close();
            } catch (Exception e) {
                System.out.println("Already closed socketInputStream");
            }
            try {
                usingSocket.close();
            } catch (Exception e) {
                System.out.println("Already closed usingsocket");
            }
            try {
                plainSocket.close();
            } catch (Exception e) {
                System.out.println("Already closed plainsocket");
            }
        }
    }

    private void plainOnSSLHelper() {
        switch (serverSettings.getPlainOnSSLOperation()) {
            case PROXY:
                break;
            case REJECT:
                sslReject();
                break;
            case REDIR:
                sslRedir();
                break;
        }
    }

    private void sslRedir() {
        setAnswer(ServerStatus.MOVED);
        int serverPort = getServerPort();
        String portString = "";
        if (serverPort != 443) {
            portString = ":" + serverPort;
        }
        setHeader("Location: https://" + getHost() + portString + getQueryString());
    }

    private void sslReject() {
        setAnswer(ServerStatus.SERVER_ERROR);
        echo("Plain HTTP is not allowed here");
    }

    private void sslProxy() {
        parsePost();
        execute();
    }

    private void sendHeaders() {
        if (!headersSent) {
            writeAnswer();
            if (responseHeaders.size() == 0) {
                responseHeaders = serverSettings.getDefaultHeaders();
            }
            for (String header : responseHeaders) {
                writeString(header + "\r\n");
            }
            writeString("\r\n");
            headersSent = true;
        }
    }

    private void parseHeaders() {
        try {
            boolean first = true;
            InputStream inputStream = socketInputStream;
            int c = 0;
            byte[] b = new byte[2048]; //bytes per line
            int iter = 0;
            while ((c = inputStream.read()) != -1) {
                char cChar = (char) c;
                if (iter == 0 && c == 22) {
                    usingSocket.close();
                    return;
                }
                if (cChar == '\n') {
                    String cLine = new String(b, 0, iter).replace("\r", "");
                    headers.add(cLine);
                    iter = 0;
                    if (cLine.equals("")) {
                        break;
                    }

                    //Headers name=>value storage
                    if (!first && cLine.contains(":")) {
                        String left = cLine.substring(0, cLine.indexOf(":"));
                        String right = null;
                        try {
                            right = cLine.substring(cLine.indexOf(":") + 1);
                        } catch (Exception e) {

                        }
                        if (right != null) {
                            headersData.put(left.trim().toLowerCase(), right.trim());
                        }
                    } else {

                    }
                    first = false;
                    //END
                } else {
                    b[iter++] = (byte) c;
                }
            }

        } catch (IOException e) {
            //e.printStackTrace();
        }
    }

    private void parseServerData() {
        if (headers.size() > 0 && (headers.get(0).startsWith("POST") || headers.get(0).startsWith("GET"))) {
            requestMethod = headers.get(0).startsWith("GET") ? RequestMethod.GET : RequestMethod.POST;
            try {
                queryString = headers.get(0).split(" ")[1];
                path = queryString.substring(1, !queryString.contains("?") ? queryString.length() : queryString.indexOf("?"));
            } catch (ArrayIndexOutOfBoundsException e) {
                setAnswer(ServerStatus.SERVER_ERROR);
                System.out.println("Bad request");
            }
        } else {

        }
    }

    private void parsePost() {

        int contentLength;
        try {
            contentLength = Integer.parseInt(getHeaderValue("content-length"));
        } catch (NumberFormatException e) {
            contentLength = 0;
        }
        int allowedPostSize = serverSettings.getMaxPostSize() * 1024 * 1024;
        if (requestMethod == RequestMethod.POST && contentLength > 0 && contentLength < allowedPostSize) {
            parseEncType();
            if (encType == EncType.WWW_FORM) {
                byte[] b = new byte[contentLength];
                try {
                    socketInputStream.read(b);
                    String postString = new String(b);
                    rawPost = postString;
                    String[] postData = postString.split("&");
                    for (String curPost : postData) {
                        String[] postVals = curPost.split("=");
                        if (postVals.length == 2) {
                            post.put(postVals[0], postVals[1]);
                        }
                    }
                } catch (IOException e) {

                }
            } else {
                postRest = contentLength;
                byte[] line = readLine(1024);
                while (line.length > 0) {
                    String cLine = new String(line);
                    if (cLine.startsWith("Content-Disposition:")) {
                        String[] parameters = cLine.split("; ");
                        String postInputName = parameters[1].split("=")[1].replace("\"", "").replace("\r", "").replace("\n", "");
                        if (cLine.contains("filename=\"")) {
                            String uploadFileName = parameters[2].split("=")[1].replace("\"", "").replace("\r", "").replace("\n", "");
                            String contentType = new String(readLine(1024)).replace("\r", "").replace("\n", "");
                            readLine(2);
                            String file = UUID.randomUUID().toString();
                            try {
                                String tmpFilePath = fileUploadTemp + "/" + file;
                                FileOutputStream fileOutputStream = new FileOutputStream(tmpFilePath);
                                readPostMultipartData(fileOutputStream);
                                fileOutputStream.close();
                                PostFile postFile = new PostFile(uploadFileName, tmpFilePath);
                                postFiles.put(postInputName, postFile);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            readLine(2);
                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            readPostMultipartData(byteArrayOutputStream);
                            byte[] post = byteArrayOutputStream.toByteArray();
                            if (post.length > 0) {
                                this.post.put(postInputName, new String(post));
                            }
                        }
                    }
                    line = readLine(1024);
                }
            }
        }
    }

    private void readPostMultipartData(OutputStream outputStream) {
        byte[] buffer1 = readLine(4096);
        if (!checkIsBoundary(buffer1)) {
            try {
                while (true) {
                    byte[] buffer2 = readLine(4096);
                    if (checkIsBoundary(buffer2)) {
                        outputStream.write(buffer1, 0, buffer1.length - 2);
                        break;
                    } else {
                        outputStream.write(buffer1);
                        buffer1 = buffer2;
                    }
                }
            } catch (IOException e) {

            }
        }
    }

    private boolean checkIsBoundary(byte[] line) {
        boolean ret = false;
        if (line.length >= boundayLength && line.length <= boundayLength + 7) { //7 указано примерно, так как впереди и после boundary браузеры добавляют --, а также запас для новой строки
            String string = new String(line);
            if (string.contains(boundary)) {
                ret = true;
            }
        }
        return ret;
    }

    private byte[] readLine(int buffer) {
        int current = 0;
        int count = 0;
        byte[] lineBuffer = new byte[buffer];
        while (postRest > 0) {
            try {
                if (count == buffer) {
                    break;
                }
                current = socketInputStream.read();
                postRest--;
                lineBuffer[count++] = (byte) current;
                if ((char) current == '\n') {
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Arrays.copyOf(lineBuffer, count);
    }

    private void parseEncType() {
        String contentType = getHeaderValue("content-type");
        encType = contentType != null && contentType.contains("multipart/form-data") ? EncType.MULTIPART : EncType.WWW_FORM;
        if (encType == EncType.MULTIPART) {
            String boundaryName = "boundary=";
            int pos = contentType.indexOf(boundaryName);
            int length = boundaryName.length();
            int start = pos + length;
            boundary = contentType.substring(start);
            boundayLength = boundary.getBytes().length;
        }
    }

    private void parseGet() {
        if (queryString.contains("?")) {
            String[] act = queryString.substring(queryString.indexOf("?") + 1).split("&");
            for (String current : act) {
                String[] values = current.split("=");
                if (values.length == 2) {
                    get.put(values[0], values[1]);
                }
            }
        }
    }

    private void writeAnswer() {
        String code = null;
        switch (serverStatus) {
            case OK:
                code = "HTTP/1.1 200 OK";
                break;
            case NOT_FOUND:
                code = "HTTP/1.1 404 Not Found";
                break;
            case MOVED:
                code = "HTTP/1.1 302 Moved Temporary";
                break;
            default:
                code = "HTTP/1.1 500 Internal Server Error";
        }

        try {
            socketOutputStream.write((code + "\r\n").getBytes());
        } catch (IOException e) {

        }
    }

    private void writeBytes(byte[] bytes, int start, int count) {
        try {
            byte[] toWrite = Arrays.copyOfRange(bytes, start, count);
            socketOutputStream.write(bytes);
        } catch (IOException e) {
            //
        }
    }

    private void writeBytes(byte[] bytes) {
        writeBytes(bytes, 0, bytes.length);
    }

    private void writeString(String string) {
        writeBytes(string.getBytes());
    }

    private void sendFile() {
        String path = getPath();
        if (path == null || path.equals("")) {
            path = directoryIndex;
        }
        try {
            if (path.contains("..")) {
                throw new IOException();
            }
            String fileName = documentRoot + "/" + path;
            InputStream inputStream = new FileInputStream(fileName);
            String contentType = Files.probeContentType(Paths.get(fileName));
            writeAnswer();
            writeString("Content-type: " + contentType + "\r\n");
            writeString("Content-length: " + inputStream.available() + "\r\n");
            writeString("\r\n");
            byte[] buffer = new byte[4096];
            int count;
            while ((count = inputStream.read(buffer)) > 0) {
                writeBytes(buffer, 0, count);
            }
            inputStream.close();
        } catch (FileNotFoundException e) {
            setAnswer(ServerStatus.NOT_FOUND);
            writeAnswer();
            writeString("\r\n");
            writeString("File " + path + " is not found on this server!");
        } catch (IOException e) {
            setAnswer(ServerStatus.SERVER_ERROR);
            writeAnswer();
            writeString("\r\n");
            writeString("Not allowed here!");
        }

    }

    private void removePostFiles() {
        Set<String> keys = postFiles.keySet();
        for (String key : keys) {
            PostFile postFile = postFiles.get(key);
            postFile.remove();
        }
    }

    public PostFile getPostFile(String name) {
        return postFiles.get(name);
    }

    public String post(String parameter) {
        return urlDecode(post.get(parameter));
    }

    public void setHeader(String header) {
        responseHeaders.add(header);
    }

    public void setAnswer(ServerStatus serverStatus) {
        this.serverStatus = serverStatus;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public String getQueryString() {
        return queryString;
    }

    public String getPath() {
        return path;
    }

    public RequestMethod getRequestMethod() {
        return requestMethod;
    }

    public String get(String parameter) {
        return urlDecode(get.get(parameter));
    }

    public String urlDecode(String string) {
        String ret = null;
        if (string != null) {
            try {
                ret = URLDecoder.decode(string, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                ret = null;
            }
        }
        return ret;
    }

    public String getRawPost() {
        return rawPost;
    }

    public String getHeaderValue(String header) {
        return headersData.get(header.toLowerCase());
    }

    public String getDocumentRoot() {
        return documentRoot;
    }

    public String getFileUploadTemp() {
        return fileUploadTemp;
    }

    public String getDirectoryIndex() {
        return directoryIndex;
    }

    public int getPort() {
        return serverSettings.getPort();
    }

    public String getHost() {
        String host = getHeaderValue("host");
        if (host.contains(":")) {
            return host.split(":")[0];
        } else {
            return host;
        }
    }

    public String getRemoteAddress() {
        return usingSocket.getInetAddress().getHostAddress();
    }

    public int getRemotePort() {
        return usingSocket.getPort();
    }

    public int getServerPort() {
        return usingSocket.getLocalPort();
    }

    public void echo(byte[] bytes) {
        if (!headersSent) {
            sendHeaders();
        }
        writeBytes(bytes);
    }

    public void echo(String string) {
        if (string == null) {
            echo("null");
        } else {
            echo(string.getBytes());
        }
    }

    public void echo(Object object) {
        if (object == null) {
            echo("null");
        } else {
            echo(object.toString());
        }
    }

    public abstract void execute();

}