package club.playthis.playthis;

import com.mysql.cj.xdevapi.DbDoc;
import com.mysql.cj.xdevapi.JsonParser;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.ws.rs.QueryParam;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@RestController
@RequestMapping("/")
public class AppController {

    @GetMapping("/schema")
    public String getSchema(){
        return DbModel.schemas().toFormattedString();
    }

    @GetMapping("/spotify_callback")
    public String callback(@QueryParam("code") String code){
        System.out.println("[Spotify] " + code);

        DbDoc body = null;
        String redirectUri = "";
        HttpClient httpClient = new DefaultHttpClient();
        String clientId = "96cb241b6b2446cb8fd48b68f1493871";
        String clientSecret = Utils.readFile("src/main/resources/static/appsecret.txt").trim();
        try {
            HttpPost request = new HttpPost("https://accounts.spotify.com/api/token");

            request.addHeader("Content-Type", "application/x-www-form-urlencoded");
            request.addHeader("Accept","application/json");

            String formEncoded = "client_id=" + clientId +
                    "&client_secret=" + clientSecret +
                    "&grant_type=client_credentials" +
                    "&code=" + code +
                    "&redirect_uri=" + redirectUri;
            request.setEntity(new StringEntity(formEncoded));

            HttpResponse response = httpClient.execute(request);
            String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
            body = JsonParser.parseDoc(responseString);

            // handle response here...
        }catch (Exception ex) {
            // handle exception here
            ex.printStackTrace();
        } finally {
            httpClient.getConnectionManager().shutdown();
        }

        if(body != null){
            System.out.println("[response]" + body.toFormattedString());
        }

        return "OK";
    }

    @GetMapping("/spotify_login")
    public ModelAndView spotify() throws UnsupportedEncodingException {
        String scopes = "streaming user-read-birthdate user-read-email user-read-private";
        scopes = (!scopes.isEmpty() ? "&scope=" + URLEncoder.encode(scopes, StandardCharsets.UTF_8.toString()) : "");

        String redirectUri = "http://localhost:2516/spotify_callback/";
        String rediretUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.toString());

        String url = "https://accounts.spotify.com/authorize" +
        "?response_type=code" +
                "&client_id=" + "96cb241b6b2446cb8fd48b68f1493871" +
                scopes + "&redirect_uri=" + rediretUri;


        String fin = url;
        return new ModelAndView("redirect:" + fin);
    }
}
