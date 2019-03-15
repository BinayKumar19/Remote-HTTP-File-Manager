package server; /**
 *
 * @author binay
 */

import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

public class HttpServer extends Thread {

  private Socket clientObj = null;
  private BufferedReader inputFromClient = null;
  private DataOutputStream outputToClient = null;
  static String sFileDirectory;
  String sStatus=null,sContentType="application/json";
  String[] sFileData=new String[10],sFileNames=new String[10]; 
  int iFileLinesCount=0,iFilesCount=0,iContentTypeCount=0,icontentLength=0;
  static boolean bDebuggingFlag=false;
  
  public HttpServer(Socket client) {
    clientObj = client;
  }

  public void run() {
    System.out.println("The client.Client " + clientObj.getInetAddress() + ":"
        + clientObj.getPort() + " is connected");
  
    String requestString = null,headerLine,sHttpMethod = "",sHttpPathLocal = null,shttpProtocol;
    StringTokenizer tokenizer = null;
  
    try {
      inputFromClient = new BufferedReader(new InputStreamReader(clientObj.getInputStream()));
      requestString = inputFromClient.readLine();
    } catch (IOException e1) { e1.printStackTrace(); }

    headerLine = requestString;
    if (headerLine != null && !headerLine.isEmpty()) {
      tokenizer = new StringTokenizer(headerLine);
      sHttpMethod = tokenizer.nextToken();
      sHttpPathLocal = tokenizer.nextToken();
      shttpProtocol = tokenizer.nextToken();
    }       
      
  StringBuffer responseBuffer = new StringBuffer();
  try
    { if (sHttpPathLocal.lastIndexOf('/')>0)
        { sStatus="HTTP/1.1 403 Forbidden\r\n";
          sendResponse(sStatus);        
        }
        
      while (inputFromClient.ready()) 
      { responseBuffer.append(requestString + "<BR>");
        requestString = inputFromClient.readLine();
        if (requestString.contains("Content-Type"))
         { sContentType = requestString;    
          }
       }
    
     if (sHttpMethod.equals("GET") && sHttpPathLocal.length()==1) 
       { listFiles(sFileDirectory);
         sendResponse(sStatus);     
       }
      else if (sHttpMethod.equals("GET") && sHttpPathLocal.length()>1) 
       {  FileContentPrint(sHttpPathLocal);
          sendResponse(sStatus);     
       }
      else if (sHttpMethod.equals("POST")) 
       {   PostWriteFile(requestString,sHttpPathLocal);
           sendResponse(sStatus);     
       }
      } 
     catch (Exception e1) { e1.printStackTrace();} 
  }

 public void sendResponse(String statusLine) throws Exception {
    String serverdetails = "Server: HTTP Server\r\n";
    String contentLengthLine = null;                
    DateFormat dateFormat = new SimpleDateFormat("E,dd MMM yyyy HH:mm:ss");
    Date date = new Date();
    
    contentLengthLine = "Content-Length: " + icontentLength + "\r\n";
    debuggingMsgs("Before Sending Output to client.Client",bDebuggingFlag);
    
    OutputStream os = null;
    os = clientObj.getOutputStream();
    outputToClient = new DataOutputStream(os);

    outputToClient.writeBytes(statusLine);
    outputToClient.writeBytes(serverdetails);
    outputToClient.writeBytes("Date:"+dateFormat.format(date)+"\r\n");
     
    outputToClient.writeBytes(sContentType+"\r\n");
    outputToClient.writeBytes(contentLengthLine);
    outputToClient.writeBytes("Connection: close\r\n");
    outputToClient.writeBytes("\r\n");
     if (iFileLinesCount>0)
     {  for(int i=0;i<iFileLinesCount;i++)
         { outputToClient.writeBytes(sFileData[i]+"\r\n");
         }
     }
     else if (iFilesCount>0)
     {   for(int i=0;i<iFilesCount;i++)
         { outputToClient.writeBytes(sFileNames[i]+"\r\n");
         }
       }
    outputToClient.close();
    debuggingMsgs("Output to client.Client Sent",bDebuggingFlag);
  
 }
 
 public void listFiles(String directoryName){
    int i=0;
    directoryName.replace('/', '\\');
    debuggingMsgs("Get Called with File List Option",bDebuggingFlag);
    debuggingMsgs("Directory Path:"+directoryName,bDebuggingFlag);
   
    File directory = new File(directoryName);
        //get all the files from a directory
        File[] fList = directory.listFiles();
        for (File file : fList){
            if (file.isFile()){
               sFileNames[i]=file.getName();
               icontentLength=icontentLength+sFileNames[i].length();
               i++;
               iFilesCount++; 
            }
        }
    if (iFilesCount>0)
    { sStatus="HTTP/1.1 200 OK\r\n";   
      debuggingMsgs("Operation Successful",bDebuggingFlag);
    }
    else 
    { sStatus="HTTP/1.1 204 No Content\r\n";
      debuggingMsgs("Operation Un-successful",bDebuggingFlag);
    }   
 }
 
 public synchronized void FileContentPrint(String sHttpPath)
  {String sCurrentLine=null;
    BufferedReader br = null;
    FileReader fr = null;
    sHttpPath= sFileDirectory + sHttpPath;
    int i=0;
   debuggingMsgs("Get Called for file content Print",bDebuggingFlag);
   debuggingMsgs("File Path:"+sHttpPath,bDebuggingFlag);
   
    try {  fr = new FileReader(sHttpPath+".txt");
	     br = new BufferedReader(fr);
           	while ((sCurrentLine = br.readLine()) != null) {
            	        sFileData[i] = sCurrentLine;
                        icontentLength=icontentLength+sFileData[i].length();
                        i++;
                        iFileLinesCount++;
                }
                 br.close();
	         fr.close();
             } catch (IOException e) {
			e.printStackTrace();
                         sStatus="HTTP/1.1 404 NOT FOUND\r\n";
                       return;                         
		}
    if (iFileLinesCount>0)
    { sStatus="HTTP/1.1 200 OK\r\n";   
     debuggingMsgs("Operation Successful",bDebuggingFlag);
    }
    else 
    {sStatus="HTTP/1.1 204 No Content\r\n";
     debuggingMsgs("Operation Un-successful",bDebuggingFlag);
 }
  }     
  
 public synchronized void PostWriteFile(String sData,String sHttpPath)
   {BufferedWriter output=null;
    sHttpPath = sFileDirectory + sHttpPath;
    debuggingMsgs("Post Called",bDebuggingFlag);
    debuggingMsgs("File Path:"+sHttpPath,bDebuggingFlag);
    debuggingMsgs("Data to Write:"+sData,bDebuggingFlag);
   
    try{ 
      FileWriter file = new FileWriter(sHttpPath+".txt",false);  
      output = new BufferedWriter(file);
      output.write(sData+System.lineSeparator());
      output.close();
      sStatus="HTTP/1.1 200 OK\r\n";
      debuggingMsgs("Operation Successful",bDebuggingFlag);
      }catch(Exception e)
      {System.out.println(e.getMessage());
      sStatus="400 Bad Request\r\n";
      debuggingMsgs("Operation un-Successful",bDebuggingFlag);
      }
  }      
  
   public void debuggingMsgs(String msg,Boolean printFlag)
   {if (printFlag)
     System.out.println(msg);  
   }       
           
   public static void main(String args[]) throws Exception {
   int iPortNo = 8080;
   sFileDirectory = "E:\\serverFiles";
   for(int i=0;i<args.length;i++)
    {  switch (args[i]) {
         case "-v": bDebuggingFlag = true; 
                    break;
         case "-p": iPortNo=Integer.parseInt(args[i+1]);
                    break;
         case "-d": sFileDirectory = args[i+1];          
                    break;
         default:   break;
         }
     }
  
    ServerSocket Server = new ServerSocket(iPortNo, 10000, InetAddress.getByName("127.0.0.1"));
    HttpServer hServer;
    System.out.println("Sever is running");
    while (true) {
      Socket connected = Server.accept();
      if (connected != null)
      {hServer = new HttpServer(connected);
          hServer.start();
       };
    }
  }
}