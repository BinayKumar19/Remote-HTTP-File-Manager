package client;

import java.io.*;
import java.net.*;

/**
 * Http Client
 *
 * @author Binay
 */
public class Client {
    private String method;
    private String methodOption = null;
    private String url = null;
    private String format = null;
    private String filePath = null;
    private String inlineData = null;
    private String outputFileName = null;
    private String hostName = null;
    private Boolean verboseOption = false;
    private String userAgent = "Concordia-HTTP/1.0";
    private int serverPortNo = 80;

    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.println("Please provide the required parameters as a command line arguments");
            return;
        }

        Client client = new Client();
        client.setHttpParameters(args);

        switch (client.method) {
            case "help":
                client.helpInformationPrint(args.length);
                break;
            case "get":
                client.getMethod();
                break;
            case "post":
                client.postMethod();
                break;
            default:
                System.out.println("Invalid Input");
                break;
        }
    }

    /**
     * To set HTTP request Parameters according to the input command
     *
     * @param inputParameters command line input parameters
     */
    private void setHttpParameters(String[] inputParameters) {
        method = inputParameters[0];

        for (int i = 0; i < inputParameters.length; i++) {
            switch (inputParameters[i]) {
                case "-v":
                    verboseOption = true;
                    break;
                case "-h":
                    if (format == null)
                        format = inputParameters[i + 1];
                    else
                        format = format + " " + inputParameters[i + 1];
                    break;
                case "-d":
                    inlineData = inputParameters[i + 1];
                    break;
                case "-f":
                    filePath = inputParameters[i + 1];
                    break;
                case "-o":
                    outputFileName = inputParameters[i + 1];
                    break;
                case "-L":
                    method = "get";
                    break;
                default:
                    break;
            }
            if (inputParameters[i].contains("http:") ||
                    inputParameters[i].contains("www.") ||
                    inputParameters[i].contains("https:"))
                url = inputParameters[i];
        }

        if (url != null && url.contains("http://"))
            hostName = (url.substring(0, url.replace("http://", "").indexOf("/") + 7)).replace("http://", "www.");
        else if (url != null && url.contains("www."))
            hostName = url.substring(0, url.indexOf("/"));
        else if (url.contains("localhost")) {
            serverPortNo = Integer.parseInt(url.substring(17, 21));
            url = url.substring(21);
            hostName = "localhost";
            verboseOption = true;
        }

        if (inputParameters.length == 2 && (inputParameters[1].equals("get") || inputParameters[1].equals("post")))
            methodOption = inputParameters[1];
    }

    /**
     * Function to display help for the HTTP application
     *
     * @param inputParametersLength
     */
    private void helpInformationPrint(int inputParametersLength) {
        if (inputParametersLength == 1) {
            System.out.println("httpc is a curl-like application but supports HTTP protocol only.\n"
                    + "Usage:httpc command [arguments]\nThe commands are:\n"
                    + " get    executes a HTTP GET request and prints the response.\n"
                    + " post   executes a HTTP POST request and prints the response.\n"
                    + " help   prints this screen.\n\n"
                    + "Use \"httpc help [command]\" for more information about a command.");

        } else if (methodOption.equals("get")) {
            System.out.println("usage: httpc get [-v] [-h key:value] URL\n"
                    + "Get executes a HTTP GET request for a given URL.\n"
                    + " -v           Prints the detail of the response such as protocol, status, and headers.\n"
                    + " -h key:value Associates headers to HTTP Request with the format 'key:value'.");

        } else if (methodOption.equals("post")) {
            System.out.println("usage: httpc post [-v] [-h key:value] [-d inline-data] [-f file] URL\n"
                    + "Post executes a HTTP POST request for a given URL with inline data or from file.\n"
                    + " -v  Prints the detail of the response such as protocol, status, and headers.\n"
                    + " -h  key:value Associates headers to HTTP Request with the format 'key:value'.\n"
                    + " -d  string Associates an inline data to the body HTTP POST request.\n"
                    + " -f  file Associates the content of a file to the body HTTP POST request.\n\n"
                    + "Either [-d] or [-f] can be used but not both.");
        } else {
            System.out.println("Invalid Option");
        }
    }

    /**
     * To send get request
     */
    private void getMethod() {

        InputStream inputStream = sendRequest("GET");
        readServerResponse(true, inputStream);
    }

    /**
     * To send post request
     */
    private void postMethod() {
        if (filePath != null) {
            readInlineDataFromFile();
        }
        InputStream inputStream = sendRequest("POST");
        readServerResponse(false, inputStream);

    }

    /**
     * Read inline data from a file
     */
    private void readInlineDataFromFile() {
        String currentLine;

        BufferedReader br;
        FileReader fr;
        try {
            fr = new FileReader(filePath);
            br = new BufferedReader(fr);

            while ((currentLine = br.readLine()) != null) {
                if (inlineData == null)
                    inlineData = currentLine;
                else inlineData = inlineData + currentLine;
            }
            br.close();
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * To send the HTTP request
     *
     * @param method contains "GET" for get request and "POST" for post request
     * @return InputStream containing response
     */
    private InputStream sendRequest(String method) {
        Socket socket;

        try {
            InetAddress addr = InetAddress.getByName(hostName);
            socket = new Socket(addr, serverPortNo);

            BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            wr.write(method + " " + url + " HTTP/1.0\r\n");
            wr.write("Host:" + hostName + "\r\n");
            if (method.equals("POST") &&
                    inlineData != null) {
                wr.write("Content-Length:" + inlineData.length() + "\r\n");
            }

            wr.write(format + "\r\n");
            wr.write("User-Agent:" + userAgent + "\r\n");
            wr.write("\r\n");
            wr.write("");
            wr.flush();
            wr.close();
            return socket.getInputStream();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * To read the HTTP response
     *
     * @param getResponse contains true for get response and false for post response
     * @param inputStream containing response
     */
    private void readServerResponse(Boolean getResponse, InputStream inputStream) {
        String textFromServer;

        BufferedWriter output = null;
        try {

            //IF Server response has to be written in output file
            if (outputFileName != null) {
                FileWriter file = new FileWriter(outputFileName + ".txt", true);
                output = new BufferedWriter(file);
                output.append("Output of Get Request" + System.lineSeparator());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));

            while ((textFromServer = br.readLine()) != null) {
                if (getResponse &&
                        textFromServer.contains("302")) {
                    while ((textFromServer = br.readLine()) != null) {
                        if (textFromServer.contains("Location")) {
                            url = textFromServer.substring(10);
                            getMethod();
                            break;
                        }
                    }
                    break;
                }
                if (verboseOption ||
                        textFromServer.startsWith("{")) {
                    verboseOption = true;
                }

                if (verboseOption) {
                    if (outputFileName == null)
                        System.out.println(textFromServer);
                    else if (output != null)
                        output.append(textFromServer + System.lineSeparator());
                }
            }

            if (output != null)
                output.close();

            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}