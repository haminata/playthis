package club.playthis.playthis.db;

import com.mysql.cj.xdevapi.DbDoc;
import java.util.HashMap;

public class Musicroom extends DbModel {
    public Integer createdBy;
    public String name;
    public String status;

    public static final String ATTR_CREATED_BY = "created_by";

    @Override
    public String getModelNamePlural() {
        return "musicrooms";
    }

    @Override
    public HashMap<String, AttributeType> getAttributes() {
        return new HashMap<String, AttributeType>() {{
            put(ATTR_CREATED_BY, AttributeType.INTEGER);
            put("name", AttributeType.STRING);
            put("status", AttributeType.STRING);
            put("description", AttributeType.TEXT);
        }};
    }

    public User getCreatedBy(){
        Integer createdBy = (Integer) getValue(ATTR_CREATED_BY);
        User user = null;
        if(createdBy != null && createdBy > 0) user = User.findOne(User.class, new Where(){{
            put(ATTR_ID, createdBy.toString());
        }});
        return user;
    }

    @Override
    public DbDoc toJson() {
        DbDoc json = super.toJson();
        User user = getCreatedBy();

        if(user != null) {
            json.put(ATTR_CREATED_BY, user.toJson());
        }
        return json;
    }

    public static void main(String[] args) {
        Musicroom m = new Musicroom();
        System.out.println("Table sync result is" + m.syncTable());
        //m.name = "Hawa's Graduation";
        //m.save();

    }

}
