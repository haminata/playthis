import java.sql.*;
import java.io.IOException;

public class DatabaseC {

    public static void main(String[] args) throws Exception {
        Connection c = getConnection(args[0], args[1]);
        System.out.println("[Connection] " + c);

        Statement stmt = c.createStatement();
        ResultSet rs;

        rs = stmt.executeQuery("SELECT * FROM User;");
        while ( rs.next() ) {
            String lastName = rs.getString("name");
            System.out.println(lastName);
        }
        c.close();
    }


    public static Connection getConnection(String user, String password) throws Exception {
        String driver = "com.mysql.cj.jdbc.Driver";
        String url = "jdbc:mysql://localhost:3306/playthisTestDataB?useSSL=false";
        Class.forName(driver);

        Connection conn = DriverManager.getConnection(url, user, password);
        System.out.println("Connected!");

        return conn;
    }

}

