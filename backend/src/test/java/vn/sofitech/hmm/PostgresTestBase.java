package vn.sofitech.hmm;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;

/**
 * Lớp cha cho test tích hợp với PostgreSQL thật. Dùng database riêng {@code hmm_loyalty_test} (tạo lại một lần mỗi lượt chạy),
 * không đụng dữ liệu demo. Các lớp con dùng chung một Spring context.
 * Lớp con gắn {@code @EnabledIf("vn.sofitech.hmm.PostgresTestBase#databaseAvailable")} để tự bỏ qua khi máy không có PostgreSQL.
 */
@SpringBootTest(properties = "app.seed-demo-data=false")
public abstract class PostgresTestBase {

    static final String TEST_DB = "hmm_loyalty_test";
    private static Boolean available;

    static String password() {
        String env = System.getenv("DB_PASSWORD");
        if (env != null) return env;
        try {
            Properties p = new Properties();
            Path f = Path.of("local.properties");
            if (Files.exists(f)) try (var r = Files.newBufferedReader(f)) { p.load(r); }
            return p.getProperty("DB_PASSWORD", "postgres");
        } catch (IOException e) {
            return "postgres";
        }
    }

    static String host() {
        return System.getenv().getOrDefault("DB_HOST", "localhost") + ":" + System.getenv().getOrDefault("DB_PORT", "5432");
    }

    /** Điều kiện chạy test: kết nối được PostgreSQL; đồng thời tạo lại database test trống (chỉ một lần). */
    public static synchronized boolean databaseAvailable() {
        if (available != null) return available;
        available = false;
        try (Connection c = DriverManager.getConnection("jdbc:postgresql://" + host() + "/postgres",
                System.getenv().getOrDefault("DB_USER", "postgres"), password()); Statement st = c.createStatement()) {
            st.execute("DROP DATABASE IF EXISTS " + TEST_DB + " WITH (FORCE)");
            st.execute("CREATE DATABASE " + TEST_DB + " ENCODING 'UTF8' TEMPLATE template0");
            return available = true;
        } catch (Exception e) {
            System.out.println("Bỏ qua test tích hợp: không kết nối được PostgreSQL – " + e.getMessage());
            return false;
        }
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> "jdbc:postgresql://" + host() + "/" + TEST_DB);
        r.add("spring.datasource.password", PostgresTestBase::password);
    }
}
