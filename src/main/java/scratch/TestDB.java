package scratch;

import Utils.Database;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestDB {
    public static void main(String[] args) {
        try {
            Connection c = Database.getInstance().getConnection();
            Statement s = c.createStatement();
            ResultSet rs = s.executeQuery("DESCRIBE user_favourite_events");
            while(rs.next()) {
                System.out.println(rs.getString("Field") + " - " + rs.getString("Type"));
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
