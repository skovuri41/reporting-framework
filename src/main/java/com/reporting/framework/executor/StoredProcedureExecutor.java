package com.reporting.framework.executor;

import com.reporting.framework.connection.ConnectionProvider;
import com.reporting.framework.exception.ReportExecutionException;
import com.reporting.framework.metadata.ParameterMetadata;
import com.reporting.framework.metadata.ReportMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes stored procedures using CallableStatement.
 * Handles parameter binding and output parameter extraction.
 */
public class StoredProcedureExecutor {

    private static final Logger logger = LoggerFactory.getLogger(StoredProcedureExecutor.class);

    private final ConnectionProvider connectionProvider;
    private final GenericParameterBinder parameterBinder;

    public StoredProcedureExecutor(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
        this.parameterBinder = new GenericParameterBinder();
    }

    /**
     * Execute a stored procedure and return results.
     *
     * @param metadata The report metadata
     * @param inputParameters The input parameter values
     * @return ExecutionResult containing ResultSet and output parameters
     */
    public ExecutionResult execute(ReportMetadata metadata, Map<String, Object> inputParameters) {
        String reportName = metadata.getReportName();
        String storedProcedure = metadata.getStoredProcedure();

        logger.info("Executing stored procedure: {} for report: {}", storedProcedure, reportName);

        Connection connection = null;
        CallableStatement statement = null;
        ResultSet resultSet = null;

        try {
            // Get database connection
            connection = connectionProvider.getConnection();

            // Build CallableStatement SQL
            String sql = buildCallableStatementSql(metadata);
            logger.debug("CallableStatement SQL: {}", sql);

            // Prepare CallableStatement
            statement = connection.prepareCall(sql);

            // Bind input parameters and register output parameters
            int paramIndex = 1;
            Map<String, Integer> outputParamIndices = new HashMap<>();

            // Process input parameters
            for (ParameterMetadata param : metadata.getInputParameters()) {
                Object value = inputParameters.get(param.getName());

                // Validate required parameters
                if (param.isRequired() && value == null) {
                    throw new ReportExecutionException(reportName, storedProcedure,
                            "Required parameter is missing: " + param.getName());
                }

                parameterBinder.bindInputParameter(
                        statement, param.getName(), value, param.getSqlType(), paramIndex);
                paramIndex++;
            }

            // Register output parameters
            for (ParameterMetadata param : metadata.getOutputParameters()) {
                parameterBinder.registerOutputParameter(
                        statement, param.getName(), param.getSqlType(), paramIndex);
                outputParamIndices.put(param.getName(), paramIndex);
                paramIndex++;
            }

            // Execute stored procedure
            boolean hasResultSet = statement.execute();
            logger.debug("Stored procedure executed, hasResultSet: {}", hasResultSet);

            // Get ResultSet if available
            if (hasResultSet) {
                resultSet = statement.getResultSet();
            }

            // Extract output parameters
            Map<String, Object> outputParameters = new HashMap<>();
            for (ParameterMetadata param : metadata.getOutputParameters()) {
                int index = outputParamIndices.get(param.getName());
                Object value = parameterBinder.getOutputParameter(
                        statement, param.getName(), param.getSqlType(), index);
                outputParameters.put(param.getName(), value);
            }

            logger.info("Successfully executed stored procedure: {} with {} output parameters",
                    storedProcedure, outputParameters.size());

            // Note: Do NOT close connection, statement, or resultSet here
            // They will be closed by the caller after processing the ResultSet
            return new ExecutionResult(resultSet, outputParameters);

        } catch (SQLException e) {
            // Clean up on error
            closeQuietly(resultSet);
            closeQuietly(statement);
            closeQuietly(connection);

            logger.error("Failed to execute stored procedure: {} for report: {}",
                    storedProcedure, reportName, e);
            throw new ReportExecutionException(reportName, storedProcedure,
                    "Stored procedure execution failed: " + e.getMessage(), e);
        } catch (Exception e) {
            // Clean up on error
            closeQuietly(resultSet);
            closeQuietly(statement);
            closeQuietly(connection);

            logger.error("Unexpected error executing stored procedure: {} for report: {}",
                    storedProcedure, reportName, e);
            throw new ReportExecutionException(reportName, storedProcedure,
                    "Unexpected error: " + e.getMessage(), e);
        }
    }

    /**
     * Build the CallableStatement SQL string.
     * Format: {call dbo.procedureName(?, ?, ?)}
     */
    private String buildCallableStatementSql(ReportMetadata metadata) {
        int totalParams = metadata.getInputParameters().size() +
                metadata.getOutputParameters().size();

        if (totalParams == 0) {
            return "{call " + metadata.getStoredProcedure() + "}";
        }

        StringBuilder sql = new StringBuilder("{call ");
        sql.append(metadata.getStoredProcedure());
        sql.append("(");

        for (int i = 0; i < totalParams; i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("?");
        }

        sql.append(")}");
        return sql.toString();
    }

    /**
     * Close a resource quietly without throwing exceptions.
     */
    private void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                logger.warn("Error closing resource", e);
            }
        }
    }

    /**
     * Close execution resources (Connection, Statement, ResultSet).
     * Should be called by the caller after processing the ResultSet.
     */
    public void closeResources(Connection connection, CallableStatement statement, ResultSet resultSet) {
        closeQuietly(resultSet);
        closeQuietly(statement);
        closeQuietly(connection);
    }
}
