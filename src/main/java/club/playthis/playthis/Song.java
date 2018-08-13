package club.playthis.playthis;

import club.playthis.playthis.DbModel;
import com.mysql.cj.xdevapi.DbDoc;
import com.mysql.cj.xdevapi.DbDocImpl;
import com.mysql.cj.xdevapi.JsonString;

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
        return new HashMap<String, AttributeType>(){{
            put("title", AttributeType.STRING);
            put("artist_name", AttributeType.STRING);
            put("trackId", AttributeType.STRING);
            put("thumbnail_url", new AttributeType(AttributeType.DATA_TYPE_STRING, 400));
        }};
    }

    public static void main(String[] args) {
        Song s = new Song();
        s.syncTable();
        s.update(new DbDocImpl(){{
            put("title", new JsonString(){{
                setValue("2Face");
            }});
            put("artist_name", new JsonString(){{
                setValue("True love");
            }});
            put("thumbnail_url", new JsonString(){{
                setValue("https://i2.wp.com/retrojamz.com/wp-content/uploads/2016/11/2face1-1.jpg?fit=305%2C304&ssl=1");
            }});
        }});
        s.save();
    }
}
