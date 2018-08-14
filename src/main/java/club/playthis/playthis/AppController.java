package club.playthis.playthis;

import com.mysql.cj.xdevapi.DbDoc;
import com.mysql.cj.xdevapi.DbDocImpl;
import com.mysql.cj.xdevapi.JsonParser;
import com.mysql.cj.xdevapi.JsonString;
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
import java.util.Arrays;
import java.util.Base64;

@RestController
@RequestMapping("/")
public class AppController {

    @GetMapping("/schemas")
    public String getSchema(){
        return DbModel.schemas().toFormattedString();
    }

    @GetMapping("/spotify_token")
    public String getSpotify(){
        return User.findOne(User.class, new DbModel.Where(){{
            put(DbModel.ATTR_ID, "1");
        }}).spotifyAccesstoken().raw.toFormattedString();
    }

    public static final String CLIENT_ID = "e3966e30011d4895997ce89c797de5a5";

    @GetMapping("/spotify_callback")
    public String callback(@QueryParam("code") String code){
        System.out.println("[Spotify] " + code);

        DbDoc body = null;
        String redirectUri = "http://localhost:2516/spotify_callback/";
        HttpClient httpClient = new DefaultHttpClient();
        //String clientId = "96cb241b6b2446cb8fd48b68f1493871";
        String clientSecret = Utils.readFile("src/main/resources/static/appsecret.txt").trim();

        try {
            HttpPost request = new HttpPost("https://accounts.spotify.com/api/token");

            String authStr = CLIENT_ID + ":" + clientSecret;

            request.addHeader("Content-Type", "application/x-www-form-urlencoded");
            request.addHeader("Authorization", "Basic " + Base64.getUrlEncoder().encodeToString(authStr.getBytes()));

            System.out.println("Header: " + Arrays.toString(request.getAllHeaders()));

            String formEncoded = "grant_type=authorization_code" + //authorization_code client_credentials
                    "&code=" + code +
                    "&redirect_uri=" + redirectUri +
                    "&client_id=" + CLIENT_ID +
                    "&client_secret=" + clientSecret;

            request.setEntity(new StringEntity(formEncoded));

            HttpResponse response = httpClient.execute(request);
            String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
            body = JsonParser.parseDoc(responseString);

            // handle response here...
            User user = User.findOne(User.class, new DbModel.Where(){{
                put(DbModel.ATTR_ID, "1");
            }});

            user.update(new DbDocImpl(){{
                put(User.ATTR_SPOTIFY_ACCESSTOKEN, new JsonString(){{
                    setValue(responseString);
                }});
            }});
            user.save();
        }catch (Exception ex) {
            // handle exception here
            ex.printStackTrace();
        } finally {
            httpClient.getConnectionManager().shutdown();
        }

        if(body != null){
            System.out.println("[response]" + body.toFormattedString());
            return body.toFormattedString();
        }

        return "OK";
    }

    @GetMapping("/spotify_login")
    public ModelAndView spotify() throws UnsupportedEncodingException {
        String scopes = "streaming app-remote-control user-read-birthdate user-read-email user-read-private user-read-currently-playing user-modify-playback-state user-read-playback-state";

        scopes = (!scopes.isEmpty() ? "&scope=" + URLEncoder.encode(scopes, StandardCharsets.UTF_8.toString()) : "");

        String redirectUri = "http://localhost:2516/spotify_callback/";
        String rediretUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.toString());

        String url = "https://accounts.spotify.com/authorize" +
        "?response_type=code" +
                "&client_id=" + CLIENT_ID + "&show_dialog=true" +
                scopes + "&redirect_uri=" + rediretUri;


        String fin = url;
        return new ModelAndView("redirect:" + fin);
    }
}
