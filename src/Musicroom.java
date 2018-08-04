import com.mysql.cj.xdevapi.DbDoc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class Musicroom extends DbModel {
    public Integer createdBy;
    public String name;
    public String status;


    @Override
    public String getModelNamePlural() {
        return "musicrooms";
    }

    @Override
    public HashMap<String, AttributeType> getAttributes() {
        return new HashMap<String, AttributeType>() {{
            put("created_by", AttributeType.INTEGER);
            put("name", AttributeType.STRING);
            put("status", AttributeType.STRING);
        }};
    }

    public static void main(String[] args) {
        Musicroom m = new Musicroom();
        System.out.println("Table sync result is" + m.syncTable());
        m.name = "Hawa's Graduation";
        m.save();

    }

}
