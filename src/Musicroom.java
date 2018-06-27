import java.util.HashMap;

public class Musicroom extends DbModel {
    public int createdBy;
    public String name;


    @Override
    public String getModelNamePlural() {
        return "musicrooms";
    }

    @Override
    public HashMap<String, AttributeType> getAttributes() {
        return new HashMap<String, AttributeType>() {{
            put("created_by", AttributeType.INTEGER);
            put("name", AttributeType.STRING);
        }};
    }

    @Override
    public Object getValue(String attributeName) {
        switch (attributeName) {
            case "created_by":
                return this.createdBy;
            case "name":
                return this.name;
            default:
                return null;
        }
    }

    public static void main(String[] args) {
        Musicroom m = new Musicroom();
        System.out.println("Table sync result is" + m.syncTable());
        m.name = "Hawa's Graduation";
        m.save();

    }

}
