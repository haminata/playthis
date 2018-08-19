package club.playthis.playthis.client;


import club.playthis.playthis.db.DbModel;
import club.playthis.playthis.db.Musicroom;
import club.playthis.playthis.db.Track;
import com.mysql.cj.xdevapi.*;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class WebsocketController extends TextWebSocketHandler {

    public static final HashMap<String, WebSocketSession> CONNECTIONS = new HashMap<>();

    public static void broadcast(String subject, DbModel dbModel) {
        if(CONNECTIONS.isEmpty()) return;

        for (Map.Entry<String, WebSocketSession> e : CONNECTIONS.entrySet()) {
            try {
                DbDoc json = dbModel.toJson();
                WsMessage msg = new WsMessage(URI.create("/subscribe?subject=" + subject), json);
                e.getValue().sendMessage(msg.toTextMsg());
            } catch (Exception error) {
                error.printStackTrace();
            }
        }
    }

    static class WsMessage {

        public URI topicUrl;
        public DbDoc body;

        WsMessage(URI topic, DbDoc body) {
            this.topicUrl = topic;
            this.body = body;
        }

        public String getTopic() {
            if (topicUrl == null) return null;
            return topicUrl.getPath();
        }

        static WsMessage fromTextMsg(TextMessage txt) {
            return WsMessage.fromTextMsg(txt.getPayload());
        }

        static WsMessage fromJson(DbDoc json) {
            String t = ((JsonString) json.get("topic")).getString();

            //if(t != null && t.startsWith("/")) t += "." + t;

            URI uri = URI.create(t);
            WsMessage msg = new WsMessage(uri, (DbDoc) json.get("data"));

            return msg;
        }

        static WsMessage fromTextMsg(String txt) {
            DbDoc json = JsonParser.parseDoc(txt);
            return WsMessage.fromJson(json);
        }

        @Override
        public String toString() {
            String bodyStr = body != null ? this.body.toFormattedString() : "<EMPTY>";
            return "Message(" + topicUrl + ", " + bodyStr + ")";
        }

        public WsMessage update(DbDoc j){
            if(j == null) return this;

            for (Map.Entry<String, JsonValue> v :
                    j.entrySet()) {
                this.body.add(v.getKey(), v.getValue());
            }
            return this;
        }

        public TextMessage toTextMsg() {
            DbDoc json = new DbDocImpl();
            JsonValue v = topicUrl != null ? new JsonString() {{
                String url = topicUrl.toString();
                setValue(url.startsWith("\\") ? url.substring(1) : url);
            }} : JsonLiteral.NULL;

            json.put("topic", v);

            if (body != null) json.put("data", body);

            System.out.println("[formatted] " + json.toFormattedString());
            return new TextMessage(json.toString());
        }
    }

    public static final String TOPIC_CREATE_ROOM = "/create_room";
    public static final String TOPIC_GET_ROOM_TRACKS = "/get_room_tracks";
    public static final String TOPIC_DELETE_ROOM = "/delete_room";
    public static final String TOPIC_ADD_ROOM_TRACK = "/add_room_track";
    public static final String TOPIC_GET_OR_CREATE_TRACK = "/get_or_create_track";
    public static final String TOPIC_SUBSCRIBE = "/subscribe";

    public Musicroom createRoom(DbDoc json){
        Musicroom obj = new Musicroom();
        obj.update(json);
        obj.getValidation();
        obj.save();
        return obj;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        // A message has been received
        WsMessage obj = WsMessage.fromTextMsg(textMessage);
        System.out.println("Message received: " + session.getId() + ", msg:" + obj.body.toFormattedString());
        Musicroom room;
        Integer roomId;

        switch (obj.getTopic()){
            case TOPIC_DELETE_ROOM:
                roomId = ((JsonNumber) obj.body.get("roomId")).getInteger();
                Musicroom m = Musicroom.findById(Musicroom.class, roomId);
                if(m != null) {
                    m.deletedAt = new Date();
                    m.save();
                    obj.body.put("room", m.toJson());
                }
                break;
            case TOPIC_GET_OR_CREATE_TRACK:
                DbDoc data = ((DbDoc) obj.body.get("track"));
                String artistName = ((JsonString) data.get("artistName")).getString();
                String title = ((JsonString) data.get(Track.ATTR_TITLE)).getString();
                System.out.println("Track: " + artistName + ", title: " + title);

                Track t = Track.findOne(Track.class, new DbModel.Where(){{
                    put(Track.ATTR_ARTIST_NAME, artistName);
                    put(Track.ATTR_TITLE, title);
                }});

                if(t == null){
                    t = new Track();
                    t.update(data);
                    t.save();
                }
                obj.body.add("track", t.toJson());
                break;
            case TOPIC_ADD_ROOM_TRACK:
                roomId = ((JsonNumber) obj.body.get("roomId")).getInteger();
                Integer trackId = ((JsonNumber) obj.body.get("trackId")).getInteger();

                room = Musicroom.findById(Musicroom.class, roomId);
                room.addTrackById(trackId);
                break;
            case TOPIC_GET_ROOM_TRACKS:
                roomId = ((JsonNumber) obj.body.get("roomId")).getInteger();

                room = Musicroom.findById(Musicroom.class, roomId);

                obj.body.add("tracks", DbModel.manyToJson(room.getTracks()));
                break;
            case TOPIC_CREATE_ROOM:
                obj.update(createRoom(obj.body).toJson());
                break;
            case TOPIC_SUBSCRIBE:
                obj.body.put("subscribed", new JsonString(){{
                    setValue("db_update");
                }});
                break;
            default:
                obj.body.put("error", new JsonString(){{
                    setValue("404 - not found");
                }});
        }

        System.out.println("Message received (obj): " + obj + ", topic=" + obj.getTopic());
        session.sendMessage(obj.toTextMsg());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        System.out.println("[connection closed]" + session.getId());
        CONNECTIONS.remove(session.getId());
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        System.out.println("[connection open]" + session.getId());

        session.sendMessage(new TextMessage("{\"topic\": \"/sessionid?id=" + session.getId() + "\"}"));
        CONNECTIONS.put(session.getId(), session);
    }
}



