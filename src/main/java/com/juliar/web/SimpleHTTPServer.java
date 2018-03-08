package com.juliar.web;

import com.bugsnag.Bugsnag;
import com.juliar.Juliar;
import com.juliar.errors.JuliarLogger;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class SimpleHTTPServer {
    static File jarPath=new File(Juliar.class.getProtectionDomain().getCodeSource().getLocation().getPath());
    static String fullPath = jarPath.getParentFile().getAbsolutePath().replace("\\", "/");


    static private HttpServer server;

    static private Juliar compiler = new Juliar();
    static private String charset = "UTF-8";
    static private String contentType = "Content-Type";

    public static void main()  {
        try {
            server = HttpServer.create(new InetSocketAddress(Juliar.port), 0);

        } catch(IOException e){
            InetSocketAddress myport = new InetSocketAddress(0);
            try {
                server = HttpServer.create(myport, 0);
            } catch(IOException err){
                JuliarLogger.log(err);
            }
            Juliar.port = myport.getPort();
        }
        server.createContext("/", new MyHandler());
        server.createContext("/get", new GetHandler());
        server.createContext("/exit", new ExitHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    static class MyHandler implements HttpHandler {
        public void handle(HttpExchange httpExchange) throws IOException {
            String response = "";
            String uri = httpExchange.getRequestURI().toString();
            if(uri.isEmpty() || "/".equals(uri)){
                uri = "index.html";
            }
            //remove "/" exploit
            while(uri.startsWith("/")){
                uri = uri.substring(1);
            }
            String ext = getFileExtension(uri);

            if(Juliar.class.getResourceAsStream(uri) != null){
                response = new Scanner(Juliar.class.getResourceAsStream(uri), charset).useDelimiter("\\A").next();

            } else if(new File(fullPath+uri).exists()){
                response = new String(Files.readAllBytes(Paths.get(fullPath+uri)));
            }

            SimpleHTTPServer.writeResponse(httpExchange, response,ext);
        }
        private static String getFileExtension(String name) {
            try {
                return name.substring(name.lastIndexOf('.') + 1);
            } catch (Exception e) {
                return "";
            }
        }
    }

    static class GetHandler implements HttpHandler {
        public void handle(HttpExchange httpExchange) throws IOException {
            StringBuilder response = new StringBuilder();
            String q = httpExchange.getRequestURI().getQuery();

            response.append("<html><body>");
            //Map <String,String> parms = SimpleHTTPServer.queryToMap(httpExchange.getRequestURI().getQuery());
            if(!q.isEmpty() && q.startsWith("q=")){
                response.append(doInPlaceInterpret(q.substring(2)));
            }
            else{
                response.append("File not found");
            }

            response.append("</body></html>");
            SimpleHTTPServer.writeResponse(httpExchange, response.toString(),"");
        }
        private static String doInPlaceInterpret( String theCode) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            // IMPORTANT: Save the old System.out!
            PrintStream old = System.out;
            // Tell Java to use your special stream
            System.setOut(ps);

            try{
                compiler.isDebug = false;
                InputStream inputStream = new ByteArrayInputStream(theCode.getBytes(charset));

                List<String> errors = compiler.compile(inputStream, ".", false);

                if (errors != null || errors.size() > 0) {
                    for (int i = 0; i < errors.size(); i++) {
                        System.out.println(errors.get(i));
                    }
                }

            }
            catch (Exception ex){
                throw ex;
            }
            // Put things back
            System.out.flush();
            System.setOut(old);
            return baos.toString();
        }
    }

    static class ExitHandler implements HttpHandler {
        public void handle(HttpExchange httpExchange) throws IOException {
            server.stop(0);
        }
    }


    public static void writeResponse(HttpExchange httpExchange, String response,String ext) throws IOException {
        Headers h = httpExchange.getResponseHeaders();

        switch (ext) {
            case "ico":
                h.add(contentType, "image/x-icon");
                break;
            case "jrl":
                h.add(contentType, "text/plain");
                break;
            case "svg":
                h.add(contentType, "image/svg+xml");
                break;
            case "html":
                h.add(contentType, "text/html");
                break;
            case "css":
                h.add(contentType, "text/css");
                break;
            case "js":
                h.add(contentType, "text/javascript");
                break;
            case "png":
                h.add(contentType, "image/png");
                break;
            case "jpeg":
            case "jpg":
                h.add(contentType, "image/jpeg");
                break;
            case "gif":
                h.add(contentType, "image/gif");
                break;
            case "pdf":
                h.add(contentType, "application/pdf");

                break;
            default:
                break;
        }

        byte[] newResponse = response.getBytes(charset);

        httpExchange.sendResponseHeaders(200, newResponse.length);
        OutputStream os = httpExchange.getResponseBody();
        os.write(newResponse, 0, newResponse.length);
        os.close();
    }

    public static Map<String, String> queryToMap(String query){
        Map<String, String> result = new HashMap<>();
        for (String param : query.split("&")) {
            String pair[] = param.split("=");
            if (pair.length>1) {
                result.put(pair[0], pair[1]);
            }else{
                result.put(pair[0], "");
            }
        }
        return result;
    }


}