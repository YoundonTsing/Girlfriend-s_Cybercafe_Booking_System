package com.ticketsystem.show.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

// @Component - 禁用自动SQL执行，避免表已存在错误
public class SQLExecutor implements CommandLineRunner {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private static boolean executed = false;
    
    @Override
    public void run(String... args) throws Exception {
        if (!executed) {
            executed = true;
            executeSQLScript();
        }
    }
    
    private void executeSQLScript() {
        try {
            String sqlFile = "d:/Tickets/sql/add_ticket_tables_to_show_db.sql";
            BufferedReader reader = new BufferedReader(new FileReader(sqlFile));
            StringBuilder sql = new StringBuilder();
            String line;
            List<String> statements = new ArrayList<>();
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("--") && !line.startsWith("USE")) {
                    sql.append(line).append(" ");
                    if (line.endsWith(";")) {
                        String sqlStatement = sql.toString().trim();
                        if (!sqlStatement.isEmpty()) {
                            statements.add(sqlStatement);
                        }
                        sql = new StringBuilder();
                    }
                }
            }
            reader.close();
            
            // 执行SQL语句
            for (String statement : statements) {
                try {
                    System.out.println("Executing: " + statement.substring(0, Math.min(50, statement.length())) + "...");
                    jdbcTemplate.execute(statement);
                } catch (Exception e) {
                    System.out.println("Error executing statement: " + e.getMessage());
                    // 继续执行其他语句
                }
            }
            
            System.out.println("SQL script execution completed!");
            
        } catch (Exception e) {
            System.out.println("Error reading SQL file: " + e.getMessage());
        }
    }
}