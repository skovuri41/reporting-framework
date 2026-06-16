package com.reporting.framework.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Utility class for setting up test database.
 */
public class TestDataSetup {

    private static final Logger logger = LoggerFactory.getLogger(TestDataSetup.class);

    /**
     * Execute SQL script from classpath resource.
     */
    public static void executeSqlScript(Connection connection, String scriptPath) throws Exception {
        logger.info("Executing SQL script: {}", scriptPath);

        try (InputStream is = TestDataSetup.class.getClassLoader().getResourceAsStream(scriptPath)) {
            if (is == null) {
                throw new IllegalArgumentException("SQL script not found: " + scriptPath);
            }

            String sql = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));

            // Split by semicolon and execute each statement
            String[] statements = sql.split(";");

            try (Statement stmt = connection.createStatement()) {
                for (String sqlStatement : statements) {
                    String trimmed = sqlStatement.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                        stmt.execute(trimmed);
                    }
                }
            }

            logger.info("Successfully executed SQL script: {}", scriptPath);
        }
    }
}
