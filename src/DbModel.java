import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by haminata on 23/06/2018.
 */
public abstract class DbModel {

    private static Connection conn = null;
    protected Integer id;

    public Connection getConnection(){
        try {
            if (conn == null || conn.isClosed()) {
                System.out.println("[getConnection] creating new connection...");
                conn = createConnection("pwdev", "Password&");
            }
        }catch(Exception err){
            System.err.println("[getConnection] error creating connection: " + err);
        }

        return conn;
    }

    public static Connection createConnection(String user, String password) {
        try{
            String driver = "com.mysql.cj.jdbc.Driver";
            String url = "jdbc:mysql://localhost:3306/ptdev?useSSL=false";
            Class.forName(driver);

            return DriverManager.getConnection(url, user, password);
        }catch (Exception err){
            System.err.println("[createConnection] error creating connection: " + err);
        }

        return null;
    }

    public abstract String toJson();
    public abstract void save();

    public abstract String getTableName();
    public abstract HashMap<String, String> getAttributes();
    public abstract void setData(ResultSet resultSet) throws SQLException;

    public Integer getId(){
        return this.id;
    }

    public <T extends DbModel> ArrayList<T> findAll(Class<T> entityClass, HashMap<String, String> where){
        return this.find(entityClass, where, null);
    }

    public <T extends DbModel> ArrayList<T> all(Class<T> entityClass){
        return this.find(entityClass, Where.EMPTY, null);
    }

    public <T extends DbModel> T findOne(Class<T> entityClass, HashMap<String, String> where) {
        ArrayList<T> models = this.find(entityClass, where, 1);
        return models.isEmpty() ? null : models.get(0);
    }

    public <T extends DbModel> ArrayList<T> find(Class<T> entityClass, HashMap<String, String> where, Integer limit) {
        where = where == null ? Where.EMPTY : where;
        ArrayList<T> models = new ArrayList<>();

        System.out.println("[" + this.getClass().getSimpleName() + "] find: " + where);
        try{
            Statement stmt = getConnection().createStatement();
            ResultSet rs;

            String tableName = getTableName();

            if(tableName == null) throw new Exception("Table name can not be null: " + getClass().getCanonicalName());

            String query = "SELECT * FROM " + tableName;

            HashMap<String, String> attrs = this.getAttributes();
            attrs = attrs == null ? new HashMap<>() : attrs;

            for (Map.Entry<String, String> e: attrs.entrySet()) {

                if(!where.containsKey(e.getKey())) continue;

                if(!query.contains(" WHERE")) query += " WHERE";

                String value = where.get(e.getKey());
                switch (e.getValue()){
                    case AttributeType.STRING:
                        query += " " + e.getKey() + "=\"" + value + "\"";
                        break;
                    default:
                        throw new Exception("Unsupported attribute: type=\"" + e.getValue() + "\", value=\"" + value);
                }
            }

            if(limit != null && limit > 0) query += " LIMIT " + limit + ";";

            System.out.println("[" + this.getClass().getSimpleName() + ".find] submitting query: " + query);
            rs = stmt.executeQuery(query);

            while (rs.next()) {
                T obj = entityClass.newInstance();
                obj.setData(rs);
                models.add(obj);
            }

        }catch (Exception err){
            System.err.println("[" + this.getClass().getSimpleName() + ".find] error: " + err);
        }
        return models;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(id=" + getId() + ")";
    }

    public static void main(String[] args) {
        Where where = new Where(){{
           put("gender", "m");
        }};

        ArrayList<User> users = (new User()).findAll(User.class, where);

        System.out.println("User: " + manyToJson(users));

        //Song song = (new Song()).findOne(Song.class, Where.EMPTY);
        //System.out.println("Song is: " + song);
    }

    public static <T extends DbModel> String manyToJson(ArrayList<T> models) {
        StringBuilder jsonArray = new StringBuilder("[");

        int addedCount = 0;
        for (T model : models) {
            jsonArray.append(model.toJson());
            addedCount++;

            if(addedCount < models.size()) jsonArray.append(",\n");
        }

        jsonArray.append("]");
        return jsonArray.toString();
    }

    static class AttributeType{
        public static final String STRING = "string";
        public static final String DATE = "date";
        public static final String INTEGER = "integer";
    }

    static class Where extends HashMap<String, String> {
        public static final Where EMPTY = new Where();
    }
}

