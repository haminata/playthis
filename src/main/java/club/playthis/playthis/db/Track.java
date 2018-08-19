package club.playthis.playthis.db;

import club.playthis.playthis.Utils;

import java.util.HashMap;

/**
 * Created by haminata on 26/07/2018.
 */
public class Track extends DbModel {

    @Override
    public String getModelNamePlural() {
        return "tracks";
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
        Track s = new Track();
        s.syncTable();
        s.update(new Utils.Json("title", "Shayo")
                .add("artist_name", "Bigiano")
                .add("thumbnail_url", "https://i1.wp.com/thenetng.net/wp-content/uploads/2012/11/Bigiano.jpg"));
        s.save();
    }
}
