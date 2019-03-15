package server;

import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

public class HttpServer extends Thread {

    private Socket clientObj;
    private BufferedReader inputFromClient = null;
    private DataOutputStream outputToClient = null;
    private static String fileDirectory;
    private String status = null, contentType = "application/json";
    private String[] fileData = new String[10], fileNames = new String[10];
    private int fileLinesCount = 0, filesCount = 0, contentLength = 0;
    private static boolean debuggingOption = false;

    private HttpServer(Socket client) {
        clientObj = client;
    }

    public void run() {
        System.out.println("The client.Client " + clientObj.getInetAddress() + ":"
                + clientObj.getPort() + " is connected");

        String requestString = null, headerLine, httpMethod = "", httpPathLocal = null, shttpProtocol;
        StringTokenizer tokenizer;

        try {
            inputFromClient = new BufferedReader(new InputStreamReader(clientObj.getInputStream()));
            requestString = inputFromClient.readLine();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        headerLine = requestString;
        if (headerLine != null && !headerLine.isEmpty()) {
            tokenizer = new StringTokenizer(headerLine);
            httpMethod = tokenizer.nextToken();
            httpPathLocal = tokenizer.nextToken();
     //       shttpProtocol = tokenizer.nextToken();
        }

        StringBuffer responseBuffer = new StringBuffer();
        try {
            if (httpPathLocal.lastIndexOf('/') > 0) {
                status = "HTTP/1.1 403 Forbidden\r\n";
                sendResponse(status);
            }

            while (inputFromClient.ready()) {
                responseBuffer.append(requestString + "<BR>");
                requestString = inputFromClient.readLine();
                if (requestString.contains("Content-Type")) {
                    contentType = requestString;
                }
            }

            if (httpMethod.equals("GET") && httpPathLocal.length() == 1) {
                listFiles(fileDirectory);
                sendResponse(status);
            } else if (httpMethod.equals("GET") && httpPathLocal.length() > 1) {
                FileContentPrint(httpPathLocal);
                sendResponse(status);
            } else if (httpMethod.equals("POST")) {
                PostWriteFile(requestString, httpPathLocal);
                sendResponse(status);
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    private void sendResponse(String statusLine) throws Exception {
        String serverDetails = "Server: HTTP Server\r\n";
        String contentLengthLine;
        DateFormat dateFormat = new SimpleDateFormat("E,dd MMM yyyy HH:mm:ss");
        Date date = new Date();

        contentLengthLine = "Content-Length: " + contentLength + "\r\n";
        debuggingMsgs("Before Sending Output to client.Client", debuggingOption);

        OutputStream os = clientObj.getOutputStream();
        outputToClient = new DataOutputStream(os);

        outputToClient.writeBytes(statusLine);
        outputToClient.writeBytes(serverDetails);
        outputToClient.writeBytes("Date:" + dateFormat.format(date) + "\r\n");

        outputToClient.writeBytes(contentType + "\r\n");
        outputToClient.writeBytes(contentLengthLine);
        outputToClient.writeBytes("Connection: close\r\n");
        outputToClient.writeBytes("\r\n");
        if (fileLinesCount > 0) {
            for (int i = 0; i < fileLinesCount; i++) {
                outputToClient.writeBytes(fileData[i] + "\r\n");
            }
        } else if (filesCount > 0) {
            for (int i = 0; i < filesCount; i++) {
                outputToClient.writeBytes(fileNames[i] + "\r\n");
            }
        }
        outputToClient.close();
        debuggingMsgs("Output to client.Client Sent", debuggingOption);

    }

    private void listFiles(String directoryName) {
        int i = 0;
        directoryName = directoryName.replace('/', '\\');
        debuggingMsgs("Get Called with File List Option", debuggingOption);
        debuggingMsgs("Directory Path:" + directoryName, debuggingOption);

        File directory = new File(directoryName);
        //get all the files from a directory
        File[] fList = directory.listFiles();
        for (File file : fList) {
            if (file.isFile()) {
                fileNames[i] = file.getName();
                contentLength = contentLength + fileNames[i].length();
                i++;
                filesCount++;
            }
        }
        if (filesCount > 0) {
            status = "HTTP/1.1 200 OK\r\n";
            debuggingMsgs("Operation Successful", debuggingOption);
        } else {
            status = "HTTP/1.1 204 No Content\r\n";
            debuggingMsgs("Operation Un-successful", debuggingOption);
        }
    }

    private synchronized void FileContentPrint(String httpPath) {
        String currentLine;
        BufferedReader br;
        FileReader fr;
        httpPath = fileDirectory + httpPath;
        int i = 0;
        debuggingMsgs("Get Called for file content Print", debuggingOption);
        debuggingMsgs("File Path:" + httpPath, debuggingOption);

        try {
            fr = new FileReader(httpPath + ".txt");
            br = new BufferedReader(fr);
            while ((currentLine = br.readLine()) != null) {
                fileData[i] = currentLine;
                contentLength = contentLength + fileData[i].length();
                i++;
                fileLinesCount++;
            }
            br.close();
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
            status = "HTTP/1.1 404 NOT FOUND\r\n";
            return;
        }
        if (fileLinesCount > 0) {
            status = "HTTP/1.1 200 OK\r\n";
            debuggingMsgs("Operation Successful", debuggingOption);
        } else {
            status = "HTTP/1.1 204 No Content\r\n";
            debuggingMsgs("Operation Un-successful", debuggingOption);
        }
    }

    private synchronized void PostWriteFile(String sData, String sHttpPath) {
        BufferedWriter output;
        sHttpPath = fileDirectory + sHttpPath;
        debuggingMsgs("Post Called", debuggingOption);
        debuggingMsgs("File Path:" + sHttpPath, debuggingOption);
        debuggingMsgs("Data to Write:" + sData, debuggingOption);

        try {
            FileWriter file = new FileWriter(sHttpPath + ".txt", false);
            output = new BufferedWriter(file);
            output.write(sData + System.lineSeparator());
            output.close();
            status = "HTTP/1.1 200 OK\r\n";
            debuggingMsgs("Operation Successful", debuggingOption);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            status = "400 Bad Request\r\n";
            debuggingMsgs("Operation un-Successful", debuggingOption);
        }
    }

    private void debuggingMsgs(String msg, Boolean printOption) {
        if (printOption)
            System.out.println(msg);
    }

    public static void main(String[] args) throws Exception {
        int portNo = 8080;
        fileDirectory = "E:\\serverFiles";
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-v":
                    debuggingOption = true;
                    break;
                case "-p":
                    portNo = Integer.parseInt(args[i + 1]);
                    break;
                case "-d":
                    fileDirectory = args[i + 1];
                    break;
                default:
                    break;
            }
        }

        ServerSocket Server = new ServerSocket(portNo, 10000, InetAddress.getByName("127.0.0.1"));
        HttpServer hServer;
        System.out.println("Sever is running");
        while (true) {
            Socket connected = Server.accept();
            if (connected != null) {
                hServer = new HttpServer(connected);
                hServer.start();
            }
        }
    }
}