package club.playthis.playthis;

import club.playthis.playthis.DbModel;
import com.mysql.cj.xdevapi.DbDoc;
import com.mysql.cj.xdevapi.JsonNumber;
import com.mysql.cj.xdevapi.JsonParser;
import com.mysql.cj.xdevapi.JsonString;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by haminata on 23/06/2018.
 */

class User extends DbModel {

    public static final String ATTR_SPOTIFY_ACCESSTOKEN = "spotify_accesstoken";

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
            put("password_hash", AttributeType.STRING);
            put(ATTR_SPOTIFY_ACCESSTOKEN, AttributeType.TEXT);
        }};
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
    }
}

