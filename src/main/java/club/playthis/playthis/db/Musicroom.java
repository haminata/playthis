package club.playthis.playthis.db;

import club.playthis.playthis.Utils;
import com.mysql.cj.xdevapi.DbDoc;

import java.util.ArrayList;
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

    public ArrayList<Track> getTracks() {
        ArrayList<Track> tracks = new ArrayList<>();

        if(getId() == null) return tracks;

        ArrayList<RoomTrack> roomTracks = RoomTrack.findAll(RoomTrack.class, new Where() {{
            put(RoomTrack.ATTR_ROOM_ID, getId().toString());
        }});

        for (RoomTrack rt : roomTracks) {
            Track t = rt.getTrack();
            if(t != null) tracks.add(t);
        }
        return tracks;
    }

    public RoomTrack addTrackById(Integer trackId) {
        RoomTrack rt = RoomTrack.findOne(RoomTrack.class, new Where(){{
            put(RoomTrack.ATTR_ROOM_ID, getId().toString());
            put(RoomTrack.ATTR_TRACK_ID, trackId.toString());
        }});
        if(rt != null) return rt;

        RoomTrack newRt = new RoomTrack();
        newRt.update(new Utils.Json()
                .add(RoomTrack.ATTR_TRACK_ID, trackId)
                .add(RoomTrack.ATTR_ROOM_ID, getId()));
        newRt.save();
        return newRt;
    }
}
