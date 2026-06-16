-- Simplified test schema for H2
-- Uses simpler approach without complex stored procedures

-- Create metadata table
CREATE TABLE REPORT_METADATA (
    REPORT_NAME VARCHAR(100) PRIMARY KEY,
    STORED_PROCEDURE VARCHAR(200) NOT NULL,
    RESULT_CLASS VARCHAR(500) NOT NULL,
    METADATA_JSON NVARCHAR(1000) NOT NULL,
    CREATED_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UPDATED_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create test tables (H2 syntax with IDENTITY)
CREATE TABLE employees (
    employee_id INT IDENTITY PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    salary DECIMAL(18,2) NOT NULL,
    hire_date DATE NOT NULL,
    department_id INT NOT NULL
);

CREATE TABLE sales (
    sale_id INT IDENTITY PRIMARY KEY,
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
