import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.*;
    
public class RequestProcessor implements Runnable {

  private final static Logger logger = Logger.getLogger(
      RequestProcessor.class.getCanonicalName());

  private File rootDirectory;
  //redirect to index.html
  private String indexFileName = "index.html";
  private Socket connection;
  
  public RequestProcessor(File rootDirectory,
                          String indexFileName, Socket connection) {
        
    if (rootDirectory.isFile()) {
      throw new IllegalArgumentException(
          "rootDirectory must be a directory, not a file");   
    }
    try {
      rootDirectory = rootDirectory.getCanonicalFile();
    } catch (IOException ex) {
    }
    this.rootDirectory = rootDirectory;
    if (indexFileName != null) this.indexFileName = indexFileName;
    this.connection = connection;
  }
  
  @Override
  public void run() {
    // for security checks
    String root = rootDirectory.getPath();
    try {              
      OutputStream raw = new BufferedOutputStream(
                          connection.getOutputStream()
                         );         
      Writer out = new OutputStreamWriter(raw);
      Reader in = new InputStreamReader(
                   new BufferedInputStream(
                    connection.getInputStream()
                   ),"US-ASCII"
                  );
      StringBuilder requestLine = new StringBuilder();
      while (true) {
        int c = in.read();
        if (c == '\r' || c == '\n') break;
        requestLine.append((char) c);
      }
      
      String get = requestLine.toString();
      
      logger.info(connection.getRemoteSocketAddress() + " " + get);
      
      String[] tokens = get.split("\\s+");
      String method = tokens[0];
      String version = "";

      //----------------------------------------------------------------------------------------------------------------
      //GET REQUEST
      if (method.equals("GET")) {
        String fileName = tokens[1];
        if (fileName.endsWith("/")) fileName += indexFileName;
        String contentType =
                URLConnection.getFileNameMap().getContentTypeFor(fileName);
        if (tokens.length > 2) {
          version = tokens[2];
        }

        File theFile = new File(rootDirectory, 
            fileName.substring(1, fileName.length()));
        
        if (theFile.canRead() 
            // Don't let clients outside the document root
            && theFile.getCanonicalPath().startsWith(root)) {
          byte[] theData = Files.readAllBytes(theFile.toPath());
          if (version.startsWith("HTTP/")) { // send a MIME header
            sendHeader(out, "HTTP/1.0 200 OK", contentType, theData.length);
          } 
      
          // send the file; it may be an image or other binary data 
          // so use the underlying output stream 
          // instead of the writer
          raw.write(theData);
          raw.flush();
        } else { // can't find the file
          String body = new StringBuilder("<HTML>\r\n")
              .append("<HEAD><TITLE>File Not Found</TITLE>\r\n")
              .append("</HEAD>\r\n")
              .append("<BODY>")
              .append("<H1>HTTP Error 404: File Not Found</H1>\r\n")
              .append("</BODY></HTML>\r\n").toString();
          if (version.startsWith("HTTP/")) { // send a MIME header
            sendHeader(out, "HTTP/1.0 404 File Not Found", 
                "text/html; charset=utf-8", body.length());
          } 
          out.write(body);
          out.flush();
        }
      }


      //----------------------------------------------------------------------------------------------------------------
      //HEAD REQUEST
      else if(method.equals("HEAD")){
        String fileName = tokens[1];
        if (fileName.endsWith("/")) fileName += indexFileName;
        String contentType =
                URLConnection.getFileNameMap().getContentTypeFor(fileName);
        if (tokens.length > 2) {
          version = tokens[2];
        }

        File theFile = new File(rootDirectory,
                fileName.substring(1, fileName.length()));

        if (theFile.canRead()
                // Don't let clients outside the document root
                && theFile.getCanonicalPath().startsWith(root)) {
          if (version.startsWith("HTTP/")) {
            // send a header out
            sendHeader(out, "HTTP/1.0 200 OK", contentType, 0);
          }

        } else { // can't find the file
          if (version.startsWith("HTTP/")) { // send a MIME header
            sendHeader(out, "HTTP/1.0 404 File Not Found",
                    "text/html; charset=utf-8", 0);
          }
        }
      }

      //----------------------------------------------------------------------------------------------------------------
      //POST REQUEST
      else if (method.equals("POST")){

        // pattern check
        int num_stop = 0;

        String fileName = tokens[1];
        if (fileName.endsWith("/")) fileName += indexFileName;
        if (tokens.length > 2) {
          version = tokens[2];
        }

        File theFile = new File(rootDirectory,
                fileName.substring(1, fileName.length()));

        //get the request header
        StringBuilder requestHeader = new StringBuilder();

        while (true) {
          int c = in.read();
          if (c == '\r' || c == '\n') {
            num_stop++;
          }
          else { num_stop = 0;}
          requestHeader.append((char) c);
          if (num_stop >= 4) break;
        }

        // headerMap will save the headers to a HashMap for us to use later.
        Map<String, String> headerMap = new HashMap<>();
        for (String e : requestHeader.toString().split("\r\n")){
          headerMap.put(e.split(": ")[0], e.split(": ")[1]);
        }

        StringBuilder entity = new StringBuilder();
        // Record the body message and save it to a String.
        for (int i = 0; i < Integer.parseInt(headerMap.get("Content-Length")); i++){
          int c = in.read();
          entity.append((char) c);
        }

        // Entity
        Map<String, String> entityMap = new HashMap<>();
        for (String e : entity.toString().split("&")){
          entityMap.put(e.split("=")[0], e.split("=")[1]);
        }

        //Handle the POST request from form.html
        if (fileName.equals("/form.html")){
          if (theFile.canRead()
                  // Don't let clients outside the document root
                  && theFile.getCanonicalPath().startsWith(root)) {

            String body = new StringBuilder("<!DOCTYPE html>")
                    .append("<!DOCTYPE html>\n" + "<html lang=\"en\">\n" + "<head>\n" +
                            "    <meta charset=\"UTF-8\">\n" +
                            "    <title>Handling Post Request</title>\n" +
                            "    <style>\n" +
                            "     H1 {text-align: center;}  \n" +
                            "     p {text-align: center;}  \n" +
                            "    </style>\n")
                    .append("<HEAD><TITLE>Handling Post Request</TITLE>\r\n")
                    .append("</HEAD>\r\n")
                    .append("<BODY>")
                    .append("<H1>Your Rating of the day is " + entityMap.get("rating") + "</H1>\r\n")
                    .append("<P>--------Message Received--------:</P>\r\n" + "<P>" + entity.toString() +  "</P>\r\n")
                    .append("<P>----------Request Line----------: </P>\r\n" + "<P>" + requestLine.toString() + "</P>\r\n")
                    .append("<P>---------Request Header---------: </P>\r\n" + "<P>" +
                            requestHeader.toString().replace("\r\n", "<br>") + "</P>\r\n")
                    .append("</BODY></HTML>\r\n").toString();

            // Send out header
            if (version.startsWith("HTTP/")) { // send a MIME header
              sendHeader(out, "HTTP/1.0 200 OK", "text/html; charset=utf-8", body.length());
            }
            out.write(body);
            out.flush();
          }
        }

        //Handle the POST request from index.html
        else if (fileName.equals("/index.html")){

          StringBuilder accountManage = new StringBuilder();
          accountManage.append("<P> | AdminID: admin | Password: 000000 |</P>\r\n")
                  .append("<P> | UsersID: user01| Password: 111111 |</P>\r\n");
          String accountInfo = accountManage.toString();
          boolean adminOrUser = entityMap.get("username").equals("admin");

          String body = new StringBuilder("<!DOCTYPE html>")
                  .append("<!DOCTYPE html>\n" +
                          "<html lang=\"en\">\n" +
                          "<head>\n" +
                          "    <meta charset=\"UTF-8\">\n" +
                          "    <title>Title</title>\n" +
                          "    <style>\n" +
                          "        .main {\n" +
                          "            width: 100vw;\n" +
                          "            height: 100vh;\n" +
                          "            display: flex;\n" +
                          "            flex-direction: column;\n" +
                          "            align-items: center;\n" +
                          "            justify-content: center;\n" +
                          "        }\n" +
                          "\n" +
                          "        .content {\n" +
                          "            margin-top: 80px;\n" +
                          "        }\n" +
                          "\n" +
                          "        #suImg img{\n" +
                          "            position: absolute;\n" +
                          "            top: 0px;\n" +
                          "            left: 0px;\n" +
                          "            width: 35%;\n" +
                          "        }\n" +
                          "\n" +
                          "        body {\n" +
                          "            background-image:url('syr_bg.jpg');\n" +
                          "            background-repeat: repeat;\n" +
                          "        }\n" +
                          "\n" +
                          "    </style>\n" +
                          "\n" +
                          "</head>\n" +
                          "<body>\n" +
                          "<div id=\"suImg\">\n" +
                          "   <img src=\"syr_ecs.svg\" alt=\"\"/>\n" +
                          "</div>\n" +

                          "<div class=\"main\">\n" +
                          "   <marquee><span style=\"font-weight: bolder;font-size: " +
                          "80px;color: orange;\">Welcome!</span></marquee>\n" +
                          "   <h1>You logged in as " + entityMap.get("username") + "</h1>\n" +
                          "\n")
                  .append(adminOrUser ? "<h2>You have access to the account management system<h2>"
                          : "<h2>You do not have access to the account management system<h2>")
                  .append(adminOrUser ? accountInfo : "\n")
                  .append("</div>\n" +  "</body>\n" + "</html>").toString();


          if (version.startsWith("HTTP/")) { // send a MIME header
            sendHeader(out, "HTTP/1.0 200 OK", "text/html; charset=utf-8", body.length());
          }
          out.write(body);
          out.flush();
        }
      }

      //----------------------------------------------------------------------------------------------------------------
      //OPTIONS REQUEST
      else if(method.equals("OPTIONS")){
        String fileName = tokens[1];
        if (fileName.endsWith("/")) fileName += indexFileName;
        String contentType =
                URLConnection.getFileNameMap().getContentTypeFor(fileName);
        if (tokens.length > 2) {
          version = tokens[2];
        }

        File theFile = new File(rootDirectory,
                fileName.substring(1, fileName.length()));

        if (theFile.canRead()
                // Don't let clients outside the document root
                && theFile.getCanonicalPath().startsWith(root)) {
          if (version.startsWith("HTTP/")) { // send a MIME header
            sendOptionsHeader(out, "HTTP/1.0 200 OK", contentType);
          }

        } else { // can't find the file
          if (version.startsWith("HTTP/")) { // send a MIME header
            sendHeader(out, "HTTP/1.0 404 File Not Found",
                    "text/html; charset=utf-8", 0);
          }
       //out.flush();
        }
      }

      //----------------------------------------------------------------------------------------------------------------
      else { // method does not implemented
        String body = new StringBuilder("<HTML>\r\n")
            .append("<HEAD><TITLE>Not Implemented</TITLE>\r\n")
            .append("</HEAD>\r\n")
            .append("<BODY>")
            .append("<H1>HTTP Error 501: Not Implemented</H1>\r\n")
            .append("</BODY></HTML>\r\n").toString();
        if (version.startsWith("HTTP/")) { // send a MIME header
          sendHeader(out, "HTTP/1.0 501 Not Implemented", 
                    "text/html; charset=utf-8", body.length());
        }
        out.write(body);
        out.flush();
      }
    } catch (IOException ex) {
      logger.log(Level.WARNING, 
          "Error talking to " + connection.getRemoteSocketAddress(), ex);
    } finally {
      try {
        connection.close();        
      }
      catch (IOException ex) {} 
    }
  }

  //----------------------------------------------------------------------------------------------------------------
  private void sendHeader(Writer out, String responseCode,
      String contentType, int length)
      throws IOException {
    out.write(responseCode + "\r\n");
    Date now = new Date();
    out.write("Date: " + now + "\r\n");
    out.write("Server: JHTTP 2.0\r\n");
    out.write("Content-length: " + length + "\r\n");
    out.write("Content-type: " + contentType + "\r\n\r\n");
    out.flush();
  }

  //----------------------------------------------------------------------------------------------------------------
  private void sendOptionsHeader(Writer out, String responseCode,
                          String contentType)
          throws IOException {
    out.write(responseCode + "\r\n");
    Date now = new Date();
    out.write("Date: " + now + "\r\n");
    out.write("Access-Control-Allow-Methods: GET, HEAD, POST, OPTIONS\r\n");
    out.write("Content-type: " + contentType + "\r\n\r\n");
    out.flush();
  }
}