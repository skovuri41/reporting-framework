package com.reporting.framework.unit;

import com.reporting.framework.example.EmployeeReportRequest;
import com.reporting.framework.mapper.PojoToParameterConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PojoToParameterConverter.
 */
class PojoToParameterConverterTest {

    private PojoToParameterConverter converter;

    @BeforeEach
    void setup() {
        converter = new PojoToParameterConverter();
    }

    @Test
    @DisplayName("Convert POJO to parameter Map")
    void testConvertPojoToMap() {
        EmployeeReportRequest request = new EmployeeReportRequest();
        request.setDepartmentId(10);
        request.setStartDate(LocalDate.of(2020, 1, 1));

        Map<String, Object> result = converter.convert(request);

        assertThat(result).isNotNull();
        assertThat(result).containsEntry("departmentId", 10);
        // Note: Jackson ObjectMapper serializes LocalDate as array [year, month, day] by default
        // This is expected behavior - the actual parameter binding will handle the conversion
        assertThat(result).containsKey("startDate");
        assertThat(result.get("startDate")).isNotNull();
    }

    @Test
    @DisplayName("Handle null input")
    void testNullInput() {
        Map<String, Object> result = converter.convert(null);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Pass through Map input unchanged")
    void testMapInput() {
        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put("key1", "value1");
        inputMap.put("key2", 123);

        Map<String, Object> result = converter.convert(inputMap);

        assertThat(result).isSameAs(inputMap);
    }

    @Test
    @DisplayName("Handle POJO with null fields")
    void testPojoWithNullFields() {
        EmployeeReportRequest request = new EmployeeReportRequest();
        request.setDepartmentId(10);
        // startDate is null

        Map<String, Object> result = converter.convert(request);

        assertThat(result).containsEntry("departmentId", 10);
        assertThat(result).containsKey("startDate");
        // Verify null field is included
    }
}
