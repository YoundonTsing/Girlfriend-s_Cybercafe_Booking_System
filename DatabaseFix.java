import java.sql.*;

/**
 * 修复ticket-user服务数据库表结构问题
 * 添加缺失的avatar字段到t_user表
 */
public class DatabaseFix {
    
    // 从配置文件读取的数据库连接信息
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/ticket_user_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "123456";
    
    public static void main(String[] args) {
        System.out.println("开始修复数据库表结构...");
        
        try {
            // 注册MySQL驱动
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // 建立数据库连接
            try (Connection connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
                System.out.println("✓ 数据库连接成功！");
                
                // 检查avatar字段是否存在
                boolean avatarExists = checkColumnExists(connection, "t_user", "avatar");
                
                if (avatarExists) {
                    System.out.println("✓ avatar字段已存在，无需修复");
                } else {
                    System.out.println("× avatar字段不存在，正在添加...");
                    addAvatarColumn(connection);
                    System.out.println("✓ avatar字段添加成功！");
                }
                
                // 验证修复结果
                showTableStructure(connection);
                
                System.out.println("\n✓ 数据库修复完成！现在可以重新启动ticket-user服务了。");
                
            }
            
        } catch (ClassNotFoundException e) {
            System.err.println("× MySQL驱动未找到，请确保已添加MySQL连接器依赖");
            System.err.println("错误详情: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("× 数据库操作失败");
            System.err.println("错误详情: " + e.getMessage());
            if (e.getMessage().contains("Access denied")) {
                System.err.println("提示: 请检查数据库用户名和密码是否正确");
            } else if (e.getMessage().contains("Unknown database")) {
                System.err.println("提示: 请确保数据库 'ticket_user_db' 已创建");
            }
        } catch (Exception e) {
            System.err.println("× 未知错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 检查表中是否存在指定字段
     */
    private static boolean checkColumnExists(Connection connection, String tableName, String columnName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_SCHEMA = 'ticket_user_db' " +
                    "AND TABLE_NAME = ? " +
                    "AND COLUMN_NAME = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, tableName);
            stmt.setString(2, columnName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }
    
    /**
     * 添加avatar字段
     */
    private static void addAvatarColumn(Connection connection) throws SQLException {
        String sql = "ALTER TABLE t_user ADD COLUMN avatar VARCHAR(255) COMMENT '头像URL' AFTER email";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }
    
    /**
     * 显示表结构
     */
    private static void showTableStructure(Connection connection) throws SQLException {
        System.out.println("\n当前t_user表结构:");
        System.out.println("+--------------+----------+------+-----+---------+-------+");
        System.out.println("| Field        | Type     | Null | Key | Default | Extra |");
        System.out.println("+--------------+----------+------+-----+---------+-------+");
        
        String sql = "DESCRIBE t_user";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                System.out.printf("| %-12s | %-8s | %-4s | %-3s | %-7s | %-5s |\n",
                    rs.getString("Field"),
                    rs.getString("Type"),
                    rs.getString("Null"),
                    rs.getString("Key"),
                    rs.getString("Default") != null ? rs.getString("Default") : "NULL",
                    rs.getString("Extra") != null ? rs.getString("Extra") : ""
                );
            }
        }
        System.out.println("+--------------+----------+------+-----+---------+-------+");
    }
}