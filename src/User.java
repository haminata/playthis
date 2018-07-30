import com.mysql.cj.xdevapi.DbDoc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * Created by haminata on 23/06/2018.
 */

class User extends DbModel {

    public String email;
    public String name;
    public String gender;

    public final static User shared = new User();

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
        }};
    }

    @Override
    public void updateFromResultSet(ResultSet resultSet) throws SQLException {
        this.email = resultSet.getString("email");
        this.name = resultSet.getString("name");
        this.gender = resultSet.getString("gender");
        this.id = resultSet.getInt("id");
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "User(id=" + getId() + ", email=" + email + ", name=" + name + ", gender=" + this.gender + ")";
    }

    @Override
    public Object getValue(String attributeName) {
        switch (attributeName){
            case "email":
                return this.email;
            case "name":
                return this.getName();
            case "gender":
                return this.gender;
            case "password_hash":
                return "[PASSWRD_HASH]";
        }
        return super.getValue(attributeName);
    }

}

