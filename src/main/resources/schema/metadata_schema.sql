-- ================================================================
-- Reporting Framework - Metadata Schema
-- ================================================================

-- Main metadata table storing report configuration as JSON
CREATE TABLE REPORT_METADATA (
    REPORT_NAME VARCHAR(100) PRIMARY KEY,
    STORED_PROCEDURE VARCHAR(200) NOT NULL,
    RESULT_CLASS VARCHAR(500) NOT NULL,
    METADATA_JSON NVARCHAR(MAX) NOT NULL,
    CREATED_DATE DATETIME DEFAULT GETDATE(),
    UPDATED_DATE DATETIME DEFAULT GETDATE()
);

-- Index for faster lookups
CREATE INDEX IDX_REPORT_METADATA_NAME ON REPORT_METADATA(REPORT_NAME);

-- ================================================================
-- Sample Stored Procedures for Testing
-- ================================================================

-- Example 1: Employee Report Stored Procedure
CREATE PROCEDURE dbo.usp_GetEmployees
    @DepartmentId INT,
    @StartDate DATE = NULL,
    @TotalCount INT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    -- Return employee result set
    SELECT
        employee_id,
        first_name,
        last_name,
        email,
        salary,
        hire_date,
        department_id
    FROM employees
    WHERE department_id = @DepartmentId
        AND (@StartDate IS NULL OR hire_date >= @StartDate)
    ORDER BY hire_date DESC;

    -- Set output parameter
    SELECT @TotalCount = COUNT(*)
    FROM employees
    WHERE department_id = @DepartmentId
        AND (@StartDate IS NULL OR hire_date >= @StartDate);
END;
GO

-- Example 2: Monthly Sales Report Stored Procedure
CREATE PROCEDURE dbo.usp_GetMonthlySales
    @Year INT,
    @Month INT,
    @TotalSales DECIMAL(18,2) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    -- Return sales result set
    SELECT
        product_name,
        sales_amount,
        sales_date,
        customer_name,
        region
    FROM sales
    WHERE YEAR(sales_date) = @Year
        AND MONTH(sales_date) = @Month
    ORDER BY sales_date DESC;

    -- Set output parameter
    SELECT @TotalSales = ISNULL(SUM(sales_amount), 0)
    FROM sales
    WHERE YEAR(sales_date) = @Year
        AND MONTH(sales_date) = @Month;
END;
GO

-- ================================================================
-- Sample Test Tables
-- ================================================================

-- Employees table for testing
CREATE TABLE employees (
    employee_id INT PRIMARY KEY IDENTITY(1,1),
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    salary DECIMAL(18,2) NOT NULL,
    hire_date DATE NOT NULL,
    department_id INT NOT NULL
);

-- Sales table for testing
CREATE TABLE sales (
    sale_id INT PRIMARY KEY IDENTITY(1,1),
    product_name VARCHAR(100) NOT NULL,
    sales_amount DECIMAL(18,2) NOT NULL,
    sales_date DATE NOT NULL,
    customer_name VARCHAR(100) NOT NULL,
    region VARCHAR(50) NOT NULL
);

-- ================================================================
-- Sample Metadata Inserts
-- ================================================================

-- Employee Report Metadata
INSERT INTO REPORT_METADATA (REPORT_NAME, STORED_PROCEDURE, RESULT_CLASS, METADATA_JSON)
VALUES (
    'employee_report',
    'dbo.usp_GetEmployees',
    'com.reporting.framework.example.EmployeeReport',
    '{
        "reportName": "employee_report",
        "storedProcedure": "dbo.usp_GetEmployees",
        "resultClass": "com.reporting.framework.example.EmployeeReport",
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
            "strategy": "EXPLICIT",
            "columnMappings": [
                {"column": "employee_id", "field": "employeeId", "required": true},
                {"column": "first_name", "field": "firstName", "required": true},
                {"column": "last_name", "field": "lastName", "required": true},
                {"column": "email", "field": "email", "required": true},
                {"column": "salary", "field": "salary", "required": true},
                {"column": "hire_date", "field": "hireDate", "required": true},
                {"column": "department_id", "field": "departmentId", "required": true}
            ],
            "unmappedColumnsStrategy": "IGNORE"
        }
    }'
);

-- Sales Report Metadata
INSERT INTO REPORT_METADATA (REPORT_NAME, STORED_PROCEDURE, RESULT_CLASS, METADATA_JSON)
VALUES (
    'sales_report',
    'dbo.usp_GetMonthlySales',
    'com.reporting.framework.example.SalesReport',
    '{
        "reportName": "sales_report",
        "storedProcedure": "dbo.usp_GetMonthlySales",
        "resultClass": "com.reporting.framework.example.SalesReport",
        "inputParameters": [
            {
                "name": "Year",
                "sqlType": "INTEGER",
                "javaType": "java.lang.Integer",
                "required": true
            },
            {
                "name": "Month",
                "sqlType": "INTEGER",
                "javaType": "java.lang.Integer",
                "required": true
            }
        ],
        "outputParameters": [
            {
                "name": "TotalSales",
                "sqlType": "DECIMAL",
                "javaType": "java.math.BigDecimal"
            }
        ],
        "resultSetMapping": {
            "strategy": "EXPLICIT",
            "columnMappings": [
                {"column": "product_name", "field": "productName", "required": true},
                {"column": "sales_amount", "field": "salesAmount", "required": true},
                {"column": "sales_date", "field": "salesDate", "required": true},
                {"column": "customer_name", "field": "customerName", "required": true},
                {"column": "region", "field": "region", "required": true}
            ],
            "unmappedColumnsStrategy": "IGNORE"
        }
    }'
);

-- ================================================================
-- Sample Test Data
-- ================================================================

-- Insert sample employees
INSERT INTO employees (first_name, last_name, email, salary, hire_date, department_id)
VALUES
    ('John', 'Doe', 'john.doe@example.com', 75000.00, '2020-01-15', 10),
    ('Jane', 'Smith', 'jane.smith@example.com', 82000.00, '2019-03-22', 10),
    ('Bob', 'Johnson', 'bob.johnson@example.com', 68000.00, '2021-06-10', 10),
    ('Alice', 'Williams', 'alice.williams@example.com', 91000.00, '2018-11-05', 20),
    ('Charlie', 'Brown', 'charlie.brown@example.com', 79000.00, '2020-09-18', 20);

-- Insert sample sales
INSERT INTO sales (product_name, sales_amount, sales_date, customer_name, region)
VALUES
    ('Product A', 1500.00, '2024-06-01', 'Customer 1', 'North'),
    ('Product B', 2300.00, '2024-06-05', 'Customer 2', 'South'),
    ('Product C', 1800.00, '2024-06-10', 'Customer 3', 'East'),
    ('Product A', 2100.00, '2024-06-15', 'Customer 4', 'West'),
    ('Product D', 3200.00, '2024-06-20', 'Customer 5', 'North');
