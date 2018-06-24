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

    public User(){
        super();
    }

    @Override
    public String toJson() {
        return "{\"id\": " + getId() + ", \n"+
                "\"email\": \"" + email + "\", \n" +
                "\"name\": \"" + name + "\", \n"+
                "\"gender\": \"" + this.gender + "\"}";
    }

    public void save(){

    }

    @Override
    public String getTableName() {
        return "users";
    }

    @Override
    public HashMap<String, String> getAttributes() {
        return new HashMap<String, String>(){{
            put("email", AttributeType.STRING);
            put("name", AttributeType.STRING);
            put("gender", AttributeType.STRING);
        }};
    }

    @Override
    public void setData(ResultSet resultSet) throws SQLException {
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

}

