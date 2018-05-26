import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
/**
 * Created by haminata on 26/05/2018.
 */
public class PlayThisServer {
    public static void main (String[] args)throws Exception {

            HttpServer server = HttpServer.create(new InetSocketAddress(2516), 0);
            server.createContext("/test", new MyHandler());
            server.setExecutor(null); // creates a default executor
            server.start();
        }

        static class MyHandler implements HttpHandler {
            @Override
            public void handle(HttpExchange t) throws IOException {
                String response = "<!DOCTYPE HTML>\n" +
                        "<html>\n" +
                        "<head>\n" +
                        "<title>PlayThis</title>\n" +
                        "</head>\n" +
                        "\n" +
                        "<body>\n" +
                        "<h1>Hello World!</h1>\n" +
                        "\n" +
                        "Insert: <input type=\"text\" value=\"\" id=\"nameinput\" placeholder=\"enter name...\">\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "<p>Click the button below to see your input</p>\n" +
                        "<button onclick=\"myFunction()\">Click me</button>\n" +
                        "<p id=\"demo\"></p>\n" +
                        "\n" +
                        "<script>\n" +
                        "function myFunction() {\n" +
                        "\tlet nameInput = document.getElementById('nameinput');\n" +
                        "\tlet name = nameInput.value;\n" +
                        "\tdocument.getElementById(\"demo\").innerHTML = \"What would you like me to do next \" + name + \"?\";\n" +
                        "}\n" +
                        "</script>\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "</body>\n" +
                        "</html>";
                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }

    }