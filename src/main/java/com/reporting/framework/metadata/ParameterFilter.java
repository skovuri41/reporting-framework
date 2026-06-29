package com.reporting.framework.metadata;

import com.reporting.framework.exception.MissingParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Filters input parameters based on stored procedure metadata.
 * Only passes parameters that are defined in the metadata and validates required parameters.
 */
public class ParameterFilter {

    private static final Logger logger = LoggerFactory.getLogger(ParameterFilter.class);

    /**
     * Filter input parameters based on procedure metadata.
     *
     * - Validates that all required parameters are present
     * - Filters out parameters not defined in metadata
     * - Returns only parameters that match the procedure's metadata
     *
     * @param allParameters Superset of all available parameters
     * @param metadata Stored procedure metadata defining which parameters are expected
     * @return Filtered map containing only valid parameters for this procedure
     * @throws MissingParameterException if a required parameter is missing
     */
    public Map<String, Object> filterParameters(
            Map<String, Object> allParameters,
            StoredProcedureMetadata metadata) {

        if (allParameters == null) {
            allParameters = Map.of();
        }

        String procedureId = metadata.getProcedureId();
        Set<String> allParamNames = metadata.getAllParameterNames();
        Set<String> requiredParamNames = metadata.getRequiredParameterNames();

        // Validate required parameters are present
        for (String required : requiredParamNames) {
            if (!allParameters.containsKey(required)) {
                logger.error("Missing required parameter '{}' for procedure: {}", required, procedureId);
                throw new MissingParameterException(required, procedureId);
            }

            // Check for null values on required parameters
            if (allParameters.get(required) == null) {
                logger.error("Required parameter '{}' has null value for procedure: {}", required, procedureId);
                throw new MissingParameterException(required, procedureId);
            }
        }

        // Filter parameters - only include those defined in metadata
        Map<String, Object> filtered = new HashMap<>();
        int filteredCount = 0;

        for (Map.Entry<String, Object> entry : allParameters.entrySet()) {
            String paramName = entry.getKey();

            if (allParamNames.contains(paramName)) {
                filtered.put(paramName, entry.getValue());
            } else {
                // Parameter not defined in metadata - filter it out
                logger.debug("Filtering out parameter '{}' - not defined in metadata for procedure: {}",
                        paramName, procedureId);
                filteredCount++;
            }
        }

        if (filteredCount > 0) {
            logger.info("Filtered out {} parameters not defined in metadata for procedure: {}",
                    filteredCount, procedureId);
        }

        logger.debug("Passing {} parameters to procedure: {}", filtered.size(), procedureId);
        return filtered;
    }
}
