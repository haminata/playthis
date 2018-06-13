import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Created by haminata on 26/05/2018.
 */

public class PlayThisServer {

    public static void main(String[] args) throws Exception {
        Connection conn = DatabaseC.getConnection(args[0], args[1]);
        
        HttpServer server = HttpServer.create(new InetSocketAddress(2516), 0);
        server.createContext("/", new MyHandler(conn));
        server.setExecutor(null); // creates a default executor
        server.start();

    }

    static class MyHandler implements HttpHandler {

        public Connection conn;

        public MyHandler(Connection conn) {
            this.conn = conn;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            String response;

            String reqQuery = t.getRequestURI().getQuery();
            String reqFragment = t.getRequestURI().getFragment();

            StringBuilder contentBuilder = new StringBuilder();


            try {

                String path = "../playFile.html";
                System.out.println("Current working directory is: " + System.getProperty("user.dir"));
                System.out.println("Request query is: " + reqQuery);
                System.out.println("Request hash is: " + reqFragment);
                System.out.println("Path of boyFile is: " + path);



                BufferedReader in = new BufferedReader(new FileReader(path));
                String str;

                while ((str = in.readLine()) != null) {
                    contentBuilder.append(str);
                }
                in.close();

            } catch (IOException e) {
                System.err.println("[Error] " + e);
                contentBuilder.append("[Error] cant load HTML from file: " + e);
            }

            response = contentBuilder.toString();
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

}












