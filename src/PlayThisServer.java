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
        Connection conn = null;//DatabaseC.getConnection(args[0], args[1]);

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
            String response = "Not Found";
            int statusCode = 404;

            String reqQuery = t.getRequestURI().getQuery();
            String reqFragment = t.getRequestURI().getFragment();

            System.out.println("Request path is: " + t.getRequestURI().getPath());

            System.out.println("Current working directory is: " + System.getProperty("user.dir"));
            System.out.println("Request query is: " + reqQuery);
            System.out.println("Request hash is: " + reqFragment);

            StringBuilder contentBuilder = new StringBuilder();


            String path = t.getRequestURI().getPath();


            if (path.equals("/home") || path.equals("/")) {
                try {
                    String filePath = "./playThisFile.html";
                    System.out.println("File path is: " + filePath);


                    BufferedReader in = new BufferedReader(new FileReader(filePath));
                    String str;

                    while ((str = in.readLine()) != null) {
                        contentBuilder.append(str);
                    }
                    in.close();
                    response = contentBuilder.toString();

                } catch (IOException e) {
                    System.err.println("[Error] " + e);
                    contentBuilder.append("[Error] cant load HTML from file: " + e);
                }
            } else if (path.equals("/search")) {
                t.getResponseHeaders().add("Content-Type", "application/json");
                response = "{\"hello\": \"world\"}";
                statusCode = 200;
            } else if (path.equals("/musicrooms")) {
                response = "Got Music Rooms!";
                statusCode = 200;
            } else if(path.equals("/users")){
                response = "{\"user\": {" +
                "name\": \"Obialo\"," +
                        "email\": \"example.com\"," +
                        "age\": 17" +
                "}}";
                statusCode = 200;
                //String jsonString = this.readFile("users.json");
            }

            t.sendResponseHeaders(statusCode, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

}












