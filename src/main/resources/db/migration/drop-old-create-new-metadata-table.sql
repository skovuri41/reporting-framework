-- ============================================================================
-- Database Migration Script: Report Metadata JSON Structure
-- ============================================================================
-- Feature: 001-metadata-json-structure
-- Purpose: Replace old REPORT_METADATA table with new JSON-based structure
-- Database: SQL Server 2016+
-- WARNING: This script DROPS the existing REPORT_METADATA table - ensure backup!
-- ============================================================================

-- Drop old metadata table
IF OBJECT_ID('dbo.REPORT_METADATA', 'U') IS NOT NULL
BEGIN
    PRINT 'Dropping old REPORT_METADATA table...';
    DROP TABLE dbo.REPORT_METADATA;
    PRINT 'Old REPORT_METADATA table dropped.';
END
GO

-- Create new metadata table with JSON structure
PRINT 'Creating new REPORT_METADATA table...';

CREATE TABLE dbo.REPORT_METADATA (
    REPORT_ID VARCHAR(100) NOT NULL,
    REPORT_NAME VARCHAR(255) NOT NULL,
    REPORT_DESCRIPTION VARCHAR(1000) NULL,
    METADATA_JSON NVARCHAR(MAX) NOT NULL,
    CREATED_DATE DATETIME2 NOT NULL DEFAULT GETDATE(),
    MODIFIED_DATE DATETIME2 NOT NULL DEFAULT GETDATE(),

    CONSTRAINT PK_REPORT_METADATA PRIMARY KEY CLUSTERED (REPORT_ID)
);
GO

-- Create index on report name for catalog queries
CREATE NONCLUSTERED INDEX IX_REPORT_METADATA_NAME
    ON dbo.REPORT_METADATA(REPORT_NAME);
GO

-- Add check constraint to ensure METADATA_JSON is valid JSON
ALTER TABLE dbo.REPORT_METADATA
    ADD CONSTRAINT CK_REPORT_METADATA_JSON
    CHECK (ISJSON(METADATA_JSON) = 1);
GO

PRINT 'New REPORT_METADATA table created successfully.';
PRINT 'Migration complete.';
PRINT '';
PRINT 'Next steps:';
PRINT '1. Insert report metadata using the new JSON structure';
PRINT '2. Verify JSON Schema compliance for all metadata';
PRINT '3. Update application to use new MetadataLoader implementation';
GO
