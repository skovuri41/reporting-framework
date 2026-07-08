-- ============================================================================
-- Sample Report Metadata INSERT Scripts
-- ============================================================================
-- Purpose: Example metadata for common reporting scenarios
-- Database: SQL Server 2016+
-- Note: These are examples - customize for your stored procedures
-- ============================================================================

-- Example 1: Employee Report with Input Parameters
-- Demonstrates: Input parameters, multiple columns, naming transformations
INSERT INTO dbo.REPORT_METADATA (REPORT_ID, REPORT_NAME, REPORT_DESCRIPTION, METADATA_JSON)
VALUES ('employee-report', 'Employee Directory', 'Lists employees with department filter',
N'{
  "reportId": "employee-report",
  "reportName": "Employee Directory",
  "reportDescription": "Lists employees with optional department and salary filters",
  "datasets": [
    {
      "datasource": "dbo.usp_GetEmployees",
      "datasourceId": "ds-employees",
      "datasourceType": "StoredProc",
      "datasourceDescription": "Fetches employee data from HR system",
      "parameters": [
        {
          "parameterName": "department_id",
          "dataType": "INTEGER",
          "parameterDirection": "Input",
          "nullable": true
        },
        {
          "parameterName": "min_salary",
          "dataType": "DECIMAL",
          "parameterDirection": "Input",
          "nullable": true
        }
      ],
      "columns": [
        {
          "sourceColumn": "employee_id",
          "displayName": "employee_id",
          "dataType": "INTEGER",
          "sortable": true,
          "groupable": false,
          "filterable": true
        },
        {
          "sourceColumn": "full_name",
          "displayName": "full_name",
          "dataType": "VARCHAR",
          "sortable": true,
          "groupable": false,
          "filterable": true
        },
        {
          "sourceColumn": "email_address",
          "displayName": "email_address",
          "dataType": "VARCHAR",
          "sortable": true,
          "groupable": false,
          "filterable": true
        },
        {
          "sourceColumn": "department_name",
          "displayName": "department_name",
          "dataType": "VARCHAR",
          "sortable": true,
          "groupable": true,
          "filterable": true
        },
        {
          "sourceColumn": "salary",
          "displayName": "salary",
          "dataType": "DECIMAL",
          "sortable": true,
          "groupable": false,
          "filterable": true
        },
        {
          "sourceColumn": "hire_date",
          "displayName": "hire_date",
          "dataType": "DATE",
          "sortable": true,
          "groupable": false,
          "filterable": true
        }
      ]
    }
  ]
}');
GO

-- Example 2: Sales Summary Report with Output Parameters
-- Demonstrates: Output parameters, aggregation columns
INSERT INTO dbo.REPORT_METADATA (REPORT_ID, REPORT_NAME, REPORT_DESCRIPTION, METADATA_JSON)
VALUES ('sales-summary', 'Sales Summary Report', 'Monthly sales summary with totals',
N'{
  "reportId": "sales-summary",
  "reportName": "Sales Summary Report",
  "reportDescription": "Aggregated sales data by month with total count",
  "datasets": [
    {
      "datasource": "dbo.usp_GetSalesSummary",
      "datasourceId": "ds-sales",
      "datasourceType": "StoredProc",
      "datasourceDescription": "Aggregates sales data by month",
      "parameters": [
        {
          "parameterName": "start_date",
          "dataType": "DATE",
          "parameterDirection": "Input",
          "nullable": false
        },
        {
          "parameterName": "end_date",
          "dataType": "DATE",
          "parameterDirection": "Input",
          "nullable": false
        },
        {
          "parameterName": "total_sales_count",
          "dataType": "INTEGER",
          "parameterDirection": "Output",
          "nullable": false
        },
        {
          "parameterName": "total_revenue",
          "dataType": "DECIMAL",
          "parameterDirection": "Output",
          "nullable": false
        }
      ],
      "columns": [
        {
          "sourceColumn": "sale_month",
          "displayName": "sale_month",
          "dataType": "VARCHAR",
          "sortable": true,
          "groupable": true,
          "filterable": true
        },
        {
          "sourceColumn": "total_amount",
          "displayName": "total_amount",
          "dataType": "DECIMAL",
          "sortable": true,
          "groupable": false,
          "filterable": false
        },
        {
          "sourceColumn": "sale_count",
          "displayName": "sale_count",
          "dataType": "INTEGER",
          "sortable": true,
          "groupable": false,
          "filterable": false
        },
        {
          "sourceColumn": "avg_sale_amount",
          "displayName": "avg_sale_amount",
          "dataType": "DECIMAL",
          "sortable": true,
          "groupable": false,
          "filterable": false
        }
      ]
    }
  ]
}');
GO

-- Example 3: Simple Report with No Parameters
-- Demonstrates: No parameters, basic column structure
INSERT INTO dbo.REPORT_METADATA (REPORT_ID, REPORT_NAME, REPORT_DESCRIPTION, METADATA_JSON)
VALUES ('department-list', 'Department List', 'All active departments',
N'{
  "reportId": "department-list",
  "reportName": "Department List",
  "reportDescription": "Lists all active departments with budget information",
  "datasets": [
    {
      "datasource": "dbo.usp_GetDepartments",
      "datasourceId": "ds-departments",
      "datasourceType": "StoredProc",
      "datasourceDescription": "Fetches all active departments",
      "parameters": [],
      "columns": [
        {
          "sourceColumn": "department_id",
          "displayName": "department_id",
          "dataType": "INTEGER",
          "sortable": true,
          "groupable": false,
          "filterable": true
        },
        {
          "sourceColumn": "department_name",
          "displayName": "department_name",
          "dataType": "VARCHAR",
          "sortable": true,
          "groupable": false,
          "filterable": true
        },
        {
          "sourceColumn": "budget",
          "displayName": "budget",
          "dataType": "DECIMAL",
          "sortable": true,
          "groupable": false,
          "filterable": true
        },
        {
          "sourceColumn": "employee_count",
          "displayName": "employee_count",
          "dataType": "INTEGER",
          "sortable": true,
          "groupable": false,
          "filterable": true
        }
      ]
    }
  ]
}');
GO

-- Example 4: Report with Various Naming Conventions
-- Demonstrates: Automatic naming transformation (snake_case, PascalCase, UPPER_SNAKE_CASE)
INSERT INTO dbo.REPORT_METADATA (REPORT_ID, REPORT_NAME, REPORT_DESCRIPTION, METADATA_JSON)
VALUES ('naming-demo', 'Naming Transformation Demo', 'Demonstrates automatic camelCase transformation',
N'{
  "reportId": "naming-demo",
  "reportName": "Naming Transformation Demo",
  "reportDescription": "Shows how various naming conventions are automatically transformed to camelCase",
  "datasets": [
    {
      "datasource": "dbo.usp_NamingDemo",
      "datasourceId": "ds-naming",
      "datasourceType": "StoredProc",
      "datasourceDescription": "Demo dataset for naming transformations",
      "parameters": [
        {
          "parameterName": "user_id",
          "dataType": "INTEGER",
          "parameterDirection": "Input",
          "nullable": true
        },
        {
          "parameterName": "FILTER_TYPE",
          "dataType": "VARCHAR",
          "parameterDirection": "Input",
          "nullable": true
        },
        {
          "parameterName": "StartDate",
          "dataType": "DATE",
          "parameterDirection": "Input",
          "nullable": true
        }
      ],
      "columns": [
        {
          "sourceColumn": "customer_id",
          "displayName": "customer_id",
          "dataType": "INTEGER",
          "sortable": true,
          "groupable": false,
          "filterable": true
        },
        {
          "sourceColumn": "FIRST_NAME",
          "displayName": "FIRST_NAME",
          "dataType": "VARCHAR",
          "sortable": true,
          "groupable": false,
          "filterable": true
        },
        {
          "sourceColumn": "LastName",
          "displayName": "LastName",
          "dataType": "VARCHAR",
          "sortable": true,
          "groupable": false,
          "filterable": true
        }
      ]
    }
  ]
}');
GO

PRINT 'Sample metadata inserted successfully.';
PRINT '';
PRINT 'Reports created:';
PRINT '  - employee-report (with input parameters)';
PRINT '  - sales-summary (with output parameters)';
PRINT '  - department-list (no parameters)';
PRINT '  - naming-demo (demonstrates naming transformations)';
PRINT '';
PRINT 'Note: These samples reference stored procedures that must exist in your database:';
PRINT '  - dbo.usp_GetEmployees';
PRINT '  - dbo.usp_GetSalesSummary';
PRINT '  - dbo.usp_GetDepartments';
PRINT '  - dbo.usp_NamingDemo';
GO
