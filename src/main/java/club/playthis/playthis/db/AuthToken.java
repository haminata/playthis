package club.playthis.playthis.db;

import com.mysql.cj.xdevapi.DbDoc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class AuthToken extends DbModel {

    public String token, refreshToken, scope, tokenType;
    public Integer expiresIn;

    public static AuthToken fromString(String tokenStr){
        AuthToken tkn = new AuthToken();
        tkn.update(tokenStr);
        return tkn;
    }

    public static final String ATTR_EXPIRES_IN = "expires_in";
    public static final String ATTR_ACCESS_TOKEN = "access_token";
    public static final String ATTR_REFRESH_TOKEN = "refresh_token";
    public static final String ATTR_TOKEN_TYPE = "token_type";
    public static final String ATTR_CREATED_BY = "created_by";
    public static final String ATTR_ROOM_ID = "user_id";
    public static final String ATTR_SCOPE = "scope";

    @Override
    public void updateFromJson(DbDoc json) {
        super.updateFromJson(json);
        updateFieldAttrs();
    }

    @Override
    public void updateFromResultSet(ResultSet resultSet) throws SQLException {
        super.updateFromResultSet(resultSet);
        updateFieldAttrs();
    }

    private void updateFieldAttrs(){
        expiresIn = (Integer) getValue(ATTR_EXPIRES_IN);
        token = (String) getValue(ATTR_ACCESS_TOKEN);
        refreshToken = (String) getValue(ATTR_REFRESH_TOKEN);
        tokenType = (String) getValue(ATTR_TOKEN_TYPE);
        scope = (String) getValue(ATTR_SCOPE);
    }

    public User getCreatedBy(){
        return User.findById(User.class, (Integer) getValue(ATTR_CREATED_BY));
    }

    public Musicroom getMusicroom(){
        return Musicroom.findById(Musicroom.class, (Integer) getValue(ATTR_ROOM_ID));
    }

    @Override
    public DbDoc toJson() {
        DbDoc doc = super.toJson();
        User u = getCreatedBy();
        Musicroom r = getMusicroom();

        if(u != null) doc.put(ATTR_CREATED_BY, u.toJson());
        if(r != null) doc.put(ATTR_ROOM_ID, r.toJson());

        return  doc;
    }

    @Override
    public String getModelNamePlural() {
        return "authtokens";
    }

    @Override
    public HashMap<String, AttributeType> getAttributes() {
        return new HashMap<String, AttributeType>(){{
            put(ATTR_CREATED_BY, AttributeType.INTEGER);
            put(ATTR_ROOM_ID, AttributeType.INTEGER);
            put(ATTR_EXPIRES_IN, AttributeType.INTEGER);
            put(ATTR_REFRESH_TOKEN, AttributeType.TEXT);
            put(ATTR_SCOPE, AttributeType.TEXT);
            put(ATTR_TOKEN_TYPE, AttributeType.STRING);
            put(ATTR_ACCESS_TOKEN, AttributeType.TEXT);
        }};
    }

    public static void main(String[] args) {
        new AuthToken().syncTable();
    }

}
