import java.sql.*;
import java.util.*;

/**
 * Created by haminata on 23/06/2018.
 */
public abstract class DbModel {

    private static final String DEFAULT_DATABASE = "ptdev";
    private static Connection conn = null;
    protected Integer id;
    private static final HashMap<String, Class<? extends DbModel>> CLASSES = new HashMap<>();

    public DbModel() {
        Class cls = getClass();

        if (!CLASSES.containsKey(cls.getSimpleName())) {
            CLASSES.put(cls.getSimpleName(), cls);
            System.out.println("[" + cls.getSimpleName() + "] registered handle");
        }
    }

    public static final String JDBC_DRIVER_CLASSPATH = "com.mysql.cj.jdbc.Driver";

    public Connection getConnection() {
        try {
            if (conn == null || conn.isClosed()) {
                System.out.println("[getConnection] creating new connection...");
                conn = createConnection("ptdev", "ptdev", "Password@");
            }
        } catch (Exception err) {
            System.err.println("[getConnection] error creating connection: " + err);
        }

        return conn;
    }

    public static Connection createConnection(String database, String user, String password) {
        try {
            String url = "jdbc:mysql://localhost:3306/" + database + "?useSSL=false";
            Class.forName(JDBC_DRIVER_CLASSPATH);

            return DriverManager.getConnection(url, user, password);
        } catch (Exception err) {
            System.err.println("[createConnection] error creating connection: " + err);
        }

        return null;
    }

    public abstract String toJson();

    public abstract void save();

    public abstract String getTableName();

    public abstract HashMap<String, String> getAttributes();

    public abstract void setData(ResultSet resultSet) throws SQLException;

    public Integer getId() {
        return this.id;
    }

    public static <T extends DbModel> ArrayList<T> findAll(Class<T> entityClass, HashMap<String, String> where) {
        return find(entityClass, where, null);
    }

    public static <T extends DbModel> ArrayList<T> all(Class<T> entityClass) {
        return find(entityClass, Where.EMPTY, null);
    }

    public static <T extends DbModel> T findOne(Class<T> entityClass, HashMap<String, String> where) {
        ArrayList<T> models = find(entityClass, where, 1);
        return models.isEmpty() ? null : models.get(0);
    }

    public static <T extends DbModel> ArrayList<T> find(Class<T> entityClass, HashMap<String, String> where, Integer limit) {
        where = where == null ? Where.EMPTY : where;
        ArrayList<T> models = new ArrayList<>();
        String clsName = entityClass.getSimpleName();
        System.out.println("[" + clsName + "] find: " + where);
        try {
            T fakeThis = entityClass.newInstance();
            Statement stmt = fakeThis.getConnection().createStatement();
            ResultSet rs;

            String tableName = fakeThis.getTableName();

            if (tableName == null)
                throw new Exception("Table name can not be null: " + fakeThis.getClass().getCanonicalName());

            String query = "SELECT * FROM " + tableName;

            HashMap<String, String> attrs = fakeThis.getAttributes();
            attrs = attrs == null ? new HashMap<>() : attrs;

            for (Map.Entry<String, String> e : attrs.entrySet()) {

                if (!where.containsKey(e.getKey())) continue;

                if (!query.contains(" WHERE")) query += " WHERE";

                String value = where.get(e.getKey());
                switch (e.getValue()) {
                    case AttributeType.STRING:
                        query += " " + e.getKey() + "=\"" + value + "\"";
                        break;
                    default:
                        throw new Exception("Unsupported attribute: type=\"" + e.getValue() + "\", value=\"" + value);
                }
            }

            if (limit != null && limit > 0) query += " LIMIT " + limit + ";";

            System.out.println("[" + clsName + ".find] submitting query: " + query);
            rs = stmt.executeQuery(query);

            while (rs.next()) {
                T obj = entityClass.newInstance();
                obj.setData(rs);
                models.add(obj);
            }

        } catch (Exception err) {
            System.err.println("[" + clsName + ".find] error: " + err);
        }
        return models;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(id=" + getId() + ")";
    }

    public static void main(String[] args) {
        Where where = new Where() {{
            put("gender", "m");
        }};

        ArrayList<User> users = User.findAll(User.class, where);
        //User.findOne(User.class)

        System.out.println("Check \"" + User.class.getSimpleName() + "\" in sync: " + User.shared.syncTable());
        System.out.println("User: " + manyToJson(users));

        //Song song = (new Song()).findOne(Song.class, Where.EMPTY);
        //System.out.println("Song is: " + song);
    }

    public boolean syncTable() {
        String table = getTableName();
        String schema = DEFAULT_DATABASE;

        String query = "SELECT * FROM INFORMATION_SCHEMA.COLUMNS as cols, INFORMATION_SCHEMA.TABLES as tbls " +
                "where tbls.table_schema=\"" + schema + "\" AND tbls.table_schema=cols.table_schema AND " +
                "tbls.table_name=\"" + table + "\" AND tbls.table_name=cols.table_name;";

        HashMap<String, String> attrs = getAttributes();
        attrs = attrs == null ? new HashMap<>() : attrs;
        attrs.put("id", AttributeType.INTEGER);

        Set<String> absentDb = ((HashMap<String, String>) attrs.clone()).keySet();
        Set<String> typeMismatch = ((HashMap<String, String>) attrs.clone()).keySet();
        Set<String> absentServer = new HashSet<>();

        HashMap<String, Set<String>> validation = new HashMap<String, Set<String>>() {{
            put("absentServer", absentServer);
            put("absentDb", absentDb);
            put("typeMismatch", typeMismatch);
        }};

        try (Statement stmt = getConnection().createStatement()){
            ResultSet rs = stmt.executeQuery(query);

            HashMap<String, String> dbColTypes = new HashMap<>();

            while (rs.next()) {
                String col = rs.getString("COLUMN_NAME");
                String dataType = rs.getString("DATA_TYPE");
                dbColTypes.put(col, dataType);

                if (!absentDb.contains(col)) {
                    absentServer.add(col);
                    continue;
                }

                absentDb.remove(col);
                boolean typesMatch = false;

                switch (attrs.get(col)) {
                    case AttributeType.STRING:
                        typesMatch = dataType.equals("varchar");
                        break;
                    case AttributeType.INTEGER:
                        typesMatch = dataType.equals("int");
                }

                if (typesMatch) typeMismatch.remove(col);
            }

            if (!dbColTypes.isEmpty() && !absentDb.isEmpty()) {
                System.out.println("[" + getClass().getSimpleName() + "] creating missing columns: " + absentDb);
                ArrayList<String> toRemove = new ArrayList<>();

                for (String absent : absentDb) {
                    boolean created = createColumn(absent, attrs.get(absent));
                    if(created) toRemove.add(absent);
                }

                absentDb.removeAll(toRemove);
                typeMismatch.removeAll(toRemove);
            }

            System.out.println("[" + this.getClass().getSimpleName() + "#syncTable] validation: " + validation);

            return absentDb.isEmpty() && typeMismatch.isEmpty() && absentServer.isEmpty();
        } catch (Exception e) {
            System.err.println("[" + getClass().getSimpleName() + "] error syncing table: " + e);
            e.printStackTrace();
        }

        return false;
    }

    public boolean createColumn(String colName, String dataType) {
        String query = "ALTER TABLE " + getTableName() + " ADD COLUMN " + colName + " " + AttributeType.toSql(dataType) + ";";
        System.out.println("[createColumn] query: " + query);

        try {
            Statement stmt = getConnection().createStatement();
            stmt.execute(query);
            stmt.close();

            return true;
        }catch (Exception e){
            System.err.println("[createColumn] error: " + e);
        }
        return false;
    }

    public static <T extends DbModel> String manyToJson(ArrayList<T> models) {
        StringBuilder jsonArray = new StringBuilder("[");

        int addedCount = 0;
        for (T model : models) {
            jsonArray.append(model.toJson());
            addedCount++;

            if (addedCount < models.size()) jsonArray.append(",\n");
        }

        jsonArray.append("]");
        return jsonArray.toString();
    }

    static class AttributeType {
        public static final String STRING = "string";
        public static final String DATE = "datetime";
        public static final String INTEGER = "integer";

        public static String toSql(String dataType) {
            switch (dataType) {
                case STRING:
                    return "VARCHAR(15)";
                case INTEGER:
                    return "INT";
                case DATE:
                    return "DATETIME";
                default:
                    return dataType;
            }
        }
    }

    static class Where extends HashMap<String, String> {
        public static final Where EMPTY = new Where();
    }
}

