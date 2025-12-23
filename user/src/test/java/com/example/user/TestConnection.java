package com.example.user;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TestConnection {

    @Test
    public void testConnection() {
        String url = "jdbc:postgresql://aws-1-eu-north-1.pooler.supabase.com:5432/postgres";
        String user = "postgres.yldotyunksweuovyknzg";
        String password = "Cypay.Cytech";

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return;
        }

        System.out.println("Testing connection to: " + url);
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("Connection successful!");
            System.out.println("Driver: " + conn.getMetaData().getDriverName());
            System.out.println("Version: " + conn.getMetaData().getDriverVersion());
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Connection failed", e);
        }
    }
}
