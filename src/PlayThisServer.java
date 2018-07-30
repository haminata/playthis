import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;

import com.mysql.cj.xdevapi.DbDoc;
import com.mysql.cj.xdevapi.DbDocImpl;
import com.mysql.cj.xdevapi.JsonString;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Created by haminata on 26/05/2018.
 */

public class PlayThisServer {

    public static final String HTTP_METHOD_GET = "GET";
    public static final String HTTP_METHOD_POST = "POST";

    public static void main(String[] args) throws Exception {

        DbModel.register(User.class);
        DbModel.register(Musicroom.class);
        DbModel.register(Song.class);

        HttpServer server = HttpServer.create(new InetSocketAddress(2516), 0);

        server.createContext("/", new MyHandler());
        server.setExecutor(null); // creates a default executor

        server.start();
    }

    static class MyHandler implements HttpHandler {

        public void get(HttpExchange req) throws IOException {
            String response = "Not Found";
            int statusCode = 404;
            String path = req.getRequestURI().getPath();

            if (path.equals("/home") || path.equals("/")) {
                String filePath = "./assets/index.html";
                response = Utils.readFile(filePath);
            } else if (path.equals("/search")){
                req.getResponseHeaders().add("Content-Type", "application/json");
                response = "{\"hello\": \"world\"}";
                statusCode = 200;
            } else if(path.equals("/app.js")){
                response = Utils.readFile("./assets/app.js");
                statusCode = 200;
            } else if(path.equals("/schemas")){
                response = DbModel.schemas().toFormattedString();
                req.getResponseHeaders().add("Content-Type", "application/json");
                statusCode = 200;
            } else if(path.startsWith("/templates")){
                String fileParam = path.substring("/templates".length() + 1, path.length());
                String[] arr = fileParam.split("\\.", 2);

                DbModel obj = DbModel.build(arr[0]);

                if(obj != null){
                    String txt = arr.length == 2 ? obj.getTemplate(arr[1]) : obj.getTemplate();

                    DbDoc json = new DbDocImpl(){{
                        add("template", new JsonString(){{
                            setValue(txt);
                        }});
                    }};

                    response = json.toFormattedString();
                    req.getResponseHeaders().add("Content-Type", "application/json");
                    statusCode = 200;
                }

                System.out.println("[template] " + Arrays.toString(arr) + " | " + obj);

            } else if(DbModel.getModelPluralNames().containsKey(path.substring(1))){
                System.out.println("[plural model] " + path);
                Class<? extends DbModel> cls = DbModel.getModelPluralNames().get(path.substring(1));

                response = Utils.responseJson(DbModel.all(cls)).toFormattedString();
                statusCode = 200;
                req.getResponseHeaders().add("Content-Type", "application/json");
            } else {
                System.out.println("[not found] " + path);
            }

            req.sendResponseHeaders(statusCode, response.length());
            OutputStream os = req.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        public void post(HttpExchange req) throws IOException {
            String response = "Not Found";
            int statusCode = 404;
            String path = req.getRequestURI().getPath();

            if(DbModel.getModelPluralNames().containsKey(path.substring(1))){
                DbDoc json = Utils.jsonFromInput(req.getRequestBody());
                System.out.println("[plural model] " + json.toFormattedString());
                Class<? extends DbModel> cls = DbModel.getModelPluralNames().get(path.substring(1));

                DbModel model = DbModel.instance(cls);
                model.update(json);
                model.save();

                response = model.toJson().toFormattedString();
                statusCode = 200;
                req.getResponseHeaders().add("Content-Type", "text/html");
            }

            req.sendResponseHeaders(statusCode, response.length());
            OutputStream os = req.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        @Override
        public void handle(HttpExchange t) throws IOException {

            String reqQuery = t.getRequestURI().getQuery();
            String reqFragment = t.getRequestURI().getFragment();

            System.out.println("Request path is: " + t.getRequestURI().getPath());

            System.out.println("Current working directory is: " + System.getProperty("user.dir"));
            System.out.println("Request query is: " + reqQuery);
            System.out.println("Request hash is: " + reqFragment);
            System.out.println("Request method is: " + t.getRequestMethod());

            try {
                switch (t.getRequestMethod()) {
                    case HTTP_METHOD_GET:
                        get(t);
                        break;
                    case HTTP_METHOD_POST:
                        post(t);
                        break;
                }
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }

}












