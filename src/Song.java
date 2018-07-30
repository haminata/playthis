import com.mysql.cj.xdevapi.DbDoc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * Created by haminata on 26/07/2018.
 */
public class Song extends DbModel {

    @Override
    public String getModelNamePlural() {
        return "Songs";
    }

    @Override
    public HashMap<String, AttributeType> getAttributes() {
        return null;
    }

    @Override
    public void updateFromJson(DbDoc json) {

    }

    @Override
    public void updateFromResultSet(ResultSet resultSet) throws SQLException {

    }

    @Override
    public Object getValue(String attributeName) {
        return null;
    }

}
