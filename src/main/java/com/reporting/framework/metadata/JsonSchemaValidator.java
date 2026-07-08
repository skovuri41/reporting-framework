package com.reporting.framework.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.reporting.framework.exception.MetadataValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validator for report metadata JSON against the JSON Schema.
 * <p>
 * Loads the metadata JSON Schema from the classpath and provides validation
 * methods to ensure metadata conforms to the expected structure before
 * deserialization.
 * </p>
 * <p>
 * Validation occurs at metadata load time to fail fast on invalid metadata,
 * preventing runtime errors due to malformed data.
 * </p>
 */
public class JsonSchemaValidator {

    private static final Logger logger = LoggerFactory.getLogger(JsonSchemaValidator.class);
    private static final String SCHEMA_PATH = "/schema/metadata-schema.json";

    private final JsonSchema schema;
    private final ObjectMapper objectMapper;

    /**
     * Constructs a JsonSchemaValidator by loading the schema from classpath.
     *
     * @throws IllegalStateException if the schema file cannot be loaded
     */
    public JsonSchemaValidator() {
        this.objectMapper = new ObjectMapper();
        this.schema = loadSchema();
        logger.info("JSON Schema validator initialized from {}", SCHEMA_PATH);
    }

    /**
     * Validates a JSON string against the metadata schema.
     * <p>
     * If validation fails, throws MetadataValidationException with all
     * validation error messages.
     * </p>
     *
     * @param reportId the report ID (for error context)
     * @param json the JSON string to validate
     * @throws MetadataValidationException if validation fails
     */
    public void validate(String reportId, String json) {
        try {
            JsonNode jsonNode = objectMapper.readTree(json);
            Set<ValidationMessage> errors = schema.validate(jsonNode);

            if (!errors.isEmpty()) {
                List<String> errorMessages = errors.stream()
                    .map(ValidationMessage::getMessage)
                    .collect(Collectors.toList());

                logger.error("Metadata validation failed for reportId='{}': {}", reportId, errorMessages);
                throw new MetadataValidationException(reportId, errorMessages);
            }

            logger.debug("Metadata validation successful for reportId='{}'", reportId);

        } catch (MetadataValidationException e) {
            throw e; // Re-throw validation exceptions as-is
        } catch (Exception e) {
            logger.error("Error during JSON schema validation for reportId='{}'", reportId, e);
            throw new MetadataValidationException(
                reportId,
                "Failed to parse or validate JSON: " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Loads the JSON Schema from the classpath.
     *
     * @return the loaded JsonSchema instance
     * @throws IllegalStateException if schema cannot be loaded
     */
    private JsonSchema loadSchema() {
        try (InputStream schemaStream = getClass().getResourceAsStream(SCHEMA_PATH)) {
            if (schemaStream == null) {
                throw new IllegalStateException(
                    "JSON Schema not found at classpath location: " + SCHEMA_PATH
                );
            }

            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
            JsonNode schemaNode = objectMapper.readTree(schemaStream);

            return factory.getSchema(schemaNode);

        } catch (Exception e) {
            throw new IllegalStateException("Failed to load JSON Schema from " + SCHEMA_PATH, e);
        }
    }
}
