import java.sql.*;

public class CleanDuplicateStock {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/ticket_show_db?useSSL=false&serverTimezone=UTC";
        String username = "root";
        String password = "123456";
        
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            System.out.println("Database connected successfully");
            
            // Start transaction
            conn.setAutoCommit(false);
            
            // Query all records for ticket_id = 2, ordered by ID
            String selectQuery = "SELECT * FROM t_ticket_stock WHERE ticket_id = 2 ORDER BY id";
            try (PreparedStatement stmt = conn.prepareStatement(selectQuery);
                 ResultSet rs = stmt.executeQuery()) {
                
                System.out.println("=== Current Records for Ticket ID 2 ===");
                boolean isFirst = true;
                long firstId = 0;
                int recordCount = 0;
                
                while (rs.next()) {
                    recordCount++;
                    long id = rs.getLong("id");
                    int totalStock = rs.getInt("total_stock");
                    int lockedStock = rs.getInt("locked_stock");
                    int soldStock = rs.getInt("sold_stock");
                    int version = rs.getInt("version");
                    Timestamp createTime = rs.getTimestamp("create_time");
                    
                    System.out.println("Record " + recordCount + " - ID: " + id + 
                                     ", Total: " + totalStock + 
                                     ", Locked: " + lockedStock + 
                                     ", Sold: " + soldStock + 
                                     ", Version: " + version + 
                                     ", Created: " + createTime);
                    
                    if (isFirst) {
                        firstId = id;
                        isFirst = false;
                        System.out.println("  -> This record will be KEPT (original record)");
                    } else {
                        System.out.println("  -> This record will be DELETED (duplicate)");
                    }
                }
                
                System.out.println("\nTotal records found: " + recordCount);
                
                if (recordCount > 1) {
                    // Delete all duplicate records except the first one
                    String deleteQuery = "DELETE FROM t_ticket_stock WHERE ticket_id = 2 AND id != ?";
                    try (PreparedStatement deleteStmt = conn.prepareStatement(deleteQuery)) {
                        deleteStmt.setLong(1, firstId);
                        int deletedCount = deleteStmt.executeUpdate();
                        
                        System.out.println("\n=== Cleanup Results ===");
                        System.out.println("Deleted " + deletedCount + " duplicate records");
                        System.out.println("Kept record with ID: " + firstId);
                        
                        // 提交事务
                        conn.commit();
                        System.out.println("Transaction committed successfully");
                        
                        // 验证清理结果
                        String verifyQuery = "SELECT COUNT(*) as count FROM t_ticket_stock WHERE ticket_id = 2";
                        try (PreparedStatement verifyStmt = conn.prepareStatement(verifyQuery);
                             ResultSet verifyRs = verifyStmt.executeQuery()) {
                            
                            if (verifyRs.next()) {
                                int remainingCount = verifyRs.getInt("count");
                                System.out.println("\n=== Verification ===");
                                System.out.println("Remaining records for ticket_id = 2: " + remainingCount);
                                
                                if (remainingCount == 1) {
                                    System.out.println("[SUCCESS] Cleanup successful! Only one record remains.");
                                } else {
                                    System.out.println("[ERROR] Cleanup may have failed. Expected 1 record, found " + remainingCount);
                                }
                            }
                        }
                    }
                } else {
                    System.out.println("\nNo duplicate records found. No cleanup needed.");
                    conn.rollback();
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Database operation failed: " + e.getMessage());
            e.printStackTrace();
            try {
                // 回滚事务
                System.err.println("Rolling back transaction...");
            } catch (Exception rollbackEx) {
                System.err.println("Rollback failed: " + rollbackEx.getMessage());
            }
        }
    }
}