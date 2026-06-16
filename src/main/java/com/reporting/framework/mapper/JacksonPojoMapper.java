package com.reporting.framework.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reporting.framework.exception.ReportExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Converts Maps to POJOs using Jackson ObjectMapper.
 * Uses ObjectMapper.convertValue() for efficient direct conversion without JSON serialization.
 */
public class JacksonPojoMapper {

    private static final Logger logger = LoggerFactory.getLogger(JacksonPojoMapper.class);

    private final ObjectMapper objectMapper;

    public JacksonPojoMapper() {
        this.objectMapper = ObjectMapperFactory.getInstance();
    }

    public JacksonPojoMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Convert a list of Maps to a list of POJOs.
     *
     * @param maps The list of maps to convert
     * @param pojoClass The target POJO class
     * @param <T> The POJO type
     * @return List of POJO instances
     */
    public <T> List<T> mapToPojo(List<Map<String, Object>> maps, Class<T> pojoClass) {
        if (maps == null || maps.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            List<T> results = new ArrayList<>(maps.size());

            for (Map<String, Object> map : maps) {
                T pojo = objectMapper.convertValue(map, pojoClass);
                results.add(pojo);
            }

            logger.debug("Converted {} maps to {} instances", maps.size(), pojoClass.getSimpleName());
            return results;

        } catch (Exception e) {
            logger.error("Failed to convert maps to POJO class: {}", pojoClass.getName(), e);
            throw new ReportExecutionException(
                    "Failed to map result set to POJO class: " + pojoClass.getName(), e);
        }
    }

    /**
     * Convert a single Map to a POJO.
     *
     * @param map The map to convert
     * @param pojoClass The target POJO class
     * @param <T> The POJO type
     * @return POJO instance
     */
    public <T> T mapToPojo(Map<String, Object> map, Class<T> pojoClass) {
        if (map == null) {
            return null;
        }

        try {
            return objectMapper.convertValue(map, pojoClass);
        } catch (Exception e) {
            logger.error("Failed to convert map to POJO class: {}", pojoClass.getName(), e);
            throw new ReportExecutionException(
                    "Failed to map result to POJO class: " + pojoClass.getName(), e);
        }
    }
}
