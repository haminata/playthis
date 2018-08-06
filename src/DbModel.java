import com.mysql.cj.jdbc.exceptions.MysqlDataTruncation;
import com.mysql.cj.xdevapi.*;

import java.sql.*;
import java.sql.Statement;
import java.text.SimpleDateFormat;
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

    public static final ArrayList<String> ATTR_NAMES_AUDIT = new ArrayList<String>(5){{
        add(ATTR_ID);
        add(ATTR_CREATED_AT);
        add(ATTR_UPDATED_AT);
        add(ATTR_ACTIVATED_AT);
        add(ATTR_DELETED_AT);
    }};

    public static final String SYNC_ERROR_NOT_FOUND_DB = "MySQL database is missing column(s): ";
    public static final String SYNC_ERROR_NOT_FOUND_JAVA = "Java source code is missing attribute(s): ";
    public static final String SYNC_ERROR_MISMATCH_DATA_TYPE = "Column data types are out of sync: ";

    private static final String DEFAULT_DATABASE = "ptdev";
    private static Connection conn = null;
    protected Integer id;

    public static final HashMap<String, Class<? extends DbModel>> CLASSES = new HashMap<>();
    public static final HashMap<String, Class<? extends DbModel>> PLURAL_NAME_CLASS = new HashMap<>();

    public Date createdAt, updatedAt, deletedAt, activatedAt;

    public static Boolean isAuditAttr(String attrName){
        return ATTR_NAMES_AUDIT.contains(attrName);
    }

    public static HashMap<String, Class<? extends DbModel>> getModelPluralNames(){
        return PLURAL_NAME_CLASS;
    }

    public static synchronized HashMap<String, Class<? extends DbModel>> classes(){
        return CLASSES;
    }

    public DbModel() {
    }

    public static <T extends DbModel> void register(Class<T> cls){
        boolean exists = CLASSES.containsKey(cls.getSimpleName());

        if(!exists){
            CLASSES.put(cls.getSimpleName(), cls);
            CLASSES.put(cls.getSimpleName().toLowerCase(), cls);

            DbModel inst = instance(cls);
            PLURAL_NAME_CLASS.put(inst.getModelNamePlural(), cls);
            PLURAL_NAME_CLASS.put(inst.getModelNamePlural().toLowerCase(), cls);

            System.out.println("[DbModel#register] added model: " + cls.getCanonicalName());
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

    public static <T extends DbModel> T instance(Class<T> cls){
        try {
            return cls.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getModelName(){
        return getClass().getSimpleName();
    }

    public DbDoc toJson() {
        HashMap<String, AttributeType> attrs = getResolvedAttributes();
        attrs = attrs == null ? new HashMap<>() : attrs;
        DbDoc doc = new DbDocImpl();

        doc.add("type", new JsonString(){{
            setValue(DbModel.this.getClass().getSimpleName());
        }});

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (Map.Entry<String, AttributeType> entry: attrs.entrySet()){
            String attrName = entry.getKey();

            Object value = DbModel.isAuditAttr(attrName) ? this.getAuditValue(attrName) : this.getValue(attrName);

            if(value == null) continue;

            AttributeType attrType = entry.getValue();

            if(attrType.isNumber()){
                doc.add(attrName, new JsonNumber(){{
                    this.setValue(value.toString());
                }});
            }else if(attrType.isString()){
                doc.add(attrName, new JsonString(){{
                    this.setValue(value.toString());
                }});
            } else if (attrType.isDatetime()){
                doc.add(attrName, new JsonString(){{
                    this.setValue(format.format(value));
                }});
            }
        }
        return doc;
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

    public Boolean update(ResultSet resultSet){
        try {
            updateAuditFromResultSet(resultSet);
            updateFromResultSet(resultSet);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Boolean update(String jsonString){
        try {
            DbDoc json = JsonParser.parseDoc(jsonString);
            this.update(json);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Boolean update(DbDoc json){
        updateAuditFromJson(json);
        updateFromJson(json);
        return true;
    }

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

    public void updateAuditFromResultSet(ResultSet resultSet) throws SQLException {
        this.createdAt = resultSet.getTimestamp(ATTR_CREATED_AT);
        this.updatedAt = resultSet.getTimestamp(ATTR_UPDATED_AT);
        this.deletedAt = resultSet.getTimestamp(ATTR_DELETED_AT);
        this.activatedAt = resultSet.getTimestamp(ATTR_ACTIVATED_AT);
        this.id = resultSet.getInt(ATTR_ID);
    }

    public void updateAuditFromJson(DbDoc json){
        this.createdAt = Utils.extractDate(json, ATTR_CREATED_AT);
        this.updatedAt = Utils.extractDate(json, ATTR_UPDATED_AT);
        this.deletedAt = Utils.extractDate(json, ATTR_DELETED_AT);
        this.activatedAt = Utils.extractDate(json, ATTR_ACTIVATED_AT);

        JsonNumber num = (JsonNumber) json.get(ATTR_ID);

        this.id = num != null ? num.getInteger() : null;
    }

    public void updateFromJson(DbDoc json){
        for (Map.Entry<String, AttributeType> e :
                getResolvedAttributes().entrySet()) {
            JsonValue v = json.get(e.getKey());

            if(e.getValue().isNumber()){
                values.put(e.getKey(), v != null ? ((JsonNumber) v).getInteger() : null);
            }else if(e.getValue().isString()){
                values.put(e.getKey(), v != null ? ((JsonString) v).getString() : null);
            }else if(e.getValue().isDatetime()){
                Date d = v != null ? Timestamp.valueOf(((JsonString) v).getString()) : null;
                values.put(e.getKey(), d);
            }

        }
    }

    public void updateFromResultSet(ResultSet resultSet) throws SQLException {
        for (Map.Entry<String, AttributeType> e :
                getResolvedAttributes().entrySet()) {

            if(e.getValue().isNumber()){
                values.put(e.getKey(), resultSet.getInt(e.getKey()));
            }else if(e.getValue().isString()){
                values.put(e.getKey(), resultSet.getString(e.getKey()));
            }else if(e.getValue().isDatetime()){
                values.put(e.getKey(), resultSet.getTimestamp(e.getKey()));
            }

        }
    }

    public static <T extends DbModel> T create(Class<T> modelClass, DbDoc json){
        T inst = build(modelClass, json);
        if(inst != null) inst.save();
        return inst;
    }

    public static <T extends DbModel> T build(String modelClassName){
        T newInst;
        try {
            Class<T> modelClass = (Class<T>) CLASSES.get(modelClassName.trim());
            newInst = modelClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            newInst = null;
        }

        return newInst;
    }

    public static <T extends DbModel> T build(Class<T> modelClass, DbDoc json){
        T newInst;
        try {
            newInst = modelClass.newInstance();
            newInst.updateFromJson(json);

            newInst.save();
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
            sql.append("INSERT INTO ")
                    .append(getTableName())
                    .append(" (")
                    .append(cols)
                    .append(") VALUES (")
                    .append(qms)
                    .append(");");
        } else {
            sql.append("UPDATE ");
            sql.append(getTableName());
            sql.append(" SET ");

            sql.append(updates.toString());

            sql.append(" WHERE id=");
            sql.append(getId());
            sql.append(";");
        }

        Date updatedAt = new Date();

        if(this.createdAt == null) this.createdAt = new Date();
        Date createdAt = this.createdAt;

        PreparedStatement statement = null;
        Integer updateCount = null;
        boolean success = false;

        try {
            statement = getConnection().prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS);

            System.out.println("query: " + sql);

            for (Map.Entry<Integer, String> colEntry: colIdxMap.entrySet()) {
                String attrName = colEntry.getValue();

                AttributeType type = attrs.get(attrName);
                Object value = DbModel.isAuditAttr(attrName) ? getAuditValue(attrName) : getValue(attrName);

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

            updateCount = statement.executeUpdate();
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
                    updateCount = statement.executeUpdate();
                    System.out.println("statement: " + statement.toString() + " updates: " + updateCount);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }  finally {
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;

            if (updateCount == null || updateCount == 0) {
                System.out.println("[" + this.getClass().getSimpleName() + "] save failed, no rows affected.");
            } else {

                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        this.id = generatedKeys.getInt(1);
                        success = true;
                    } else {
                        throw new SQLException("[" + this.getClass().getSimpleName() + "] save failed, no ID obtained.");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }

            }
        }

        return success;
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
                obj.update(rs);
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

    protected HashMap<String, Object> values = new HashMap<>();

    public Object getValue(String attributeName){
        if(ATTR_NAMES_AUDIT.contains(attributeName)) return getAuditValue(attributeName);
        return values.get(attributeName);
    }

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

            if(dbColTypes.isEmpty()){
                createTable();
            }

            if (!absentDb.isEmpty()) {
                System.out.println("[" + getClass().getSimpleName() + "] creating missing columns: " + absentDb);
                ArrayList<String> toRemove = new ArrayList<>();

                for (String absent : absentDb) {
                    if(absent.equals(ATTR_ID)){
                        toRemove.add(absent);
                        continue;
                    }

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

    public static <T extends DbModel> JsonArray manyToJson(ArrayList<T> models) {
        JsonArray jsonArray = new JsonArray();

        for (T model : models) {
            jsonArray.add(model.toJson());
        }

        return jsonArray;
    }

    public static String templateFileName(Class<?> cls, String format){
        String clsName = cls.getSimpleName();

        if(format != null && !format.startsWith(".") && format.trim().length() >= 1){
            format = "." + format;
        }
        format = format == null ? "" : format;

        return clsName.toLowerCase() + format + ".html";
    }

    public static String templateFileName(Class<?> cls){
        return templateFileName(cls, null);
    }

    public String getDefaultTemplate(String format){
        String defaultView = Utils.readFile("views/" + DbModel.templateFileName(DbModel.class, format));

        if(defaultView == null){
            defaultView = Utils.formFromSchema((DbDoc) DbModel.schemas().get(this.getClass().getSimpleName()));
        }

        return defaultView;
    }

    public String getDefaultTemplate(){
        return getDefaultTemplate(null);
    }

    public String getTemplate(){
        return getTemplate(null);
    }

    public String getTemplate(String format){
        String view = Utils.readFile("views/" + DbModel.templateFileName(this.getClass(), format) );
        return view == null ? getDefaultTemplate(format) : view;
    }

    public static DbDoc schemas() {
        DbDoc json = new DbDocImpl();
        for (Map.Entry<String, Class<? extends DbModel>> e: CLASSES.entrySet()){
            DbDoc attrJson = new DbDocImpl();

            try {
                DbModel inst = e.getValue().newInstance();
                attrJson = inst.getJsonSchema();
                attrJson.add("plural", new JsonString(){{
                    setValue(inst.getModelNamePlural());
                }});
            } catch (Exception e1) {
                e1.printStackTrace();
            }

            json.add(e.getValue().getSimpleName(), attrJson);
        }
        return json;
    }

    public DbDoc getJsonSchema(){
        DbDoc json = new DbDocImpl();
        DbDoc props = new DbDocImpl();

        json.add("type", new JsonString(){{
            setValue("object");
        }});

        json.add("properties", props);

        for (Map.Entry<String, AttributeType> e: getResolvedAttributes().entrySet()){
            props.add(e.getKey(), e.getValue().toJsonSchema());
        }
        return json;
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

