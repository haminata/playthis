import com.mysql.cj.xdevapi.DbDoc;
import com.mysql.cj.xdevapi.JsonParser;

import java.sql.*;
import java.util.*;
import java.util.Date;

/**
 * Created by haminata on 23/06/2018.
 */
public abstract class DbModel {

    public static final String ATTR_ID = "id";
    public static final String ATTR_CREATED_AT = "created_at";
    public static final String ATTR_UPDATED_AT = "updated_at";
    public static final String ATTR_DELETED_AT = "deleted_at";
    public static final String ATTR_ACTIVATED_AT = "activated_at";

    public static final String SYNC_ERROR_NOT_FOUND_DB = "MySQL databse is missing column(s): ";
    public static final String SYNC_ERROR_NOT_FOUND_JAVA = "Java source code is missing attribute(s): ";
    public static final String SYNC_ERROR_MISMATCH_DATA_TYPE = "Column data types are out of sync: ";

    private static final String DEFAULT_DATABASE = "ptdev";
    private static Connection conn = null;
    protected Integer id;
    private static final HashMap<String, Class<? extends DbModel>> CLASSES = new HashMap<>();
    public Date createdAt, updatedAt, deletedAt, activatedAt;

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

    public String toJson() {
        HashMap<String, AttributeType> attrs = getResolvedAttributes();
        attrs = attrs == null ? new HashMap<>() : attrs;

        StringBuilder json = new StringBuilder("{");

        int position = 0;
        for (Map.Entry<String, AttributeType> entry: attrs.entrySet()){
            position++;

            json.append("\"");
            json.append(entry.getKey());
            json.append("\"");

            json.append(": ");

            Object value = this.getValue(entry.getKey());
            AttributeType attrType = entry.getValue();
            boolean isString = attrType.dataType.equals(AttributeType.DATA_TYPE_STRING);

            if(value != null && isString) json.append("\"");

            json.append(value);

            if(value != null && isString) json.append("\"");
            if(position != attrs.size()) json.append(", \n");
        }

        json.append("}");
        return json.toString();
    }

    public HashMap<String, String> getValidation(){
        return new HashMap<>();
    }

    public abstract String getTableName();
    public abstract HashMap<String, AttributeType> getAttributes();

    public void setData(ResultSet resultSet) throws SQLException {
        this.createdAt = resultSet.getDate(ATTR_CREATED_AT);
        this.updatedAt = resultSet.getDate(ATTR_UPDATED_AT);
        this.deletedAt = resultSet.getDate(ATTR_DELETED_AT);
        this.activatedAt = resultSet.getDate(ATTR_ACTIVATED_AT);
        this.id = resultSet.getInt(ATTR_ID);
    }

    public void setData(DbDoc json){
        System.out.println("[" + getClass().getSimpleName() + "#setData] json: " + json);

    }

    public static <T extends DbModel> T create(Class<T> modelClass, DbDoc json, Boolean save){
        T newInst;
        try {
            newInst = modelClass.newInstance();
            newInst.setData(json);

            if(save) newInst.save();

        } catch (Exception e) {
            e.printStackTrace();
            newInst = null;
        }

        return newInst;
    }

    public boolean save(){

        return false;
    }

    public boolean isValid(){
        HashMap<String, String> val = getValidation();
        return val == null || val.isEmpty();
    }

    public Integer getId() {
        return this.id;
    }

    public boolean isNew() {
        return this.id == null || this.id < 1;
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
            T inst = entityClass.newInstance();
            Statement stmt = inst.getConnection().createStatement();
            ResultSet rs;

            String tableName = inst.getTableName();

            if (tableName == null)
                throw new Exception("Table name can not be null: " + inst.getClass().getCanonicalName());

            String query = "SELECT * FROM " + tableName;

            HashMap<String, AttributeType> attrs = inst.getResolvedAttributes();

            for (Map.Entry<String, AttributeType> e : attrs.entrySet()) {

                if (!where.containsKey(e.getKey())) continue;

                if (query.endsWith(tableName) && !query.contains(" WHERE")) query += " WHERE";

                String value = where.get(e.getKey());
                switch (e.getValue().dataType) {
                    case AttributeType.DATA_TYPE_STRING:
                        query += " " + e.getKey() + "=\"" + value + "\"";
                        break;
                    default:
                        throw new Exception("Unsupported attribute: type=\"" + e.getValue() + "\", value=\"" + value);
                }
            }

            if (limit != null && limit > 0) query += " LIMIT " + limit;

            query += ";";

            System.out.println("[" + clsName + "#find] submitting query: " + query);
            rs = stmt.executeQuery(query);

            while (rs.next()) {
                T obj = entityClass.newInstance();
                obj.setData(rs);
                models.add(obj);
            }

        } catch (Exception err) {
            System.err.println("[" + clsName + "#find] error: " + err);
        }
        return models;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(id=" + getId() + ")";
    }

    public Object getValue(String attributeName) {
        switch (attributeName) {
            case ATTR_CREATED_AT:
                return this.createdAt;
            case ATTR_UPDATED_AT:
                return this.updatedAt;
            case ATTR_DELETED_AT:
                return this.deletedAt;
            case ATTR_ACTIVATED_AT:
                return this.activatedAt;
            case ATTR_ID:
                return this.getId();
            default:
                return null;
        }
    }

    public HashMap<String, AttributeType> getResolvedAttributes(){
        HashMap<String, AttributeType> attrs = getAttributes();
        attrs = attrs == null ? new HashMap<>() : attrs;
        attrs.put(ATTR_ID, AttributeType.INTEGER);
        attrs.put(ATTR_CREATED_AT, AttributeType.DATE);
        attrs.put(ATTR_UPDATED_AT, AttributeType.DATE);
        attrs.put(ATTR_ACTIVATED_AT, AttributeType.DATE);
        attrs.put(ATTR_DELETED_AT, AttributeType.DATE);

        return attrs;
    }

    public boolean syncTable() {

        HashMap<String, AttributeType> attrs = getResolvedAttributes();

        Set<String> absentDb = ((HashMap<String, String>) attrs.clone()).keySet();
        Set<String> typeMismatch = ((HashMap<String, String>) attrs.clone()).keySet();
        Set<String> absentServer = new HashSet<>();

        HashMap<String, Set<String>> validation = new HashMap<String, Set<String>>() {{
            put(SYNC_ERROR_NOT_FOUND_JAVA, absentServer);
            put(SYNC_ERROR_NOT_FOUND_DB, absentDb);
            put(SYNC_ERROR_MISMATCH_DATA_TYPE, typeMismatch);
        }};

        try (Statement stmt = getConnection().createStatement()){

            String table = getTableName();

            String query = "SELECT * FROM INFORMATION_SCHEMA.COLUMNS as cols, INFORMATION_SCHEMA.TABLES as tbls " +
                    "where tbls.table_schema=\"" + DEFAULT_DATABASE + "\" AND tbls.table_schema=cols.table_schema AND " +
                    "tbls.table_name=\"" + table + "\" AND tbls.table_name=cols.table_name;";

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
                boolean typesMatch = attrs.get(col).toSqlDataType().equals(dataType);

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

    public boolean createColumn(String colName, AttributeType attrType) {
        String query = "ALTER TABLE " + getTableName() + " ADD COLUMN " + colName + " " + attrType.toSql() + ";";
        System.out.println("[createColumn] query: " + query);

        try (Statement stmt = getConnection().createStatement()){
            stmt.execute(query);
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
        public static final String DATA_TYPE_STRING = "string";
        public static final String DATA_TYPE_DATE = "datetime";
        public static final String DATA_TYPE_INTEGER = "integer";

        public static final Class DATA_TYPE_STRING_CLASS = String.class;
        public static final Class DATA_TYPE_DATE_CLASS = Date.class;
        public static final Class DATA_TYPE_INTEGER_CLASS = Integer.class;

        public static final AttributeType TEXT = new AttributeType(DATA_TYPE_STRING, 45);
        public static final AttributeType STRING = new AttributeType(DATA_TYPE_STRING, 15);
        public static final AttributeType CHARACTER = new AttributeType(DATA_TYPE_STRING, 1);
        public static final AttributeType DATE = new AttributeType(DATA_TYPE_DATE);
        public static final AttributeType INTEGER = new AttributeType(DATA_TYPE_INTEGER);

        public Integer dataLength;
        public String dataType;

        public AttributeType(String attrType, Integer dataLength){
            this.dataLength = dataLength;
            this.dataType = attrType;
        }

        public AttributeType(String attrType){
            this.dataLength = null;
            this.dataType = attrType;
        }

        public Class cls(){
            switch(this.dataType){
                case DATA_TYPE_DATE:
                    return DATA_TYPE_DATE_CLASS;
                case DATA_TYPE_INTEGER:
                    return DATA_TYPE_INTEGER_CLASS;
                case DATA_TYPE_STRING:
                    return DATA_TYPE_STRING_CLASS;
            }
            return null;
        }

        public static <T> T convert(String value, Class<T> dataTypeClass){
            if(dataTypeClass.equals(DATA_TYPE_DATE_CLASS)){
                return null;
            }else if(dataTypeClass.equals(DATA_TYPE_INTEGER_CLASS)){
                return dataTypeClass.cast(Integer.parseInt(value));
            }else if(dataTypeClass.equals(DATA_TYPE_STRING_CLASS)){
                return dataTypeClass.cast(value);
            }
            return null;
        }

        public String toSql() {
            switch (this.dataType) {
                case DATA_TYPE_STRING:
                    return dataLength == null ? "VARCHAR(15)" : "VARCHAR(" + dataLength + ")";
                case DATA_TYPE_INTEGER:
                    return "INT";
                case DATA_TYPE_DATE:
                    return "DATETIME";
                default:
                    return dataType.toUpperCase();
            }
        }

        public String toSqlDataType() {
            switch (this.dataType) {
                case DATA_TYPE_STRING:
                    return "varchar";
                case DATA_TYPE_INTEGER:
                    return "int";
                case DATA_TYPE_DATE:
                    return "datetime";
                default:
                    return dataType.toLowerCase();
            }
        }
    }

    static class Where extends HashMap<String, String> {
        public static final Where EMPTY = new Where();
    }

    public static void main(String[] args) {
        Where where = new Where() {{
            put("gender", "m");
        }};

        System.out.println("Check \"" + User.class.getSimpleName() + "\" table in sync: " + User.shared.syncTable());
        ArrayList<User> users = User.all(User.class);
        //User.findOne(User.class)

        System.out.println("User: " + manyToJson(users));
        System.out.println("User (Json): " + JsonParser.parseDoc(users.get(0).toJson()).getClass());
        System.out.println("Convert: " + AttributeType.convert("1", AttributeType.INTEGER.cls()).getClass());

        //Song song = (new Song()).findOne(Song.class, Where.EMPTY);
        //System.out.println("Song is: " + song);
    }
}

