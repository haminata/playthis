package club.playthis.playthis.db;

import java.util.HashMap;

public class RoomTrack extends DbModel {

    public static final String ATTR_ROOM_ID = "room_id";
    public static final String ATTR_TRACK_ID = "track_id"; // Track id
    public static final String ATTR_ADDED_BY = "added_by";

    @Override
    public String getModelNamePlural() {
        return "roomtracks";
    }

    public User getAddedBy(){
        return User.findById(User.class, (Integer) getValue(ATTR_ADDED_BY));
    }

    public Track getTrack(){
        return Track.findById(Track.class, (Integer) getValue(ATTR_TRACK_ID));
    }

    public Musicroom getMusicroom(){
        return Musicroom.findById(Musicroom.class, (Integer) getValue(ATTR_ADDED_BY));
    }

    @Override
    public HashMap<String, AttributeType> getAttributes() {
        return new HashMap<String, AttributeType>(){{
            put(ATTR_ROOM_ID, AttributeType.INTEGER);
            put(ATTR_TRACK_ID, AttributeType.INTEGER);
            put(ATTR_ADDED_BY, AttributeType.INTEGER);
        }};
    }

    public static void main(String[] args) {
        new RoomTrack().syncTable();
    }

}
