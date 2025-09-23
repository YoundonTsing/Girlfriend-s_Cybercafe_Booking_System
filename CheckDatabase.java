import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckDatabase {
    public static void main(String[] args) {
        String username = "root";
        String password = "123456";
        
        // Check these databases
        String[] databases = {
            "ticket_user_db",
            "ticket_show_db", 
            "ticket_order_db"
        };
        
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            for (String dbName : databases) {
                System.out.println("\n=== Checking database: " + dbName + " ===");
                String url = "jdbc:mysql://localhost:3306/" + dbName + "?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai";
                
                try {
                    Connection conn = DriverManager.getConnection(url, username, password);
                    Statement stmt = conn.createStatement();
                    
                    // Show all tables
                    ResultSet rs = stmt.executeQuery("SHOW TABLES");
                    System.out.println("Tables:");
                    boolean hasTables = false;
                    while (rs.next()) {
                        hasTables = true;
                        String tableName = rs.getString(1);
                        System.out.println("  - " + tableName);
                        
                        // Show table structure
                        Statement stmt2 = conn.createStatement();
                        ResultSet rs2 = stmt2.executeQuery("DESCRIBE " + tableName);
                        System.out.println("    Fields:");
                        while (rs2.next()) {
                            System.out.println("      " + rs2.getString("Field") + " | " + rs2.getString("Type") + " | " + rs2.getString("Null") + " | " + rs2.getString("Key"));
                        }
                        rs2.close();
                        stmt2.close();
                        System.out.println();
                    }
                    
                    if (!hasTables) {
                        System.out.println("  Database is empty, no tables found");
                    }
                    
                    rs.close();
                    stmt.close();
                    conn.close();
                    
                } catch (Exception e) {
                    System.out.println("  Connection failed: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}