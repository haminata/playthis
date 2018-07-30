/**
 * Created by haminata on 29/07/2018.
 */

import com.mysql.cj.xdevapi.DbDoc;
import com.mysql.cj.xdevapi.DbDocImpl;
import com.mysql.cj.xdevapi.JsonString;

import java.util.Date;

/**
 * As sql and java define they type different, this class handles the conversion between the two
 */
class AttributeType {
    public static final String DATA_TYPE_STRING = "string";
    public static final String DATA_TYPE_DATE = "datetime";
    public static final String DATA_TYPE_INTEGER = "integer";

    public static final Class DATA_TYPE_STRING_CLASS = String.class;
    public static final Class DATA_TYPE_DATE_CLASS = Date.class;
    public static final Class DATA_TYPE_INTEGER_CLASS = Integer.class;

    public static final AttributeType TEXT = new AttributeType(DATA_TYPE_STRING, 250);
    public static final AttributeType STRING = new AttributeType(DATA_TYPE_STRING, 45);
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

    public boolean isString(){
        return dataType.equals(DATA_TYPE_STRING);
    }

    public boolean isDatetime(){
        return dataType.equals(DATA_TYPE_DATE);
    }

    public boolean isNumber(){
        return dataType.equals(DATA_TYPE_INTEGER);
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

    public DbDoc toJsonSchema(){
        DbDoc json = new DbDocImpl();

        switch (this.dataType) {
            case DATA_TYPE_STRING:
                json.add("type", new JsonString(){{
                    setValue("string");
                }});
                break;
            case DATA_TYPE_INTEGER:
                json.add("type", new JsonString(){{
                    setValue("integer");
                }});
                break;
            case DATA_TYPE_DATE:
                json.add("type", new JsonString(){{
                    setValue("string");
                }});

                json.add("format", new JsonString(){{
                    setValue("date-time");
                }});
                break;
        }
        return json;
    }
}