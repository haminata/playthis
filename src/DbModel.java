import com.mysql.cj.jdbc.exceptions.MysqlDataTruncation;
import com.mysql.cj.xdevapi.DbDoc;

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

    public String getModelName() {
        return getClass().getSimpleName();
    }

    public String toJson() {
        HashMap<String, AttributeType> attrs = getResolvedAttributes();
        attrs = attrs == null ? new HashMap<>() : attrs;

        StringBuilder json = new StringBuilder("{");

        int position = 0;
        for (Map.Entry<String, AttributeType> entry : attrs.entrySet()) {
            position++;

            json.append("\"");
            json.append(entry.getKey());
            json.append("\"");

            json.append(": ");

            Object value = this.getValue(entry.getKey());
            AttributeType attrType = entry.getValue();
            boolean isString = attrType.dataType.equals(AttributeType.DATA_TYPE_STRING);

            if (value != null && isString) json.append("\"");

            json.append(value);

            if (value != null && isString) json.append("\"");
            if (position != attrs.size()) json.append(", \n");
        }

        json.append("}");
        return json.toString();
    }

    public HashMap<String, String> getValidation() {
        return new HashMap<>();
    }

    /**
     * Forces every class to provide the plural form of the model name
     *
     * @return
     */
    public abstract String getModelNamePlural();

    /**
     * This method collects the model name provided in the class and converts it to lower case
     * The table name of the model is always the plural form of the model name
     *
     * @return
     */
    public String getTableName() {
        return this.getModelNamePlural().toLowerCase();
    }

    /**
     * String = to the key then you provide the attribute type
     *
     * @return
     */
    public abstract HashMap<String, AttributeType> getAttributes();

    public HashMap<String, AttributeType> getDbAttributes() {
        HashMap<String, AttributeType> dbColTypes = new HashMap<>();
        try (Statement stmt = getConnection().createStatement()) {

            String table = getTableName();

            String query = "SELECT * FROM INFORMATION_SCHEMA.COLUMNS as cols, INFORMATION_SCHEMA.TABLES as tbls " +
                    "where tbls.table_schema=\"" + DEFAULT_DATABASE + "\" AND tbls.table_schema=cols.table_schema AND " +
                    "tbls.table_name=\"" + table + "\" AND tbls.table_name=cols.table_name;";

            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                String col = rs.getString("COLUMN_NAME");
                String dataType = rs.getString("DATA_TYPE");
                Integer dataLength = rs.getInt("CHARACTER_MAXIMUM_LENGTH");
                dbColTypes.put(col, new AttributeType(dataType, dataLength));
            }


        } catch (Exception e) {
            System.err.println("[" + getClass().getSimpleName() + "] error: " + e);
            e.printStackTrace();
        }
        return dbColTypes;
    }

    public void setData(ResultSet resultSet) throws SQLException {
        this.createdAt = resultSet.getTimestamp(ATTR_CREATED_AT);
        this.updatedAt = resultSet.getTimestamp(ATTR_UPDATED_AT);
        this.deletedAt = resultSet.getTimestamp(ATTR_DELETED_AT);
        this.activatedAt = resultSet.getTimestamp(ATTR_ACTIVATED_AT);
        this.id = resultSet.getInt(ATTR_ID);
    }

    public void setData(DbDoc json) {
        System.out.println("[" + getClass().getSimpleName() + "#setData] json: " + json);

    }

    public static <T extends DbModel> T create(Class<T> modelClass, DbDoc json, Boolean save) {
        T newInst;
        try {
            newInst = modelClass.newInstance();
            newInst.setData(json);

            if (save) newInst.save();

        } catch (Exception e) {
            e.printStackTrace();
            newInst = null;
        }

        return newInst;
    }

    public boolean save() {
        StringBuilder qms = new StringBuilder();
        StringBuilder cols = new StringBuilder();
        StringBuilder updates = new StringBuilder();

        HashMap<String, AttributeType> attrs = getResolvedAttributes();
        HashMap<Integer, String> colIdxMap = new HashMap<>();

        int colIdx = 1;
        for (String key : attrs.keySet()) {
            if (key.equals(ATTR_ID)) continue;

            qms.append('?');
            cols.append(key);

            updates.append(key);
            updates.append('=');
            updates.append('?');

            colIdxMap.put(colIdx, key);
            if (colIdx != (attrs.size() - 1)) {
                cols.append(',');
                qms.append(',');
                updates.append(',');
            }

            colIdx++;
        }

        StringBuilder sql = new StringBuilder();

        if (isNew()) {
            sql.append("INSERT INTO " + getTableName() + " (" +
                    cols + ") VALUES (" +
                    qms + ");");
        } else {
            sql.append("UPDATE ");
            sql.append(getTableName());
            sql.append(" SET ");

            sql.append(updates.toString());

            sql.append(" WHERE id=");
            sql.append(getId());
            sql.append(";");
        }

        if (this.createdAt == null) createdAt = new Date();
        Date createdAt = this.createdAt;
        PreparedStatement statement = null;

        try {
            statement = getConnection().prepareStatement(sql.toString());
            Date updatedAt = new Date();
            System.out.println("query: " + sql);

            for (Map.Entry<Integer, String> colEntry : colIdxMap.entrySet()) {
                AttributeType type = attrs.get(colEntry.getValue());
                Object value = type.isAudit ? getAuditValue(colEntry.getValue()) : getValue(colEntry.getValue());

                if (colEntry.getValue().equals(ATTR_UPDATED_AT)) value = updatedAt;

                if (value == null) {
                    statement.setNull(colEntry.getKey(), Types.NULL);
                    continue;
                }

                switch (type.dataType) {
                    case AttributeType.DATA_TYPE_STRING:
                        statement.setString(colEntry.getKey(), (String) value);
                        break;
                    case AttributeType.DATA_TYPE_DATE:
                        Timestamp ts = new java.sql.Timestamp(((Date) value).getTime());
                        statement.setTimestamp(colEntry.getKey(), ts);
                        break;
                    case AttributeType.DATA_TYPE_INTEGER:
                        statement.setInt(colEntry.getKey(), (Integer) value);
                        break;
                }
            }

            int updateCount = statement.executeUpdate();
            System.out.println("statement: " + statement.toString() + " updates: " + updateCount);


        } catch (MysqlDataTruncation truncErr) {
            String msg = truncErr.getMessage();
            String detect = "Data too long for column";
            int idx = msg.lastIndexOf(detect);

            String sub = idx >= 0 ? msg.substring(idx + detect.length(), msg.length()) : msg;
            if (statement != null && !sub.equals(msg)) {

                String col = sub.trim().replaceFirst("'", "").split("'", 2)[0];
                try {
                    AttributeType dbAttr = getDbAttributes().get(col);
                    Integer currentSrc = attrs.get(col).dataLength;
                    Integer currentDb = dbAttr.dataLength;
                    int newDataLen = (getValue(col) + "").length()  * 2;
                    this.changeColumn(col, new AttributeType(dbAttr.dataType, newDataLen));
                    System.out.println("[" + getClass().getSimpleName() + "#save] error col \"" + col
                            + "\": currentInScript=" + currentSrc + ", currentInDb=" + currentDb
                            + ", newInDb=" + newDataLen);
                    int updateCount = statement.executeUpdate();
                    System.out.println("statement: " + statement.toString() + " updates: " + updateCount);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }


        return false;
    }

    public boolean isValid() {
        HashMap<String, String> val = getValidation();
        return val == null || val.isEmpty();
    }

    public boolean changeColumn(String col, AttributeType attr){
        String sql = "ALTER TABLE " + getTableName() + " MODIFY " + col + " " + attr.toSql() + ";";
        try(Statement stm = getConnection().createStatement()){
            ///stm.setObject();
            System.out.println("[changeColumn] " + sql);
            stm.executeUpdate(sql);
            return true;
        }catch (SQLException ex){
            ex.printStackTrace();
        }
        return false;
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

    public abstract Object getValue(String attributeName);

    public Object getAuditValue(String attributeName) {
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

    /**
     * This methods includes all attributes that are common to all tables
     *
     * @return
     */
    public HashMap<String, AttributeType> getResolvedAttributes() {
        HashMap<String, AttributeType> attrs = getAttributes();
        attrs = attrs == null ? new HashMap<>() : attrs;

        AttributeType date = new AttributeType(AttributeType.DATA_TYPE_DATE, null, true);
        attrs.put(ATTR_ID, new AttributeType(AttributeType.DATA_TYPE_INTEGER, null, true));
        attrs.put(ATTR_CREATED_AT, date);
        attrs.put(ATTR_UPDATED_AT, date);
        attrs.put(ATTR_ACTIVATED_AT, date);
        attrs.put(ATTR_DELETED_AT, date);

        return attrs;
    }


    /**
     * @return
     */
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

        HashMap<String, String> dbColTypes = new HashMap<>();

        for (Map.Entry<String, AttributeType> entry : getDbAttributes().entrySet()) {
            String col = entry.getKey();
            String dataType = entry.getValue().toSqlDataType();
            dbColTypes.put(col, dataType);

            if (!absentDb.contains(col)) {
                absentServer.add(col);
                continue;
            }

            absentDb.remove(col);
            boolean typesMatch = attrs.get(col).toSqlDataType().equals(dataType);

            if (typesMatch) typeMismatch.remove(col);
        }

        if (dbColTypes.isEmpty()) {
            createTable();
        }

        if (!absentDb.isEmpty()) {
            System.out.println("[" + getClass().getSimpleName() + "] creating missing columns: " + absentDb);
            ArrayList<String> toRemove = new ArrayList<>();

            for (String absent : absentDb) {
                if (absent.equals(ATTR_ID)) {
                    toRemove.add(absent);
                    continue;
                }

                boolean created = createColumn(absent, attrs.get(absent));
                if (created) toRemove.add(absent);
            }

            absentDb.removeAll(toRemove);
            typeMismatch.removeAll(toRemove);
        }

        System.out.println("[" + this.getClass().getSimpleName() + "#syncTable] validation: " + validation);

        return absentDb.isEmpty() && typeMismatch.isEmpty() && absentServer.isEmpty();
    }

    public boolean createTable() {
        String query = "CREATE TABLE " + getTableName() + " (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY (id));";
        System.out.println("[createTable] query: " + query);

        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(query);
            return true;
        } catch (Exception e) {
            System.err.println("[createTable] error: " + e);
        }
        return false;
    }

    public boolean createColumn(String colName, AttributeType attrType) {
        String query = "ALTER TABLE " + getTableName() + " ADD COLUMN " + colName + " " + attrType.toSql() + ";";
        System.out.println("[createColumn] query: " + query);

        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(query);
            return true;
        } catch (Exception e) {
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

    /**
     * As sql and java define they type different, this class handles the conversion between the two
     */
    static class AttributeType {
        public static final String DATA_TYPE_STRING = "string";
        public static final String DATA_TYPE_SQL_STRING = "varchar";
        public static final String DATA_TYPE_DATE = "datetime";
        public static final String DATA_TYPE_SQL_DATE = "datetime";
        public static final String DATA_TYPE_INTEGER = "integer";
        public static final String DATA_TYPE_SQL_INTEGER = "int";

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
        public Boolean isAudit = false;

        public AttributeType(String attrType, Integer dataLength) {
            this.dataLength = dataLength;
            this.dataType = attrType;
        }

        public AttributeType(String attrType, Integer dataLength, boolean isAudit) {
            this.dataLength = dataLength;
            this.dataType = attrType;
            this.isAudit = isAudit;
        }

        public AttributeType(String attrType) {
            this.dataLength = null;
            this.dataType = attrType;
        }

        public Class cls() {
            switch (this.dataType) {
                case DATA_TYPE_DATE:
                    return DATA_TYPE_DATE_CLASS;
                case DATA_TYPE_INTEGER:
                    return DATA_TYPE_INTEGER_CLASS;
                case DATA_TYPE_STRING:
                    return DATA_TYPE_STRING_CLASS;
            }
            return null;
        }

        public static <T> T convert(String value, Class<T> dataTypeClass) {
            if (dataTypeClass.equals(DATA_TYPE_DATE_CLASS)) {
                return null;
            } else if (dataTypeClass.equals(DATA_TYPE_INTEGER_CLASS)) {
                return dataTypeClass.cast(Integer.parseInt(value));
            } else if (dataTypeClass.equals(DATA_TYPE_STRING_CLASS)) {
                return dataTypeClass.cast(value);
            }
            return null;
        }

        public String toSql() {
            switch (this.dataType) {
                case DATA_TYPE_STRING:
                case DATA_TYPE_SQL_STRING:
                    return dataLength == null ? "VARCHAR(15)" : "VARCHAR(" + dataLength + ")";
                case DATA_TYPE_INTEGER:
                case DATA_TYPE_SQL_INTEGER:
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
                case DATA_TYPE_SQL_STRING:
                    return DATA_TYPE_SQL_STRING;
                case DATA_TYPE_INTEGER:
                case DATA_TYPE_SQL_INTEGER:
                    return DATA_TYPE_SQL_INTEGER;
                case DATA_TYPE_DATE:
                    return DATA_TYPE_SQL_DATE;
                default:
                    return dataType.toLowerCase();
            }
        }
    }

    static class Where extends HashMap<String, String> {
        public static final Where EMPTY = new Where();
    }

    public static void main(String[] args) {
        User us = User.findOne(User.class, Where.EMPTY);
        System.out.println(us.toJson());

        Musicroom room = new Musicroom();
        room.syncTable();

        //Song song = (new Song()).findOne(Song.class, Where.EMPTY);
        //System.out.println("Song is: " + song);
    }
}

