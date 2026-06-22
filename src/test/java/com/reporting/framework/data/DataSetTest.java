package com.reporting.framework.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for DataSet and DataQuery.
 */
class DataSetTest {

    private List<DataRow> sampleData;
    private DataSet dataSet;

    @BeforeEach
    void setUp() {
        sampleData = new ArrayList<>();

        sampleData.add(DataRow.builder()
                .put("employee_id", 1)
                .put("name", "Alice")
                .put("department_id", 10)
                .put("salary", BigDecimal.valueOf(80000))
                .build());

        sampleData.add(DataRow.builder()
                .put("employee_id", 2)
                .put("name", "Bob")
                .put("department_id", 20)
                .put("salary", BigDecimal.valueOf(90000))
                .build());

        sampleData.add(DataRow.builder()
                .put("employee_id", 3)
                .put("name", "Charlie")
                .put("department_id", 10)
                .put("salary", BigDecimal.valueOf(85000))
                .build());

        Map<String, Object> outputParams = new HashMap<>();
        outputParams.put("total_count", 3);

        dataSet = new DataSet(sampleData, outputParams, "test_report");
    }

    @Test
    void testBasicProperties() {
        assertThat(dataSet.count()).isEqualTo(3);
        assertThat(dataSet.isEmpty()).isFalse();
        assertThat(dataSet.getReportName()).isEqualTo("test_report");
        assertThat(dataSet.getOutputParameters()).containsEntry("total_count", 3);
    }

    @Test
    void testFilter() {
        DataSet filtered = DataQuery.from(dataSet)
                .filter(row -> row.getBigDecimal("salary").compareTo(BigDecimal.valueOf(85000)) > 0)
                .execute();

        assertThat(filtered.count()).isEqualTo(1);
        assertThat(filtered.first().getString("name")).isEqualTo("Bob");
    }

    @Test
    void testSelect() {
        DataSet selected = DataQuery.from(dataSet)
                .select("name", "salary")
                .execute();

        assertThat(selected.count()).isEqualTo(3);
        assertThat(selected.first().keys()).containsOnly("name", "salary");
    }

    @Test
    void testGroupByWithAggregations() {
        DataSet aggregated = DataQuery.from(dataSet)
                .groupBy("department_id")
                .avg("salary")
                .count("employee_id")
                .execute();

        assertThat(aggregated.count()).isEqualTo(2);

        // Department 10: Alice (80k) + Charlie (85k) = avg 82.5k
        DataRow dept10 = aggregated.getRows().stream()
                .filter(row -> row.getInt("department_id") == 10)
                .findFirst().orElseThrow();

        assertThat(dept10.getInt("department_id")).isEqualTo(10);
        assertThat(dept10.getLong("employee_id_count")).isEqualTo(2L);
        assertThat(dept10.getDouble("salary_avg")).isCloseTo(82500.0, within(0.01));

        // Department 20: Bob (90k)
        DataRow dept20 = aggregated.getRows().stream()
                .filter(row -> row.getInt("department_id") == 20)
                .findFirst().orElseThrow();

        assertThat(dept20.getLong("employee_id_count")).isEqualTo(1L);
        assertThat(dept20.getDouble("salary_avg")).isCloseTo(90000.0, within(0.01));
    }

    @Test
    void testWithColumn() {
        DataSet withBonus = DataQuery.from(dataSet)
                .withColumn("bonus", row -> row.getBigDecimal("salary").multiply(BigDecimal.valueOf(0.1)))
                .execute();

        assertThat(withBonus.count()).isEqualTo(3);
        assertThat(withBonus.first().has("bonus")).isTrue();

        BigDecimal firstSalary = sampleData.get(0).getBigDecimal("salary");
        BigDecimal expectedBonus = firstSalary.multiply(BigDecimal.valueOf(0.1));
        assertThat(withBonus.first().getBigDecimal("bonus")).isEqualTo(expectedBonus);
    }

    @Test
    void testOrderBy() {
        DataSet sorted = DataQuery.from(dataSet)
                .orderBy("salary").desc()
                .execute();

        List<DataRow> rows = sorted.getRows();
        assertThat(rows.get(0).getString("name")).isEqualTo("Bob");      // 90k
        assertThat(rows.get(1).getString("name")).isEqualTo("Charlie");  // 85k
        assertThat(rows.get(2).getString("name")).isEqualTo("Alice");    // 80k
    }

    @Test
    void testLimit() {
        DataSet limited = DataQuery.from(dataSet)
                .limit(2)
                .execute();

        assertThat(limited.count()).isEqualTo(2);
    }

    @Test
    void testDistinct() {
        // Add a duplicate row
        List<DataRow> dataWithDuplicates = new ArrayList<>(sampleData);
        dataWithDuplicates.add(sampleData.get(0));
        DataSet dataSetWithDuplicates = new DataSet(dataWithDuplicates, new HashMap<>(), "test_report");

        assertThat(dataSetWithDuplicates.count()).isEqualTo(4);

        DataSet distinct = DataQuery.from(dataSetWithDuplicates)
                .distinct()
                .execute();

        assertThat(distinct.count()).isEqualTo(3);
    }

    @Test
    void testFirst() {
        DataRow first = dataSet.first();
        assertThat(first).isNotNull();
        assertThat(first.getString("name")).isEqualTo("Alice");
    }

    @Test
    void testFirstOnEmptyDataSet() {
        DataSet empty = DataSet.empty("empty");
        assertThat(empty.first()).isNull();
    }

    @Test
    void testLast() {
        DataRow last = dataSet.last();
        assertThat(last).isNotNull();
        assertThat(last.getString("name")).isEqualTo("Charlie");
    }

    @Test
    void testGetRow() {
        DataRow row = dataSet.getRow(1);
        assertThat(row.getString("name")).isEqualTo("Bob");
    }

    @Test
    void testGetRowOutOfBounds() {
        assertThatThrownBy(() -> dataSet.getRow(10))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void testToJSON() {
        String json = dataSet.toJSON();
        assertThat(json)
                .contains("Alice")
                .contains("Bob")
                .contains("Charlie");
    }

    @Test
    void testChainedOperations() {
        // Chain multiple operations using fluent DSL
        DataSet chained = DataQuery.from(dataSet)
                .filter(row -> row.getInt("department_id") == 10)
                .select("name", "salary")
                .orderBy("salary").asc()
                .limit(1)
                .execute();

        assertThat(chained.count()).isEqualTo(1);
        assertThat(chained.first().getString("name")).isEqualTo("Alice");
        assertThat(chained.first().has("salary")).isTrue();
        assertThat(chained.first().has("employee_id")).isFalse();
    }

    @Test
    void testDataRowTypeConversions() {
        DataRow row = sampleData.get(0);

        // Test type-safe accessors
        assertThat(row.getInt("employee_id")).isEqualTo(1);
        assertThat(row.getString("name")).isEqualTo("Alice");
        assertThat(row.getInt("department_id")).isEqualTo(10);
        assertThat(row.getBigDecimal("salary")).isEqualTo(BigDecimal.valueOf(80000));
    }

    @Test
    void testDataRowWith() {
        DataRow original = sampleData.get(0);
        DataRow modified = original.with("bonus", BigDecimal.valueOf(8000));

        assertThat(modified.has("bonus")).isTrue();
        assertThat(modified.getBigDecimal("bonus")).isEqualTo(BigDecimal.valueOf(8000));
        assertThat(original.has("bonus")).isFalse(); // Original unchanged
    }

    @Test
    void testDataRowWithout() {
        DataRow original = sampleData.get(0);
        DataRow modified = original.without("salary");

        assertThat(modified.has("salary")).isFalse();
        assertThat(original.has("salary")).isTrue(); // Original unchanged
    }

    @Test
    void testDataRowSelect() {
        DataRow original = sampleData.get(0);
        DataRow selected = original.select("name", "salary");

        assertThat(selected.keys()).containsOnly("name", "salary");
    }

    @Test
    void testMultipleFilters() {
        DataSet filtered = DataQuery.from(dataSet)
                .filter(row -> row.getInt("department_id") == 10)
                .filter(row -> row.getBigDecimal("salary").compareTo(BigDecimal.valueOf(82000)) >= 0)
                .execute();

        assertThat(filtered.count()).isEqualTo(1);
        assertThat(filtered.first().getString("name")).isEqualTo("Charlie");
    }

    @Test
    void testOutputParameters() {
        assertThat(dataSet.hasOutputParameter("total_count")).isTrue();
        assertThat(dataSet.getOutputParameter("total_count")).isEqualTo(3);
        assertThat(dataSet.hasOutputParameter("nonexistent")).isFalse();
        assertThat(dataSet.getOutputParameter("nonexistent")).isNull();
    }
}
