-- Test database schema for H2 in SQL Server mode
-- This schema is used for integration testing

-- Create metadata table (new JSON structure)
CREATE TABLE REPORT_METADATA (
    REPORT_ID VARCHAR(100) PRIMARY KEY,
    REPORT_NAME VARCHAR(255) NOT NULL,
    REPORT_DESCRIPTION VARCHAR(1000),
    METADATA_JSON NVARCHAR(MAX) NOT NULL,
    CREATED_DATE DATETIME DEFAULT CURRENT_TIMESTAMP,
    MODIFIED_DATE DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Create test tables
CREATE TABLE employees (
    employee_id INT PRIMARY KEY IDENTITY(1,1),
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    salary DECIMAL(18,2) NOT NULL,
    hire_date DATE NOT NULL,
    department_id INT NOT NULL
);

CREATE TABLE sales (
    sale_id INT PRIMARY KEY IDENTITY(1,1),
    product_name VARCHAR(100) NOT NULL,
    sales_amount DECIMAL(18,2) NOT NULL,
    sales_date DATE NOT NULL,
    customer_name VARCHAR(100) NOT NULL,
    region VARCHAR(50) NOT NULL
);

-- Insert test data
INSERT INTO employees (first_name, last_name, email, salary, hire_date, department_id)
VALUES
    ('John', 'Doe', 'john.doe@example.com', 75000.00, '2020-01-15', 10),
    ('Jane', 'Smith', 'jane.smith@example.com', 82000.00, '2019-03-22', 10),
    ('Bob', 'Johnson', 'bob.johnson@example.com', 68000.00, '2021-06-10', 10),
    ('Alice', 'Williams', 'alice.williams@example.com', 91000.00, '2018-11-05', 20),
    ('Charlie', 'Brown', 'charlie.brown@example.com', 79000.00, '2020-09-18', 20);

INSERT INTO sales (product_name, sales_amount, sales_date, customer_name, region)
VALUES
    ('Product A', 1500.00, '2024-06-01', 'Customer 1', 'North'),
    ('Product B', 2300.00, '2024-06-05', 'Customer 2', 'South'),
    ('Product C', 1800.00, '2024-06-10', 'Customer 3', 'East'),
    ('Product A', 2100.00, '2024-06-15', 'Customer 4', 'West'),
    ('Product D', 3200.00, '2024-06-20', 'Customer 5', 'North');

-- Create stored procedures (H2 syntax)
CREATE ALIAS usp_GetEmployees AS $$
import java.sql.*;
@CODE
ResultSet usp_GetEmployees(Connection conn, Integer departmentId, Date startDate, int[] totalCount) throws SQLException {
    String sql = "SELECT employee_id, first_name, last_name, email, salary, hire_date, department_id " +
                 "FROM employees WHERE department_id = ?";
    if (startDate != null) {
        sql += " AND hire_date >= ?";
    }
    sql += " ORDER BY hire_date DESC";

    PreparedStatement stmt = conn.prepareStatement(sql);
    stmt.setInt(1, departmentId);
    if (startDate != null) {
        stmt.setDate(2, startDate);
    }

    ResultSet rs = stmt.executeQuery();

    // Calculate total count
    String countSql = "SELECT COUNT(*) FROM employees WHERE department_id = ?";
    if (startDate != null) {
        countSql += " AND hire_date >= ?";
    }
    PreparedStatement countStmt = conn.prepareStatement(countSql);
    countStmt.setInt(1, departmentId);
    if (startDate != null) {
        countStmt.setDate(2, startDate);
    }
    ResultSet countRs = countStmt.executeQuery();
    if (countRs.next()) {
        totalCount[0] = countRs.getInt(1);
    }
    countRs.close();
    countStmt.close();

    return rs;
}
$$;

CREATE ALIAS usp_GetMonthlySales AS $$
import java.sql.*;
import java.math.BigDecimal;
@CODE
ResultSet usp_GetMonthlySales(Connection conn, Integer year, Integer month, BigDecimal[] totalSales) throws SQLException {
    String sql = "SELECT product_name, sales_amount, sales_date, customer_name, region " +
                 "FROM sales WHERE YEAR(sales_date) = ? AND MONTH(sales_date) = ? " +
                 "ORDER BY sales_date DESC";

    PreparedStatement stmt = conn.prepareStatement(sql);
    stmt.setInt(1, year);
    stmt.setInt(2, month);

    ResultSet rs = stmt.executeQuery();

    // Calculate total sales
    String sumSql = "SELECT COALESCE(SUM(sales_amount), 0) FROM sales " +
                    "WHERE YEAR(sales_date) = ? AND MONTH(sales_date) = ?";
    PreparedStatement sumStmt = conn.prepareStatement(sumSql);
    sumStmt.setInt(1, year);
    sumStmt.setInt(2, month);
    ResultSet sumRs = sumStmt.executeQuery();
    if (sumRs.next()) {
        totalSales[0] = sumRs.getBigDecimal(1);
    }
    sumRs.close();
    sumStmt.close();

    return rs;
}
$$;

-- Insert test metadata (new JSON structure format)
INSERT INTO REPORT_METADATA (REPORT_ID, REPORT_NAME, REPORT_DESCRIPTION, METADATA_JSON)
VALUES (
    'employee-report-001',
    'Employee Report',
    'Comprehensive employee data analysis',
    '{
        "reportId": "employee-report-001",
        "reportName": "Employee Report",
        "reportDescription": "Comprehensive employee data analysis",
        "datasets": [{
            "datasource": "usp_GetEmployees",
            "datasourceId": "ds-001",
            "datasourceType": "StoredProc",
            "datasourceDescription": "Retrieves employee data with department filter",
            "parameters": [{
                "parameterName": "departmentId",
                "dataType": "INTEGER",
                "parameterDirection": "Input",
                "nullable": true
            }],
            "columns": [
                {
                    "sourceColumn": "employee_id",
                    "displayName": "employeeId",
                    "dataType": "INTEGER",
                    "sortable": true,
                    "groupable": false,
                    "filterable": true
                },
                {
                    "sourceColumn": "first_name",
                    "displayName": "firstName",
                    "dataType": "VARCHAR",
                    "sortable": true,
                    "groupable": false,
                    "filterable": true
                },
                {
                    "sourceColumn": "last_name",
                    "displayName": "lastName",
                    "dataType": "VARCHAR",
                    "sortable": true,
                    "groupable": false,
                    "filterable": true
                },
                {
                    "sourceColumn": "salary",
                    "displayName": "salary",
                    "dataType": "DECIMAL",
                    "sortable": true,
                    "groupable": true,
                    "filterable": true
                },
                {
                    "sourceColumn": "hire_date",
                    "displayName": "hireDate",
                    "dataType": "DATE",
                    "sortable": true,
                    "groupable": false,
                    "filterable": true
                }
            ]
        }]
    }'
);

INSERT INTO REPORT_METADATA (REPORT_ID, REPORT_NAME, REPORT_DESCRIPTION, METADATA_JSON)
VALUES (
    'sales-report-002',
    'Sales Report',
    'Monthly sales analysis',
    '{
        "reportId": "sales-report-002",
        "reportName": "Sales Report",
        "reportDescription": "Monthly sales analysis",
        "datasets": [{
            "datasource": "usp_GetSales",
            "datasourceId": "ds-001",
            "datasourceType": "StoredProc",
            "datasourceDescription": "Retrieves sales data",
            "parameters": [],
            "columns": [
                {
                    "sourceColumn": "sale_id",
                    "displayName": "saleId",
                    "dataType": "INTEGER",
                    "sortable": true,
                    "groupable": false,
                    "filterable": true
                },
                {
                    "sourceColumn": "product_name",
                    "displayName": "productName",
                    "dataType": "VARCHAR",
                    "sortable": true,
                    "groupable": true,
                    "filterable": true
                },
                {
                    "sourceColumn": "sales_amount",
                    "displayName": "salesAmount",
                    "dataType": "DECIMAL",
                    "sortable": true,
                    "groupable": true,
                    "filterable": true
                }
            ]
        }]
    }'
);

INSERT INTO REPORT_METADATA (REPORT_ID, REPORT_NAME, REPORT_DESCRIPTION, METADATA_JSON)
VALUES (
    'department-report-003',
    'Department Report',
    'Department summary with budget information',
    '{
        "reportId": "department-report-003",
        "reportName": "Department Report",
        "reportDescription": "Department summary with budget information",
        "datasets": [{
            "datasource": "usp_GetDepartments",
            "datasourceId": "ds-001",
            "datasourceType": "StoredProc",
            "datasourceDescription": "Retrieves department data",
            "parameters": [],
            "columns": [
                {
                    "sourceColumn": "department_id",
                    "displayName": "departmentId",
                    "dataType": "INTEGER",
                    "sortable": true,
                    "groupable": false,
                    "filterable": true
                },
                {
                    "sourceColumn": "department_name",
                    "displayName": "departmentName",
                    "dataType": "VARCHAR",
                    "sortable": true,
                    "groupable": true,
                    "filterable": true
                },
                {
                    "sourceColumn": "budget",
                    "displayName": "budget",
                    "dataType": "DECIMAL",
                    "sortable": true,
                    "groupable": false,
                    "filterable": true
                }
            ]
        }]
    }'
);
