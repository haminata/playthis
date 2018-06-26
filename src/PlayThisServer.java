import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.util.ArrayList;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Created by haminata on 26/05/2018.
 */

public class PlayThisServer {

    public static void main(String[] args) throws Exception {
        Connection conn = null;

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

        public static String readFile(String filePath){
            String fileData = null;
            try (BufferedReader in = new BufferedReader(new FileReader(filePath))){
                System.out.println("File path is: " + filePath);
                String str;

                StringBuilder contentBuilder = new StringBuilder();
                while ((str = in.readLine()) != null) {
                    contentBuilder.append(str);
                }

                fileData = contentBuilder.toString();
            } catch (IOException e) {
                System.err.println("[Error] unable to read \""+ filePath + "\": " + e);
            }
            return fileData;
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

            String path = t.getRequestURI().getPath();

            if (path.equals("/home") || path.equals("/")) {
                String filePath = "./assets/playThisFile.html";
                response = readFile(filePath);
            } else if (path.equals("/search")) {
                t.getResponseHeaders().add("Content-Type", "application/json");
                response = "{\"hello\": \"world\"}";
                statusCode = 200;
            } else if (path.equals("/musicrooms")) {
                ArrayList<Musicroom> musicroom = Musicroom.all(Musicroom.class);
                response = DbModel.manyToJson(musicroom);

                statusCode = 200;
            } else if(path.equals("/users")){
                ArrayList<User> users = User.all(User.class);
                response = DbModel.manyToJson(users);
                statusCode = 200;
                //String jsonString = this.readFile("users.json");
            } else if(path.equals("/app.js")){
                response = readFile("./assets/app.js");
                statusCode = 200;
            } else if(path.equals("/template_room.html")){
                t.getResponseHeaders().add("Content-Type", "text/plain");
                response = readFile("./assets/template_room.html");
                statusCode = 200;
            }

            t.sendResponseHeaders(statusCode, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

}












