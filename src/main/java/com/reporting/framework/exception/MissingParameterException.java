package com.reporting.framework.exception;

/**
 * Exception thrown when a required parameter is missing.
 */
public class MissingParameterException extends RuntimeException {

    private final String parameterName;
    private final String procedureId;

    public MissingParameterException(String parameterName, String procedureId) {
        super(String.format("Required parameter '%s' is missing for procedure: %s", parameterName, procedureId));
        this.parameterName = parameterName;
        this.procedureId = procedureId;
    }

    public String getParameterName() {
        return parameterName;
    }

    public String getProcedureId() {
        return procedureId;
    }
}
