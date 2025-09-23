import java.sql.*;

public class AddTestUser {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true";
        String username = "root";
        String password = "123456";
        
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url, username, password);
            Statement stmt = conn.createStatement();
            
            // Create database
            stmt.execute("CREATE DATABASE IF NOT EXISTS ticket_user_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            stmt.execute("USE ticket_user_db");
            
            // Create role table
            stmt.execute("CREATE TABLE IF NOT EXISTS t_role (id BIGINT PRIMARY KEY AUTO_INCREMENT, role_name VARCHAR(50) NOT NULL UNIQUE, role_code VARCHAR(50) NOT NULL UNIQUE, description VARCHAR(200), create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0)");
            
            // Create user table
            stmt.execute("CREATE TABLE IF NOT EXISTS t_user (id BIGINT PRIMARY KEY AUTO_INCREMENT, username VARCHAR(50) NOT NULL UNIQUE, password VARCHAR(100) NOT NULL, nickname VARCHAR(50), phone VARCHAR(20) NOT NULL UNIQUE, email VARCHAR(100) UNIQUE, avatar VARCHAR(255), gender TINYINT, birthday DATE, status TINYINT NOT NULL DEFAULT 1, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, last_login_time DATETIME, is_deleted TINYINT NOT NULL DEFAULT 0)");
            
            // Create user role table
            stmt.execute("CREATE TABLE IF NOT EXISTS t_user_role (id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, role_id BIGINT NOT NULL, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            
            // Insert roles
            stmt.execute("INSERT IGNORE INTO t_role (role_name, role_code, description) VALUES ('ADMIN', 'ADMIN', 'Administrator'), ('USER', 'USER', 'Normal User')");
            
            // Insert test users
            stmt.execute("INSERT IGNORE INTO t_user (username, password, nickname, phone, email, status) VALUES ('testuser', '09c4f5615e87488cbb122ccd1e05d8b4', 'Test User', '13800138000', 'testuser@example.com', 1), ('admin', 'e10adc3949ba59abbe56e057f20f883e', 'Administrator', '13800138001', 'admin@example.com', 1)");
            
            // Link user roles
            stmt.execute("INSERT IGNORE INTO t_user_role (user_id, role_id) VALUES (1, 1), (2, 2)");
            
            System.out.println("SUCCESS: Test users added!");
            System.out.println("User: testuser, Password: pasword123");
            System.out.println("User: admin, Password: 123456");
            
            // Verify data
            ResultSet rs = stmt.executeQuery("SELECT username, nickname FROM t_user");
            while (rs.next()) {
                System.out.println("Found user: " + rs.getString("username") + " (" + rs.getString("nickname") + ")");
            }
            
            conn.close();
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}