package club.playthis.playthis;

import com.mysql.cj.xdevapi.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.HashMap;

/**
 * Created by haminata on 23/06/2018.
 */

class User extends DbModel {

    public static final String ATTR_SPOTIFY_ACCESSTOKEN = "spotify_accesstoken";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_PASSWORD_HASH = "password_hash";
    public static final String ATTR_PASSWORD = "password";


    public static final DelegatingPasswordEncoder PASSWORD_ENCODER = new DelegatingPasswordEncoder("bcrypt", new HashMap<String, PasswordEncoder>(){{
        put("bcrypt", new BCryptPasswordEncoder(4));
    }});

    @Override
    public String getModelNamePlural() {
        return "users";
    }

    @Override
    public HashMap<String, AttributeType> getAttributes() {

        return new HashMap<String, AttributeType>(){{
            put("email", AttributeType.STRING);
            put("name", AttributeType.STRING);
            put("gender", AttributeType.CHARACTER);
            put(ATTR_PASSWORD_HASH, AttributeType.TEXT);
            put(ATTR_PASSWORD, AttributeType.TEXT_VIRTUAL);
            put(ATTR_SPOTIFY_ACCESSTOKEN, new AttributeType(AttributeType.DATA_TYPE_STRING, 2500));
        }};
    }

    @Override
    public boolean save() {
        String psswd = (String) values.get(ATTR_PASSWORD);
        if(psswd != null) {
            update(new Utils.Json(ATTR_PASSWORD_HASH, PASSWORD_ENCODER.encode(psswd)));
            values.remove(ATTR_PASSWORD);
        }
        return super.save();
    }

    public String getName() {
        return (String) getValue("name");
    }

    @Override
    public String toString() {
        return "User(id=" + getId() + ", email=" + getValue("email") + ", name=" + getValue("email") +
                ", gender=" + getValue("gender") + ")";
    }

    static class Accesstoken {
        public String token, refreshToken, scope, tokenType;
        public Date createdAt;
        public Integer expiresIn;
        public DbDoc raw;

        public static Accesstoken fromString(String tokenStr){
            DbDoc json = JsonParser.parseDoc(tokenStr);

            Accesstoken tkn = new Accesstoken();
            tkn.raw = json;
            tkn.createdAt = Utils.extractDate(json,"created_at", null);
            tkn.expiresIn = ((JsonNumber) json.get("expires_in")).getInteger();
            tkn.token = ((JsonString) json.get("access_token")).getString();
            tkn.refreshToken = ((JsonString) json.get("refresh_token")).getString();
            tkn.tokenType = ((JsonString) json.get("token_type")).getString();
            tkn.scope = ((JsonString) json.get("scope")).getString();

            return tkn;
        }
    }

    public Accesstoken spotifyAccesstoken() {
        String tokenString = (String) getValue(ATTR_SPOTIFY_ACCESSTOKEN);
        if(tokenString == null) return null;
        return Accesstoken.fromString(tokenString);
    }

    public static void main(String[] args) {
        new User().syncTable();
        User s = User.findById(User.class, 2);
        assert s != null;
        //"{bcrypt}$2a$10$unwVIRGHe8UPuPFS0CwklOwFEQBn2vkJMC/rzeMcuwPizYagdwUne"
        s.update(new Utils.Json()
                .add(ATTR_PASSWORD, "password"));
        s.save();
        System.out.println(s.values);
//        System.out.println(User.all(User.class));
    }
}

