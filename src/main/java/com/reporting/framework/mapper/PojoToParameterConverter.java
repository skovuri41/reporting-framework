package com.reporting.framework.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reporting.framework.exception.ReportExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Converts POJO input objects to Map<String, Object> for parameter binding.
 * Uses Jackson ObjectMapper for bidirectional conversion.
 */
public class PojoToParameterConverter {

    private static final Logger logger = LoggerFactory.getLogger(PojoToParameterConverter.class);

    private final ObjectMapper objectMapper;

    public PojoToParameterConverter() {
        this.objectMapper = ObjectMapperFactory.getInstance();
    }

    public PojoToParameterConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Convert a POJO to a Map of parameters.
     *
     * @param inputPojo The input POJO to convert
     * @return Map of parameter name to value
     */
    public Map<String, Object> convert(Object inputPojo) {
        if (inputPojo == null) {
            return new HashMap<>();
        }

        // If already a Map, return as-is
        if (inputPojo instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) inputPojo;
            return map;
        }

        try {
            // Convert POJO to Map using ObjectMapper
            Map<String, Object> parameterMap = objectMapper.convertValue(
                    inputPojo,
                    new TypeReference<Map<String, Object>>() {}
            );

            logger.debug("Converted POJO {} to parameter map with {} entries",
                    inputPojo.getClass().getSimpleName(), parameterMap.size());

            return parameterMap;

        } catch (Exception e) {
            logger.error("Failed to convert POJO to parameter map: {}",
                    inputPojo.getClass().getName(), e);
            throw new ReportExecutionException(
                    "Failed to convert input POJO to parameter map: " + inputPojo.getClass().getName(), e);
        }
    }
}
