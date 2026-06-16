package com.reporting.framework.unit;

import com.reporting.framework.example.EmployeeReport;
import com.reporting.framework.mapper.JacksonPojoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for JacksonPojoMapper.
 */
class JacksonPojoMapperTest {

    private JacksonPojoMapper mapper;

    @BeforeEach
    void setup() {
        mapper = new JacksonPojoMapper();
    }

    @Test
    @DisplayName("Map single Map to POJO")
    void testMapSingleMapToPojo() {
        Map<String, Object> data = new HashMap<>();
        data.put("employeeId", 1);
        data.put("firstName", "John");
        data.put("lastName", "Doe");
        data.put("email", "john.doe@example.com");
        data.put("salary", new BigDecimal("75000.00"));
        data.put("hireDate", LocalDate.of(2020, 1, 15));
        data.put("departmentId", 10);

        EmployeeReport result = mapper.mapToPojo(data, EmployeeReport.class);

        assertThat(result).isNotNull();
        assertThat(result.getEmployeeId()).isEqualTo(1);
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");
        assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(result.getSalary()).isEqualByComparingTo(new BigDecimal("75000.00"));
        assertThat(result.getHireDate()).isEqualTo(LocalDate.of(2020, 1, 15));
        assertThat(result.getDepartmentId()).isEqualTo(10);
    }

    @Test
    @DisplayName("Map List of Maps to List of POJOs")
    void testMapListOfMapsToPojo() {
        Map<String, Object> data1 = new HashMap<>();
        data1.put("employeeId", 1);
        data1.put("firstName", "John");
        data1.put("lastName", "Doe");
        data1.put("salary", new BigDecimal("75000"));
        data1.put("hireDate", LocalDate.of(2020, 1, 15));

        Map<String, Object> data2 = new HashMap<>();
        data2.put("employeeId", 2);
        data2.put("firstName", "Jane");
        data2.put("lastName", "Smith");
        data2.put("salary", new BigDecimal("82000"));
        data2.put("hireDate", LocalDate.of(2019, 3, 22));

        List<Map<String, Object>> maps = List.of(data1, data2);

        List<EmployeeReport> results = mapper.mapToPojo(maps, EmployeeReport.class);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getFirstName()).isEqualTo("John");
        assertThat(results.get(1).getFirstName()).isEqualTo("Jane");
    }

    @Test
    @DisplayName("Handle empty list")
    void testEmptyList() {
        List<Map<String, Object>> emptyList = List.of();

        List<EmployeeReport> results = mapper.mapToPojo(emptyList, EmployeeReport.class);

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Handle null map")
    void testNullMap() {
        EmployeeReport result = mapper.mapToPojo((Map<String, Object>) null, EmployeeReport.class);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Ignore unknown properties")
    void testIgnoreUnknownProperties() {
        Map<String, Object> data = new HashMap<>();
        data.put("employeeId", 1);
        data.put("firstName", "John");
        data.put("unknownField", "This should be ignored");

        // Should not throw exception
        EmployeeReport result = mapper.mapToPojo(data, EmployeeReport.class);

        assertThat(result).isNotNull();
        assertThat(result.getEmployeeId()).isEqualTo(1);
        assertThat(result.getFirstName()).isEqualTo("John");
    }
}
