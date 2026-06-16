package com.reporting.framework.mapper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Factory for creating configured ObjectMapper instances for the reporting framework.
 */
public class ObjectMapperFactory {

    private static final ObjectMapper INSTANCE = createObjectMapper();

    /**
     * Get the singleton ObjectMapper instance.
     */
    public static ObjectMapper getInstance() {
        return INSTANCE;
    }

    /**
     * Create a new configured ObjectMapper.
     */
    public static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register JavaTimeModule for Java 8 date/time support
        mapper.registerModule(new JavaTimeModule());

        // Configure deserialization
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

        // Configure serialization
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        // Default naming strategy (can be overridden per operation)
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);

        return mapper;
    }

    /**
     * Create an ObjectMapper with snake_case naming strategy.
     * Useful for database column name mapping.
     */
    public static ObjectMapper createSnakeCaseMapper() {
        ObjectMapper mapper = createObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return mapper;
    }
}
