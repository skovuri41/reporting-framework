package com.reporting.framework.api;

import com.reporting.framework.api.DynamicReportResult.AggregationFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for DynamicReportResult.
 */
class DynamicReportResultTest {

    private List<Map<String, Object>> sampleData;
    private DynamicReportResult result;

    @BeforeEach
    void setUp() {
        sampleData = new ArrayList<>();

        Map<String, Object> row1 = new HashMap<>();
        row1.put("employee_id", 1);
        row1.put("name", "Alice");
        row1.put("department_id", 10);
        row1.put("salary", BigDecimal.valueOf(80000));
        sampleData.add(row1);

        Map<String, Object> row2 = new HashMap<>();
        row2.put("employee_id", 2);
        row2.put("name", "Bob");
        row2.put("department_id", 20);
        row2.put("salary", BigDecimal.valueOf(90000));
        sampleData.add(row2);

        Map<String, Object> row3 = new HashMap<>();
        row3.put("employee_id", 3);
        row3.put("name", "Charlie");
        row3.put("department_id", 10);
        row3.put("salary", BigDecimal.valueOf(85000));
        sampleData.add(row3);

        Map<String, Object> outputParams = new HashMap<>();
        outputParams.put("total_count", 3);

        result = new DynamicReportResult(sampleData, outputParams, "test_report");
    }

    @Test
    void testBasicProperties() {
        assertThat(result.count()).isEqualTo(3);
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.getReportName()).isEqualTo("test_report");
        assertThat(result.getOutputParameters()).containsEntry("total_count", 3);
    }

    @Test
    void testFilter() {
        DynamicReportResult filtered = result.filter(row ->
                ((BigDecimal) row.get("salary")).compareTo(BigDecimal.valueOf(85000)) > 0
        );

        assertThat(filtered.count()).isEqualTo(1);
        assertThat(filtered.first()).containsEntry("name", "Bob");
    }

    @Test
    void testSelect() {
        DynamicReportResult selected = result.select("name", "salary");

        assertThat(selected.count()).isEqualTo(3);
        assertThat(selected.first()).containsKeys("name", "salary");
        assertThat(selected.first()).doesNotContainKey("employee_id");
        assertThat(selected.first()).doesNotContainKey("department_id");
    }

    @Test
    void testGroupBy() {
        Map<Object, List<Map<String, Object>>> grouped = result.groupBy("department_id");

        assertThat(grouped).hasSize(2);
        assertThat(grouped.get(10)).hasSize(2);  // Alice and Charlie
        assertThat(grouped.get(20)).hasSize(1);  // Bob
    }

    @Test
    void testGroupByWithAggregations() {
        Map<String, AggregationFunction> aggregations = new HashMap<>();
        aggregations.put("salary", AggregationFunction.AVG);
        aggregations.put("employee_id", AggregationFunction.COUNT);

        Map<Object, Map<String, Object>> aggregated =
                result.groupByWithAggregations("department_id", aggregations);

        assertThat(aggregated).hasSize(2);

        // Department 10: Alice (80k) + Charlie (85k) = avg 82.5k
        Map<String, Object> dept10 = aggregated.get(10);
        assertThat(dept10).containsEntry("department_id", 10);
        assertThat(dept10).containsEntry("employee_id_count", 2L);
        assertThat((Double) dept10.get("salary_avg")).isCloseTo(82500.0, within(0.01));

        // Department 20: Bob (90k)
        Map<String, Object> dept20 = aggregated.get(20);
        assertThat(dept20).containsEntry("employee_id_count", 1L);
        assertThat((Double) dept20.get("salary_avg")).isCloseTo(90000.0, within(0.01));
    }

    @Test
    void testWithColumn() {
        DynamicReportResult withBonus = result.withColumn("bonus",
                row -> {
                    BigDecimal salary = (BigDecimal) row.get("salary");
                    return salary.multiply(BigDecimal.valueOf(0.1));
                });

        assertThat(withBonus.count()).isEqualTo(3);
        assertThat(withBonus.first()).containsKey("bonus");

        BigDecimal firstSalary = (BigDecimal) sampleData.get(0).get("salary");
        BigDecimal expectedBonus = firstSalary.multiply(BigDecimal.valueOf(0.1));
        assertThat(withBonus.first().get("bonus")).isEqualTo(expectedBonus);
    }

    @Test
    void testOrderBy() {
        DynamicReportResult sorted = result.orderBy("salary", false); // descending

        List<Map<String, Object>> rows = sorted.getRows();
        assertThat(rows.get(0).get("name")).isEqualTo("Bob");      // 90k
        assertThat(rows.get(1).get("name")).isEqualTo("Charlie");  // 85k
        assertThat(rows.get(2).get("name")).isEqualTo("Alice");    // 80k
    }

    @Test
    void testLimit() {
        DynamicReportResult limited = result.limit(2);

        assertThat(limited.count()).isEqualTo(2);
    }

    @Test
    void testDistinct() {
        // Add a duplicate row
        sampleData.add(sampleData.get(0));
        result = new DynamicReportResult(sampleData, new HashMap<>(), "test_report");

        assertThat(result.count()).isEqualTo(4);

        DynamicReportResult distinct = result.distinct();
        assertThat(distinct.count()).isEqualTo(3);
    }

    @Test
    void testFirst() {
        Map<String, Object> first = result.first();
        assertThat(first).isNotNull();
        assertThat(first).containsEntry("name", "Alice");
    }

    @Test
    void testFirstOnEmptyResult() {
        DynamicReportResult empty = new DynamicReportResult(
                Collections.emptyList(), new HashMap<>(), "empty");
        assertThat(empty.first()).isNull();
    }

    @Test
    void testGetRow() {
        Map<String, Object> row = result.getRow(1);
        assertThat(row).containsEntry("name", "Bob");
    }

    @Test
    void testGetRowOutOfBounds() {
        assertThatThrownBy(() -> result.getRow(10))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void testToJSON() {
        String json = result.toJSON();
        assertThat(json)
                .contains("Alice")
                .contains("Bob")
                .contains("Charlie");
    }

    @Test
    void testChainedOperations() {
        // Chain multiple operations
        DynamicReportResult chained = result
                .filter(row -> ((Integer) row.get("department_id")) == 10)
                .select("name", "salary")
                .orderBy("salary", true)
                .limit(1);

        assertThat(chained.count()).isEqualTo(1);
        assertThat(chained.first()).containsEntry("name", "Alice");
        assertThat(chained.first()).containsKey("salary");
        assertThat(chained.first()).doesNotContainKey("employee_id");
    }
}
