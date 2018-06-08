import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.io.BufferedReader;
import java.io.FileReader;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.sql.Connection;
import java.sql.DriverManager;


/**
* Created by haminata on 26/05/2018.
*/

public class PlayThisServer {
    public static void main (String[] args) throws Exception {

            HttpServer server = HttpServer.create(new InetSocketAddress(2516), 0);
            server.createContext("/", new MyHandler());
            server.setExecutor(null); // creates a default executor
            server.start();

            getConnection();

        }

        static class MyHandler implements HttpHandler {

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
                   public static Connection getConnection() throws Exception{
                       try{
                           String driver = "com.mysql.jdbc.Driver";
                           String url = "jdbc:mysql://localhost:2516/playthisTestDataB";
                           //String username = "hey";
                           //String password = "mypass";
                           Class.forName(driver);

                           Connection conn = DriverManager.getConnection(url);
                           System.out.println("Connected!");

                            return conn;
                       }catch(Exception e ){System.out.println(e + "Error");}

                       return null;

                   }


        }












