import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * 修复用户表结构 - 添加缺失的avatar字段
 */
public class FixUserTable {
    
    // 数据库连接配置（根据实际情况修改）
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/ticket_user_db?useSSL=false&serverTimezone=UTC&characterEncoding=utf8";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "root";
    
    public static void main(String[] args) {
        try {
            // 注册MySQL驱动
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // 建立连接
            Connection connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
            System.out.println("数据库连接成功！");
            
            // 执行SQL脚本
            executeSqlScript(connection, "sql/add_avatar_column.sql");
            
            // 关闭连接
            connection.close();
            System.out.println("数据库连接已关闭");
            
        } catch (Exception e) {
            System.err.println("执行失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 执行SQL脚本文件
     */
    private static void executeSqlScript(Connection connection, String scriptPath) throws Exception {
        Statement statement = connection.createStatement();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(scriptPath))) {
            StringBuilder sqlBuilder = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // 跳过注释和空行
                if (line.isEmpty() || line.startsWith("--") || line.startsWith("#")) {
                    continue;
                }
                
                sqlBuilder.append(line).append("\n");
                
                // 如果遇到分号，执行SQL语句
                if (line.endsWith(";")) {
                    String sql = sqlBuilder.toString().trim();
                    if (!sql.isEmpty()) {
                        try {
                            // 跳过某些特殊SQL语句
                            if (sql.startsWith("USE ") || 
                                sql.startsWith("SET ") || 
                                sql.startsWith("PREPARE ") ||
                                sql.startsWith("EXECUTE ") ||
                                sql.startsWith("DEALLOCATE ")) {
                                System.out.println("执行SQL: " + sql);
                                statement.execute(sql);
                            } else if (sql.startsWith("SELECT ")) {
                                System.out.println("执行查询: " + sql);
                                var resultSet = statement.executeQuery(sql);
                                while (resultSet.next()) {
                                    System.out.println("结果: " + resultSet.getString(1));
                                }
                            } else if (sql.startsWith("ALTER ")) {
                                System.out.println("执行表结构修改: " + sql);
                                statement.execute(sql);
                                System.out.println("✓ avatar字段添加成功！");
                            } else if (sql.startsWith("DESCRIBE ")) {
                                System.out.println("查看表结构: " + sql);
                                var resultSet = statement.executeQuery(sql);
                                System.out.println("t_user表结构:");
                                while (resultSet.next()) {
                                    System.out.printf("  %s %s %s\n", 
                                        resultSet.getString(1), 
                                        resultSet.getString(2),
                                        resultSet.getString(3));
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("执行SQL失败: " + sql);
                            System.err.println("错误: " + e.getMessage());
                        }
                    }
                    sqlBuilder.setLength(0);
                }
            }
        }
        
        statement.close();
    }
}