package com.reporting.framework.unit;

import com.reporting.framework.exception.ReportExecutionException;
import com.reporting.framework.mapper.ResultSetToMapConverter;
import com.reporting.framework.metadata.ColumnMapping;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for ResultSetToMapConverter with explicit column mappings.
 */
class ResultSetToMapConverterTest {

    private Connection connection;
    private ResultSetToMapConverter converter;

    @BeforeEach
    void setup() throws Exception {
        // Setup H2 in-memory database with unique name per test
        String dbUrl = "jdbc:h2:mem:test" + System.nanoTime() + ";MODE=MSSQLServer;DB_CLOSE_DELAY=-1";
        connection = DriverManager.getConnection(dbUrl, "sa", "");
        converter = new ResultSetToMapConverter();

        // Create test table
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE test_data (" +
                    "id INT PRIMARY KEY, " +
                    "name VARCHAR(50), " +
                    "amount DECIMAL(18,2), " +
                    "created_date DATE)");

            stmt.execute("INSERT INTO test_data VALUES (1, 'Test 1', 100.50, '2024-01-01')");
            stmt.execute("INSERT INTO test_data VALUES (2, 'Test 2', 200.75, '2024-01-02')");
        }
    }

    @AfterEach
    void cleanup() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    @DisplayName("Convert ResultSet using explicit column mappings")
    void testConvertWithExplicitMappings() throws SQLException {
        List<ColumnMapping> mappings = new ArrayList<>();
        mappings.add(new ColumnMapping("ID", "id", true));
        mappings.add(new ColumnMapping("NAME", "name", true));
        mappings.add(new ColumnMapping("AMOUNT", "amount", true));
        mappings.add(new ColumnMapping("CREATED_DATE", "createdDate", true));

        String sql = "SELECT id, name, amount, created_date FROM test_data ORDER BY id";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            List<Map<String, Object>> result = converter.convert(rs, mappings);

            assertThat(result).hasSize(2);

            Map<String, Object> row1 = result.get(0);
            assertThat(row1.get("id")).isEqualTo(1);
            assertThat(row1.get("name")).isEqualTo("Test 1");
            assertThat(row1.get("amount")).isEqualTo(new BigDecimal("100.50"));
            assertThat(row1.get("createdDate")).isEqualTo(LocalDate.of(2024, 1, 1));
        }
    }

    @Test
    @DisplayName("Convert snake_case columns to camelCase fields using explicit mappings")
    void testConvertSnakeCaseToCamelCase() throws SQLException {
        // Create table with snake_case columns
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE snake_case_table (" +
                    "employee_id INT PRIMARY KEY, " +
                    "first_name VARCHAR(50), " +
                    "last_name VARCHAR(50), " +
                    "hire_date DATE)");

            stmt.execute("INSERT INTO snake_case_table VALUES " +
                    "(1, 'John', 'Doe', '2020-01-15')");
        }

        List<ColumnMapping> mappings = new ArrayList<>();
        mappings.add(new ColumnMapping("employee_id", "employeeId", true));
        mappings.add(new ColumnMapping("first_name", "firstName", true));
        mappings.add(new ColumnMapping("last_name", "lastName", true));
        mappings.add(new ColumnMapping("hire_date", "hireDate", true));

        String sql = "SELECT employee_id, first_name, last_name, hire_date FROM snake_case_table";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            List<Map<String, Object>> result = converter.convert(rs, mappings);

            assertThat(result).hasSize(1);

            Map<String, Object> row = result.get(0);
            assertThat(row).containsKey("employeeId");
            assertThat(row).containsKey("firstName");
            assertThat(row).containsKey("lastName");
            assertThat(row).containsKey("hireDate");

            assertThat(row.get("employeeId")).isEqualTo(1);
            assertThat(row.get("firstName")).isEqualTo("John");
            assertThat(row.get("lastName")).isEqualTo("Doe");
        }
    }

    @Test
    @DisplayName("Handle null values correctly")
    void testNullValues() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("INSERT INTO test_data VALUES (3, NULL, NULL, NULL)");
        }

        List<ColumnMapping> mappings = new ArrayList<>();
        mappings.add(new ColumnMapping("ID", "id", true));
        mappings.add(new ColumnMapping("NAME", "name", false));
        mappings.add(new ColumnMapping("AMOUNT", "amount", false));
        mappings.add(new ColumnMapping("CREATED_DATE", "createdDate", false));

        String sql = "SELECT id, name, amount, created_date FROM test_data WHERE id = 3";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            List<Map<String, Object>> result = converter.convert(rs, mappings);

            assertThat(result).hasSize(1);
            Map<String, Object> row = result.get(0);
            assertThat(row.get("id")).isEqualTo(3);
            assertThat(row.get("name")).isNull();
            assertThat(row.get("amount")).isNull();
            assertThat(row.get("createdDate")).isNull();
        }
    }

    @Test
    @DisplayName("Handle empty ResultSet")
    void testEmptyResultSet() throws SQLException {
        List<ColumnMapping> mappings = new ArrayList<>();
        mappings.add(new ColumnMapping("ID", "id", true));
        mappings.add(new ColumnMapping("NAME", "name", true));

        String sql = "SELECT id, name FROM test_data WHERE id = 999";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            List<Map<String, Object>> result = converter.convert(rs, mappings);

            assertThat(result).isEmpty();
        }
    }

    @Test
    @DisplayName("Throw exception when required column is missing")
    void testMissingRequiredColumn() throws SQLException {
        List<ColumnMapping> mappings = new ArrayList<>();
        mappings.add(new ColumnMapping("ID", "id", true));
        mappings.add(new ColumnMapping("MISSING_COLUMN", "missingField", true));

        String sql = "SELECT id, name FROM test_data";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            assertThatThrownBy(() -> converter.convert(rs, mappings))
                    .isInstanceOf(ReportExecutionException.class)
                    .hasMessageContaining("Required columns not found")
                    .hasMessageContaining("MISSING_COLUMN");
        }
    }

    @Test
    @DisplayName("Ignore optional columns that are missing")
    void testMissingOptionalColumn() throws SQLException {
        List<ColumnMapping> mappings = new ArrayList<>();
        mappings.add(new ColumnMapping("ID", "id", true));
        mappings.add(new ColumnMapping("NAME", "name", true));
        mappings.add(new ColumnMapping("OPTIONAL_COLUMN", "optionalField", false));

        String sql = "SELECT id, name FROM test_data";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            List<Map<String, Object>> result = converter.convert(rs, mappings);

            assertThat(result).hasSize(2);
            assertThat(result.get(0)).containsKeys("id", "name");
            assertThat(result.get(0)).doesNotContainKey("optionalField");
        }
    }

    @Test
    @DisplayName("Throw exception when mappings are null or empty")
    void testNullOrEmptyMappings() throws SQLException {
        String sql = "SELECT id, name FROM test_data";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            assertThatThrownBy(() -> converter.convert(rs, null))
                    .isInstanceOf(ReportExecutionException.class)
                    .hasMessageContaining("Column mappings cannot be null or empty");

            ResultSet rs2 = stmt.executeQuery(sql);
            assertThatThrownBy(() -> converter.convert(rs2, new ArrayList<>()))
                    .isInstanceOf(ReportExecutionException.class)
                    .hasMessageContaining("Column mappings cannot be null or empty");
        }
    }
}
