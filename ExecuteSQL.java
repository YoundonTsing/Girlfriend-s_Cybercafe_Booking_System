import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class ExecuteSQL {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/ticket_show_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai";
        String username = "root";
        String password = "123456";
        String sqlFile = "sql/add_ticket_tables_to_show_db.sql";
        
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url, username, password);
            Statement stmt = conn.createStatement();
            
            // 读取SQL文件
            BufferedReader reader = new BufferedReader(new FileReader(sqlFile));
            StringBuilder sql = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("--")) {
                    sql.append(line).append(" ");
                    if (line.endsWith(";")) {
                        String sqlStatement = sql.toString().trim();
                        if (!sqlStatement.isEmpty()) {
                            System.out.println("Executing: " + sqlStatement.substring(0, Math.min(50, sqlStatement.length())) + "...");
                            stmt.execute(sqlStatement);
                        }
                        sql = new StringBuilder();
                    }
                }
            }
            
            reader.close();
            stmt.close();
            conn.close();
            
            System.out.println("SQL script executed successfully!");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}