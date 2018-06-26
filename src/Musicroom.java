import java.util.HashMap;

public class Musicroom extends DbModel {


    @Override
    public String getModelNamePlural() {
        return "musicrooms";
    }

    @Override
    public HashMap<String, AttributeType> getAttributes() {
        return new HashMap<String, AttributeType>(){{
            put("created_by", AttributeType.INTEGER);
            put("name", AttributeType.STRING);
        }};
    }

    public static void main(String[] args) {
        Musicroom m = new Musicroom();
        System.out.println("Table sync result is" + m.syncTable());
    }

}
