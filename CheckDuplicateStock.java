import java.sql.*;

public class CheckDuplicateStock {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/ticket_show_db?useSSL=false&serverTimezone=UTC";
        String username = "root";
        String password = "123456";
        
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            System.out.println("Database connected successfully");
            
            // First check if table exists and show structure
            String showTablesQuery = "SHOW TABLES LIKE 't_ticket_stock'";
            try (PreparedStatement stmt = conn.prepareStatement(showTablesQuery);
                 ResultSet rs = stmt.executeQuery()) {
                
                if (rs.next()) {
                    System.out.println("Table t_ticket_stock exists");
                } else {
                    System.out.println("Table t_ticket_stock does not exist!");
                    return;
                }
            }
            
            // Count total records
            String countQuery = "SELECT COUNT(*) as total FROM t_ticket_stock";
            try (PreparedStatement stmt = conn.prepareStatement(countQuery);
                 ResultSet rs = stmt.executeQuery()) {
                
                if (rs.next()) {
                    System.out.println("Total records in t_ticket_stock: " + rs.getInt("total"));
                }
            }
            
            // Query duplicate ticket_id
            String duplicateQuery = "SELECT ticket_id, COUNT(*) as count FROM t_ticket_stock GROUP BY ticket_id HAVING COUNT(*) > 1";
            try (PreparedStatement stmt = conn.prepareStatement(duplicateQuery);
                 ResultSet rs = stmt.executeQuery()) {
                
                System.out.println("=== Duplicate Stock Records ===");
                boolean hasDuplicates = false;
                while (rs.next()) {
                    hasDuplicates = true;
                    System.out.println("Ticket ID: " + rs.getLong("ticket_id") + ", Count: " + rs.getInt("count"));
                }
                
                if (!hasDuplicates) {
                    System.out.println("No duplicate records found");
                }
            }
            
            // Query all records for ticket_id = 2
            String ticket2Query = "SELECT * FROM t_ticket_stock WHERE ticket_id = 2";
            try (PreparedStatement stmt = conn.prepareStatement(ticket2Query);
                 ResultSet rs = stmt.executeQuery()) {
                
                System.out.println("\n=== All Stock Records for Ticket ID 2 ===");
                int count = 0;
                while (rs.next()) {
                    count++;
                    System.out.println("Record " + count + " - ID: " + rs.getLong("id") + 
                                     ", Ticket ID: " + rs.getLong("ticket_id") + 
                                     ", Total Stock: " + rs.getInt("total_stock") + 
                                     ", Locked Stock: " + rs.getInt("locked_stock") + 
                                     ", Sold Stock: " + rs.getInt("sold_stock") + 
                                     ", Version: " + rs.getInt("version") + 
                                     ", Create Time: " + rs.getTimestamp("create_time"));
                }
                
                if (count == 0) {
                    System.out.println("No records found for ticket_id = 2");
                } else {
                    System.out.println("Found " + count + " records for ticket_id = 2");
                }
            }
            
            // Show all ticket_ids and their counts
            String allTicketsQuery = "SELECT ticket_id, COUNT(*) as count FROM t_ticket_stock GROUP BY ticket_id ORDER BY ticket_id";
            try (PreparedStatement stmt = conn.prepareStatement(allTicketsQuery);
                 ResultSet rs = stmt.executeQuery()) {
                
                System.out.println("\n=== All Ticket IDs and Record Counts ===");
                while (rs.next()) {
                    System.out.println("Ticket ID: " + rs.getLong("ticket_id") + ", Count: " + rs.getInt("count"));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Database operation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}