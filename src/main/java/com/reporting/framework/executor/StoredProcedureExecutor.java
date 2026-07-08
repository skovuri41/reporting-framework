package com.reporting.framework.executor;

import com.reporting.framework.connection.ConnectionProvider;
import com.reporting.framework.exception.ReportExecutionException;
import com.reporting.framework.metadata.model.Dataset;
import com.reporting.framework.metadata.model.Parameter;
import com.reporting.framework.metadata.model.ParameterDirection;
// SqlTypeMapper not needed - GenericParameterBinder handles type conversion
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Executes stored procedures using CallableStatement.
 * <p>
 * Updated to work with new Dataset metadata model from Phase 2.
 * Handles parameter binding based on ParameterDirection (Input/Output)
 * and extracts output parameter values after execution.
 * </p>
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
     * Execute a stored procedure defined in a dataset.
     *
     * @param dataset The dataset containing stored procedure metadata
     * @param inputParameters The input parameter values (name → value)
     * @return ExecutionResult containing ResultSet and output parameters
     */
    public ExecutionResult execute(Dataset dataset, Map<String, Object> inputParameters) {
        String datasource = dataset.getDatasource();
        String datasourceId = dataset.getDatasourceId();

        logger.info("Executing stored procedure: {} (Dataset ID: {})", datasource, datasourceId);

        Connection connection = null;
        CallableStatement statement = null;
        ResultSet resultSet = null;

        try {
            // Get database connection
            connection = connectionProvider.getConnection();

            // Separate input and output parameters
            List<Parameter> inputParams = dataset.getParameters().stream()
                .filter(p -> p.getParameterDirection() == ParameterDirection.INPUT)
                .collect(Collectors.toList());

            List<Parameter> outputParams = dataset.getParameters().stream()
                .filter(p -> p.getParameterDirection() == ParameterDirection.OUTPUT)
                .collect(Collectors.toList());

            // Build CallableStatement SQL
            String sql = buildCallableStatementSql(datasource, inputParams.size() + outputParams.size());
            logger.debug("CallableStatement SQL: {}", sql);

            // Prepare CallableStatement
            statement = connection.prepareCall(sql);

            // Bind input parameters and register output parameters
            int paramIndex = 1;
            Map<String, Integer> outputParamIndices = new HashMap<>();

            // Process input parameters
            for (Parameter param : inputParams) {
                Object value = inputParameters.get(param.getParameterName());

                // Validate required (non-nullable) parameters
                if (!param.getNullable() && value == null) {
                    throw new ReportExecutionException(
                        "Required parameter '" + param.getParameterName() + "' is missing for dataset '" +
                        datasourceId + "'"
                    );
                }

                parameterBinder.bindInputParameter(
                    statement, param.getParameterName(), value, param.getDataType(), paramIndex);
                paramIndex++;
            }

            // Register output parameters
            for (Parameter param : outputParams) {
                parameterBinder.registerOutputParameter(
                    statement, param.getParameterName(), param.getDataType(), paramIndex);
                outputParamIndices.put(param.getParameterName(), paramIndex);
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
            for (Parameter param : outputParams) {
                int index = outputParamIndices.get(param.getParameterName());

                Object value = parameterBinder.getOutputParameter(
                    statement, param.getParameterName(), param.getDataType(), index);
                outputParameters.put(param.getParameterName(), value);
            }

            logger.info("Successfully executed stored procedure: {} with {} output parameters",
                datasource, outputParameters.size());

            // Note: Do NOT close connection, statement, or resultSet here
            // They will be closed by the caller after processing the ResultSet
            return new ExecutionResult(resultSet, outputParameters);

        } catch (SQLException e) {
            // Clean up on error
            closeQuietly(resultSet);
            closeQuietly(statement);
            closeQuietly(connection);

            logger.error("Failed to execute stored procedure: {} (Dataset ID: {})",
                datasource, datasourceId, e);
            throw new ReportExecutionException(
                "Stored procedure execution failed for dataset '" + datasourceId +
                "': " + e.getMessage(), e);

        } catch (Exception e) {
            // Clean up on error
            closeQuietly(resultSet);
            closeQuietly(statement);
            closeQuietly(connection);

            logger.error("Unexpected error executing stored procedure: {} (Dataset ID: {})",
                datasource, datasourceId, e);
            throw new ReportExecutionException(
                "Unexpected error for dataset '" + datasourceId + "': " + e.getMessage(), e);
        }
    }

    /**
     * Build the CallableStatement SQL string.
     * Format: {call dbo.procedureName(?, ?, ?)}
     */
    private String buildCallableStatementSql(String procedureName, int totalParams) {
        if (totalParams == 0) {
            return "{call " + procedureName + "}";
        }

        StringBuilder sql = new StringBuilder("{call ");
        sql.append(procedureName);
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

    // getSqlType() removed - GenericParameterBinder handles SQL type conversion internally

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
