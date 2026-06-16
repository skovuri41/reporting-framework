package com.reporting.framework.executor;

import java.sql.ResultSet;
import java.util.Collections;
import java.util.Map;

/**
 * Wrapper for stored procedure execution results.
 * Contains both the ResultSet and output parameters.
 */
public class ExecutionResult {

    private final ResultSet resultSet;
    private final Map<String, Object> outputParameters;

    public ExecutionResult(ResultSet resultSet, Map<String, Object> outputParameters) {
        this.resultSet = resultSet;
        this.outputParameters = outputParameters != null ? outputParameters : Collections.emptyMap();
    }

    public ResultSet getResultSet() {
        return resultSet;
    }

    public Map<String, Object> getOutputParameters() {
        return outputParameters;
    }

    public boolean hasResultSet() {
        return resultSet != null;
    }
}
