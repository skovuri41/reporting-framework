# Usage Examples

## Complete Working Example

This example demonstrates the complete workflow from database setup to executing reports.

### 1. Database Setup

```sql
-- Create metadata table
CREATE TABLE REPORT_METADATA (
    REPORT_NAME VARCHAR(100) PRIMARY KEY,
    STORED_PROCEDURE VARCHAR(200) NOT NULL,
    RESULT_CLASS VARCHAR(500) NOT NULL,
    METADATA_JSON NVARCHAR(MAX) NOT NULL,
    CREATED_DATE DATETIME DEFAULT GETDATE(),
    UPDATED_DATE DATETIME DEFAULT GETDATE()
);

-- Create business table
CREATE TABLE employees (
    employee_id INT PRIMARY KEY IDENTITY(1,1),
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    salary DECIMAL(18,2) NOT NULL,
    hire_date DATE NOT NULL,
    department_id INT NOT NULL
);

-- Create stored procedure
CREATE PROCEDURE dbo.usp_GetEmployees
    @DepartmentId INT,
    @StartDate DATE = NULL,
    @TotalCount INT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    SELECT employee_id, first_name, last_name, email, salary, hire_date, department_id
    FROM employees
    WHERE department_id = @DepartmentId
        AND (@StartDate IS NULL OR hire_date >= @StartDate)
    ORDER BY hire_date DESC;

    SELECT @TotalCount = COUNT(*)
    FROM employees
    WHERE department_id = @DepartmentId
        AND (@StartDate IS NULL OR hire_date >= @StartDate);
END;
GO

-- Insert metadata
INSERT INTO REPORT_METADATA (REPORT_NAME, STORED_PROCEDURE, RESULT_CLASS, METADATA_JSON)
VALUES (
    'employee_report',
    'dbo.usp_GetEmployees',
    'com.example.reporting.EmployeeReport',
    '{
        "reportName": "employee_report",
        "storedProcedure": "dbo.usp_GetEmployees",
        "resultClass": "com.example.reporting.EmployeeReport",
        "inputParameters": [
            {
                "name": "DepartmentId",
                "sqlType": "INTEGER",
                "javaType": "java.lang.Integer",
                "required": true
            },
            {
                "name": "StartDate",
                "sqlType": "DATE",
                "javaType": "java.time.LocalDate",
                "required": false
            }
        ],
        "outputParameters": [
            {
                "name": "TotalCount",
                "sqlType": "INTEGER",
                "javaType": "java.lang.Integer"
            }
        ],
        "resultSetMapping": {
            "strategy": "JACKSON",
            "columnNameFormat": "SNAKE_CASE",
            "fieldNameFormat": "CAMEL_CASE"
        }
    }'
);
```

### 2. Create POJO Classes

```java
package com.example.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;

// Result POJO - matches the SELECT columns from stored procedure
public class EmployeeReport {
    private Integer employeeId;
    private String firstName;
    private String lastName;
    private String email;
    private BigDecimal salary;
    private LocalDate hireDate;
    private Integer departmentId;

    // Getters and setters...
}

// Optional: Input POJO for type-safe parameters
public class EmployeeReportRequest {
    private Integer departmentId;
    private LocalDate startDate;

    // Getters and setters...
}
```

### 3. Execute the Report

```java
package com.example.reporting;

import com.reporting.framework.api.ReportResult;
import com.reporting.framework.api.ReportService;
import com.reporting.framework.connection.ConnectionProvider;
import com.reporting.framework.connection.DataSourceConnectionProvider;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class ReportingExample {

    public static void main(String[] args) {
        // 1. Setup DataSource (HikariCP recommended for production)
        DataSource dataSource = createDataSource();

        // 2. Create ConnectionProvider
        ConnectionProvider connectionProvider = new DataSourceConnectionProvider(dataSource);

        // 3. Create ReportService
        ReportService reportService = new ReportService(connectionProvider);

        // 4. Execute report with Map-based parameters
        executeReportWithMap(reportService);

        // 5. Execute report with POJO-based parameters (type-safe)
        executeReportWithPojo(reportService);
    }

    private static void executeReportWithMap(ReportService reportService) {
        System.out.println("=== Executing report with Map parameters ===");

        // Prepare input parameters
        Map<String, Object> params = new HashMap<>();
        params.put("DepartmentId", 10);
        params.put("StartDate", LocalDate.of(2020, 1, 1));

        // Execute report
        ReportResult<EmployeeReport> result = reportService.execute("employee_report", params);

        // Access results
        System.out.println("Report: " + result.getReportName());
        System.out.println("Execution Time: " + result.getExecutionTime());
        System.out.println("Result Count: " + result.getResultCount());

        // Access output parameters
        Integer totalCount = (Integer) result.getOutputParameter("TotalCount");
        System.out.println("Total Count (output param): " + totalCount);

        // Process results
        for (EmployeeReport employee : result.getResults()) {
            System.out.printf("Employee: %s %s - Salary: $%,.2f - Hired: %s%n",
                    employee.getFirstName(),
                    employee.getLastName(),
                    employee.getSalary(),
                    employee.getHireDate());
        }
    }

    private static void executeReportWithPojo(ReportService reportService) {
        System.out.println("\n=== Executing report with POJO parameters ===");

        // Prepare type-safe input POJO
        EmployeeReportRequest request = new EmployeeReportRequest();
        request.setDepartmentId(10);
        request.setStartDate(LocalDate.of(2020, 1, 1));

        // Execute report
        ReportResult<EmployeeReport> result = reportService.execute("employee_report", request);

        // Access results (same as above)
        System.out.println("Total employees: " + result.getResultCount());
    }

    private static DataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlserver://localhost:1433;databaseName=ReportingDB;encrypt=true;trustServerCertificate=true");
        config.setUsername("sa");
        config.setPassword("YourPassword");
        config.setMaximumPoolSize(10);
        config.setConnectionTimeout(30000);

        return new HikariDataSource(config);
    }
}
```

## Spring Boot Integration

```java
package com.example.reporting.config;

import com.reporting.framework.api.ReportService;
import com.reporting.framework.connection.ConnectionProvider;
import com.reporting.framework.connection.DataSourceConnectionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class ReportingConfig {

    @Bean
    public ConnectionProvider connectionProvider(DataSource dataSource) {
        return new DataSourceConnectionProvider(dataSource);
    }

    @Bean
    public ReportService reportService(ConnectionProvider connectionProvider) {
        return new ReportService(connectionProvider, true); // Enable caching
    }
}
```

```java
package com.example.reporting.controller;

import com.example.reporting.EmployeeReport;
import com.example.reporting.EmployeeReportRequest;
import com.reporting.framework.api.ReportResult;
import com.reporting.framework.api.ReportService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/employees")
    public ReportResult<EmployeeReport> getEmployeeReport(
            @RequestBody EmployeeReportRequest request) {
        return reportService.execute("employee_report", request);
    }

    @PostMapping("/clear-cache")
    public void clearCache() {
        reportService.clearMetadataCache();
    }
}
```

## Advanced: Adding a New Report at Runtime

```java
// 1. Insert new metadata into database
String sql = "INSERT INTO REPORT_METADATA (REPORT_NAME, STORED_PROCEDURE, RESULT_CLASS, METADATA_JSON) " +
             "VALUES (?, ?, ?, ?)";

// 2. Create POJO class
public class NewReport {
    // Fields matching stored procedure columns
}

// 3. Execute immediately - framework loads metadata on-demand
ReportResult<NewReport> result = reportService.execute("new_report", params);

// 4. If metadata was updated, invalidate cache
reportService.invalidateMetadataCache("new_report");
```

## Error Handling

```java
try {
    ReportResult<EmployeeReport> result = reportService.execute("employee_report", params);
    // Process results
} catch (MetadataException e) {
    // Metadata loading or parsing failed
    System.err.println("Metadata error: " + e.getMessage());
    System.err.println("Report name: " + e.getReportName());
} catch (ReportExecutionException e) {
    // Stored procedure execution failed
    System.err.println("Execution error: " + e.getMessage());
    System.err.println("Report: " + e.getReportName());
    System.err.println("Procedure: " + e.getStoredProcedure());
}
```

## Testing Your Reports

```java
@Test
void testEmployeeReport() {
    // Setup test database and connection
    ConnectionProvider provider = new SimpleConnectionProvider(
        "jdbc:sqlserver://localhost:1433;databaseName=TestDB",
        "sa",
        "password"
    );

    ReportService service = new ReportService(provider);

    Map<String, Object> params = Map.of("DepartmentId", 10);
    ReportResult<EmployeeReport> result = service.execute("employee_report", params);

    assertNotNull(result);
    assertFalse(result.getResults().isEmpty());
    assertEquals(10, result.getResults().get(0).getDepartmentId());
}
```

## Performance Tips

1. **Enable Metadata Caching** (default: enabled)
   ```java
   ReportService service = new ReportService(provider, true);
   ```

2. **Use Connection Pooling** (HikariCP recommended)
   ```java
   DataSource dataSource = new HikariDataSource(config);
   ConnectionProvider provider = new DataSourceConnectionProvider(dataSource);
   ```

3. **Invalidate Cache When Metadata Changes**
   ```java
   service.invalidateMetadataCache("employee_report");
   ```

4. **Use POJO Input Parameters** for compile-time type safety
   ```java
   EmployeeReportRequest request = new EmployeeReportRequest();
   request.setDepartmentId(10);
   service.execute("employee_report", request);
   ```
