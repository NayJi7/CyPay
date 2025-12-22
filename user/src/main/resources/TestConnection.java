import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TestConnection {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://db.yldotyunksweuovyknzg.supabase.co:5432/postgres?sslmode=require";
        String user = "postgres";
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
        }
    }
}
