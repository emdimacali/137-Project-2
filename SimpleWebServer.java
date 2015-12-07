/***
	Dimacali, Ejandra Mae T.
	2012-45232
	Project 2 : Simple HTTP Server using Sockets
****/
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.swing.JOptionPane;

public class SimpleWebServer{
	 private ServerSocket s;
	 private String HTTPversion;
	 private String contentBeingFetched;
	 
	 private final int MAXSIZE = 1024;
	 
	 private static LinkedList<String> requestAttributes; //this has the request attributes
	 private static HashMap<String,String> requestAttribsWithVals; //this has the mapping of the request attributes with values
	 
	 private static LinkedList<String> urlParameters; // this has the url parameters (if ever it is present)
	 private static HashMap<String,String> urlParamsWithVals; //this has the mapping of the url parameters with values.
	 
	 private static String request;
	 private String statCode;
	 /**
	  * Creates and returns server socket.
	  * @param port Server port.
	  * @return created server socket
	  * @throws Exception Exception thrown, if socket cannot be created.
	  */
    protected ServerSocket getServerSocket(int port) throws Exception {
    	
        return new ServerSocket(port);
    }
    
    /**
     * Starts web server and handles web browser requests.
     * @param port Server port(ex. 80, 8080)
     * @throws Exception Exception thrown, if server fails to start.
     */
    public void runServer(int port) throws Exception {
        s = getServerSocket(port);
 
        while (true) {
            try {
            	//initialize
            	requestAttributes =  new LinkedList<String>();
            	requestAttribsWithVals = new HashMap<String,String>();
            	urlParameters =  new LinkedList<String>();
            	urlParamsWithVals = new HashMap<String,String>();
                Socket serverSocket = s.accept();
                handleRequest(serverSocket);
            } catch(IOException e) {
            	 System.out.println("Failed to start server: " + e.getMessage());
                System.exit(0);
                return;
            }
        }
    }
    
    /**
     * Handles web browser requests and returns a static web page to browser.
     * @param s socket connection between server and web browser.
     */
    public void handleRequest(Socket s) {
        BufferedReader is;     // inputStream from web browser
        PrintWriter os;        // outputStream to web browser
       
        try {
            String webServerAddress = s.getInetAddress().toString(  );
            System.out.println("Accepted connection from " + webServerAddress);
            is = new BufferedReader(new InputStreamReader(s.getInputStream()));
 
            request = is.readLine(); // the first line is always the request
            System.out.println("Server received request from client: " + request);
            
            String line;
            do{ //this just gets the header attributes from the client request
                line = is.readLine(); //succeeding lines will just be the supporting attributes that comprise the whole header of the request.
                if(request != null && !request.isEmpty()){
                	 
                	String attribs[] = line.split(":",2); 
	                if(attribs.length > 1){
	                	requestAttributes.add(attribs[0]); 
	                	requestAttribsWithVals.put(attribs[0],attribs[1]);
	                }
                }else{
                	break;
                }
            } while(line.length() != 0);
            
            //find the file specified in the request
             if(request != null && !request.isEmpty()){ //checks if request is null, if not null proceed.
	            String [] requestInfo = parseRequest(request); // requestInfo contains the <OPERATION TYPE> <PAGE TO FETCH> <HTTP VERSION>
	            os = new PrintWriter(s.getOutputStream(), true);
	           
	            //this is for the GET route.
	            contentBeingFetched = readFile(requestInfo[1]); //get the page content
	            this.HTTPversion = requestInfo[2];
	            
	            if(requestInfo[0].equals("POST")){
		            try{
		            	char[] params = new char[MAXSIZE];
		            	is.read(params);
		            	line = new String(params);
		            	//further parsing for toReturn[1].
	            		String[] req2 = line.split("&");
	            		for(int i=0;i<req2.length;i++){
	            			String[] req3 = req2[i].split("=");
	            			urlParameters.add(req3[0]);
	            			urlParamsWithVals.put(req3[0], req3[1]);
	            		}
		            	
		            }catch(Exception e){e.printStackTrace();} 
	            }
	            
	            //generate the html table 
	            String table = generateTable();
	            contentBeingFetched = mergeTableAndContent(table,contentBeingFetched);
	            
	            String header = "";
	            
	            //generate the response header based on the content read
	            switch(contentBeingFetched){
	            	case "":
	            		contentBeingFetched = readFile("404.html");
	            		contentBeingFetched = mergeTableAndContent(table,contentBeingFetched);
	            		header = returnResponseHeader("404");
	            		break;
	            	default:
	            		header = returnResponseHeader("200");
	            		break;
	            }
	            
	            //Header + table + page content
	            String totalGeneratedPage = header + "\r\n\r\n" + contentBeingFetched;
	            
	            
	            os = new PrintWriter(s.getOutputStream(), true);
	            os.println(totalGeneratedPage);

	            os.flush();
	            os.close();
	            
            }
            s.close();
        } catch (IOException e) {
            System.out.println("Failed to send response to client: " + e.getMessage());
        } finally {
        	if(s != null) {
        		try {
					s.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
        	}
        }
        return;
    }
    
    //this function merges the content with the table generated.
    public static String mergeTableAndContent(String table,String content){
    	String mergedContent = content.replaceAll("<body>", "<body>\r\n" + table);
    	return mergedContent;
    }
    
    //This function generates the table that appears before the actual page to be loaded.
    public static String generateTable(){
    	String toReturn = "";
    	toReturn += "<table>\r\n";
    	toReturn += "<tr>";
			toReturn += "<td>";
				toReturn += "<h3>Request Values</h3>";
			toReturn += "</td>";
	    toReturn += "</tr>";
	    toReturn += "<tr>";
			toReturn += "<td>";
				toReturn += "<h4>" + request + "</h4>";
			toReturn += "</td>";
		toReturn += "</tr>";
	    toReturn += "<tr>";
			toReturn += "<td>";
				toReturn += "<h4>Attribute</h4>";
			toReturn += "</td>";
			
			toReturn += "<td>";
				toReturn += "<h4>Value</h4>";
			toReturn += "</td>";
		toReturn += "</tr>";
    	
		for(int i=0;i<requestAttributes.size();i++){
    		String value = requestAttributes.get(i);
    		toReturn += "<tr>";
    			toReturn += "<td>";
    				toReturn += value;
    			toReturn += "</td>";
    			toReturn += "<td>";
					toReturn += requestAttribsWithVals.get(value);
				toReturn += "</td>";
    		toReturn += "</tr>";
    	}
		if(urlParameters.size() > 0){
	    	toReturn += "<tr>";
				toReturn += "<td>";
					toReturn += "<h3>Parameters</h3>";
				toReturn += "</td>";
			toReturn += "</tr>";
			
			toReturn += "<tr>";
				toReturn += "<td>";
					toReturn += "<h4>Name</h4>";
				toReturn += "</td>";
				toReturn += "<td>";
					toReturn += "<h4>Value</h4>";
				toReturn += "</td>";
			toReturn += "</tr>";
			
			for(int i=0;i<urlParameters.size();i++){
	    		String value = urlParameters.get(i);
	    		toReturn += "<tr>";
	    			toReturn += "<td>";
	    				toReturn += value;
	    			toReturn += "</td>";
	    			toReturn += "<td>";
						toReturn += urlParamsWithVals.get(value);
					toReturn += "</td>";
	    		toReturn += "</tr>";
	    	}
		}
    	toReturn += "</table>\r\n";
    	
    	return toReturn;
    }
    
    //This function returns a String array with the following assignments :
    // index 0 - request type ; index 1 - file to open ; index 2 - http protocol
    public static String[] parseRequest(String request){
    	int parseSize = 3; // edit if ever this needs to get big
    	StringTokenizer strtok = new StringTokenizer(request," ");
    	String[] toReturn = new String[parseSize];
    	
    	for(int i=0; i<parseSize;i++){
    		toReturn[i] = strtok.nextToken();
    	}
    	
    	//further parsing for toReturn[1].
    	String[] req = toReturn[1].split("[?]");
    	toReturn[1] = req[0];
    	if(req.length>1){ //this means parameters were appended
    		String[] req2 = req[1].split("&");
    		for(int i=0;i<req2.length;i++){
    			String[] req3 = req2[i].split("=");
    			urlParameters.add(req3[0]);
    			urlParamsWithVals.put(req3[0], req3[1]);
    		}
    	}
    	return toReturn;
    }

  //this returns the read string from the text file specified , (String parameter : file extension to be read)
  	public String readFile(String fileName){
  		if(fileName.equals("/")){ // finding the index
  			String toReturn = "";
  			FileReader reader;
			try {
				reader = new FileReader("index.html");
				char[] chars = new char[(int) toReturn.length()];
		        reader.read(chars);
		        reader.close();
		        toReturn += new String(chars); //then append the content
			} catch (Exception e) {
				// TODO Auto-generated catch block
				
				return "";
			}			//adapted from stackoverflow.com solution
		       
		    return toReturn;
  		}else{
  			fileName = fileName.replaceAll("/", "");
  		}
  		
  		String content = "";
  		File file = new File(fileName);
  		  
	      try {
		       FileReader reader = new FileReader(file);			//adapted from stackoverflow.com solution
		       char[] chars = new char[(int) file.length()];
		       reader.read(chars);
		       reader.close();
		       content += new String(chars); //then append the content
		       return content;
	      }catch(Exception e){
	    	   //return this 404 error not found code to the browser;
			  return "";
	      }
  	}
  	
  	//this actually returns the responseHeader.
  	public String returnResponseHeader(String statusCode){
  		String response = "";
  		
  		switch(statusCode){
  			case "404":
  				response = this.HTTPversion + " 404 Not Found\r\n";
  				break;
  			case "200":
  				response = this.HTTPversion + " 200 OK\r\n";
  				response += "Content: " + getTypeOfContent() + "\r\n";
  				break;
  		}
  		response += "Content-Length: " + getContentLength() + "\r\n";
  		response += "Date: " + getDay() + ", " + getDate() + "\r\n";
  		response += "Server: Http\r\n";
  		
  		response += "Connection: close\r\n";
  		return response;
  	}
  	//this returns the content length
  	public int getContentLength(){
  		byte[] converted = contentBeingFetched.getBytes();
  		return contentBeingFetched.length();
  	}
  	//returns type content of the file being requested
  	public String getTypeOfContent(){
  		String[] req = parseRequest(request);
  		String file = req[1].replaceAll("/","");
  		if(file.endsWith("html"))
  			return "text/html";
  		if(file.endsWith("css"))
  			return "text/css";
  		if(file.endsWith("js"))
  			return "application/javascript"; //text/javascript is obsolete
  		return null; // if file type cannot be found, just return null first.
  	}
  	//gets the current time stamp formatted for the header
  	public String getDay(){
  		Calendar calendar = Calendar.getInstance();
  		int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
  		switch(dayOfWeek){
  			case Calendar.SUNDAY: return "Sun";
  			case Calendar.MONDAY: return "Mon";
  			case Calendar.TUESDAY: return "Tues";
  			case Calendar.WEDNESDAY: return "Wed";
  			case Calendar.THURSDAY: return "Thurs";
  			case Calendar.FRIDAY: return "Fri";
  			case Calendar.SATURDAY: return "Sat";
  		}
  		
  		return null; //fail-safe mechanism
  	}
  	
  	public String getDate(){
  		Date date = new Date();
  		SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM YYYY HH:mm:ss z");
 	   //get current date time with Date()
  	   dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
 	   String stringedDate = dateFormat.format(date);
 	   return stringedDate;
  	}

}

