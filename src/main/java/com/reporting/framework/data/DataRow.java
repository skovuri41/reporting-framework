package com.reporting.framework.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Immutable wrapper over Map<String, Object> representing a single row of data.
 * Provides type-safe accessors with null handling for common data types.
 *
 * This class makes working with dynamic data safer and more convenient than
 * directly accessing Map<String, Object>.
 */
public class DataRow {

    private final Map<String, Object> data;

    /**
     * Create a DataRow from a map.
     *
     * @param data The underlying data map
     */
    public DataRow(Map<String, Object> data) {
        this.data = data != null ? new HashMap<>(data) : new HashMap<>();
    }

    /**
     * Get a value by key.
     *
     * @param key The column name
     * @return The value, or null if not found
     */
    public Object get(String key) {
        return data.get(key);
    }

    /**
     * Get a String value.
     *
     * @param key The column name
     * @return The string value, or null if not found or not a string
     */
    public String getString(String key) {
        Object value = data.get(key);
        return value != null ? String.valueOf(value) : null;
    }

    /**
     * Get a String value with a default.
     *
     * @param key The column name
     * @param defaultValue The default value if not found
     * @return The string value or default
     */
    public String getString(String key, String defaultValue) {
        String value = getString(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Get an Integer value.
     *
     * @param key The column name
     * @return The integer value, or null if not found or not convertible
     */
    public Integer getInt(String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Get an Integer value with a default.
     *
     * @param key The column name
     * @param defaultValue The default value if not found
     * @return The integer value or default
     */
    public Integer getInt(String key, Integer defaultValue) {
        Integer value = getInt(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Get a Long value.
     *
     * @param key The column name
     * @return The long value, or null if not found or not convertible
     */
    public Long getLong(String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Get a Long value with a default.
     *
     * @param key The column name
     * @param defaultValue The default value if not found
     * @return The long value or default
     */
    public Long getLong(String key, Long defaultValue) {
        Long value = getLong(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Get a Double value.
     *
     * @param key The column name
     * @return The double value, or null if not found or not convertible
     */
    public Double getDouble(String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Get a Double value with a default.
     *
     * @param key The column name
     * @param defaultValue The default value if not found
     * @return The double value or default
     */
    public Double getDouble(String key, Double defaultValue) {
        Double value = getDouble(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Get a BigDecimal value.
     *
     * @param key The column name
     * @return The BigDecimal value, or null if not found
     */
    public BigDecimal getBigDecimal(String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        if (value instanceof String) {
            try {
                return new BigDecimal((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Get a BigDecimal value with a default.
     *
     * @param key The column name
     * @param defaultValue The default value if not found
     * @return The BigDecimal value or default
     */
    public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
        BigDecimal value = getBigDecimal(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Get a Boolean value.
     *
     * @param key The column name
     * @return The boolean value, or null if not found
     */
    public Boolean getBoolean(String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return null;
    }

    /**
     * Get a Boolean value with a default.
     *
     * @param key The column name
     * @param defaultValue The default value if not found
     * @return The boolean value or default
     */
    public Boolean getBoolean(String key, Boolean defaultValue) {
        Boolean value = getBoolean(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Get a LocalDate value.
     *
     * @param key The column name
     * @return The LocalDate value, or null if not found
     */
    public LocalDate getLocalDate(String key) {
        Object value = data.get(key);
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        return null;
    }

    /**
     * Get a LocalDateTime value.
     *
     * @param key The column name
     * @return The LocalDateTime value, or null if not found
     */
    public LocalDateTime getLocalDateTime(String key) {
        Object value = data.get(key);
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        return null;
    }

    /**
     * Get a LocalTime value.
     *
     * @param key The column name
     * @return The LocalTime value, or null if not found
     */
    public LocalTime getLocalTime(String key) {
        Object value = data.get(key);
        if (value instanceof LocalTime) {
            return (LocalTime) value;
        }
        return null;
    }

    /**
     * Check if a key exists in the row.
     *
     * @param key The column name
     * @return true if the key exists
     */
    public boolean has(String key) {
        return data.containsKey(key);
    }

    /**
     * Get all column names (keys).
     *
     * @return Set of column names
     */
    public Set<String> keys() {
        return new HashSet<>(data.keySet());
    }

    /**
     * Get the underlying map (defensive copy).
     *
     * @return Copy of the underlying data map
     */
    public Map<String, Object> toMap() {
        return new HashMap<>(data);
    }

    /**
     * Get the number of columns in this row.
     *
     * @return The number of columns
     */
    public int size() {
        return data.size();
    }

    /**
     * Check if this row is empty.
     *
     * @return true if the row has no columns
     */
    public boolean isEmpty() {
        return data.isEmpty();
    }

    /**
     * Create a new DataRow with an additional column.
     *
     * @param key The column name
     * @param value The column value
     * @return A new DataRow with the additional column
     */
    public DataRow with(String key, Object value) {
        Map<String, Object> newData = new HashMap<>(data);
        newData.put(key, value);
        return new DataRow(newData);
    }

    /**
     * Create a new DataRow without a specific column.
     *
     * @param key The column name to remove
     * @return A new DataRow without the specified column
     */
    public DataRow without(String key) {
        Map<String, Object> newData = new HashMap<>(data);
        newData.remove(key);
        return new DataRow(newData);
    }

    /**
     * Create a new DataRow with only the specified columns.
     *
     * @param keys The column names to keep
     * @return A new DataRow with only the specified columns
     */
    public DataRow select(String... keys) {
        Map<String, Object> newData = new HashMap<>();
        for (String key : keys) {
            if (data.containsKey(key)) {
                newData.put(key, data.get(key));
            }
        }
        return new DataRow(newData);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataRow dataRow = (DataRow) o;
        return Objects.equals(data, dataRow.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }

    @Override
    public String toString() {
        return "DataRow" + data;
    }

    /**
     * Builder for creating DataRow instances.
     */
    public static class Builder {
        private final Map<String, Object> data = new HashMap<>();

        public Builder put(String key, Object value) {
            data.put(key, value);
            return this;
        }

        public Builder putAll(Map<String, Object> values) {
            data.putAll(values);
            return this;
        }

        public DataRow build() {
            return new DataRow(data);
        }
    }

    /**
     * Create a new Builder.
     *
     * @return A new DataRow.Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a DataRow from a map.
     *
     * @param data The data map
     * @return A new DataRow
     */
    public static DataRow of(Map<String, Object> data) {
        return new DataRow(data);
    }

    /**
     * Create an empty DataRow.
     *
     * @return An empty DataRow
     */
    public static DataRow empty() {
        return new DataRow(Collections.emptyMap());
    }
}
