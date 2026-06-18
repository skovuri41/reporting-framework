package com.reporting.framework.transform;

import com.reporting.framework.api.DynamicReportResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ReportTransformer.
 */
class ReportTransformerTest {

    private DynamicReportResult employees;
    private DynamicReportResult departments;

    @BeforeEach
    void setUp() {
        // Create employees data
        List<Map<String, Object>> empData = new ArrayList<>();

        Map<String, Object> emp1 = new HashMap<>();
        emp1.put("employee_id", 1);
        emp1.put("name", "Alice");
        emp1.put("department_id", 10);
        empData.add(emp1);

        Map<String, Object> emp2 = new HashMap<>();
        emp2.put("employee_id", 2);
        emp2.put("name", "Bob");
        emp2.put("department_id", 20);
        empData.add(emp2);

        Map<String, Object> emp3 = new HashMap<>();
        emp3.put("employee_id", 3);
        emp3.put("name", "Charlie");
        emp3.put("department_id", 10);
        empData.add(emp3);

        employees = new DynamicReportResult(empData, new HashMap<>(), "employees");

        // Create departments data
        List<Map<String, Object>> deptData = new ArrayList<>();

        Map<String, Object> dept1 = new HashMap<>();
        dept1.put("department_id", 10);
        dept1.put("department_name", "Engineering");
        deptData.add(dept1);

        Map<String, Object> dept2 = new HashMap<>();
        dept2.put("department_id", 20);
        dept2.put("department_name", "Sales");
        deptData.add(dept2);

        Map<String, Object> dept3 = new HashMap<>();
        dept3.put("department_id", 30);
        dept3.put("department_name", "Marketing");
        deptData.add(dept3);

        departments = new DynamicReportResult(deptData, new HashMap<>(), "departments");
    }

    @Test
    void testInnerJoin() {
        DynamicReportResult joined = ReportTransformer.innerJoin(
                employees, departments, "department_id", "department_id");

        assertThat(joined.count()).isEqualTo(3);

        // Verify joined data
        List<Map<String, Object>> rows = joined.getRows();
        assertThat(rows.get(0)).containsEntry("name", "Alice");
        assertThat(rows.get(0)).containsEntry("department_name", "Engineering");
    }

    @Test
    void testLeftJoin() {
        // Add an employee with no matching department
        List<Map<String, Object>> empData = new ArrayList<>(employees.getRows());
        Map<String, Object> emp4 = new HashMap<>();
        emp4.put("employee_id", 4);
        emp4.put("name", "David");
        emp4.put("department_id", 99);  // No matching department
        empData.add(emp4);

        DynamicReportResult empWithOrphan = new DynamicReportResult(
                empData, new HashMap<>(), "employees");

        DynamicReportResult joined = ReportTransformer.leftJoin(
                empWithOrphan, departments, "department_id", "department_id");

        assertThat(joined.count()).isEqualTo(4);

        // David should be included even though his department doesn't exist
        List<Map<String, Object>> rows = joined.getRows();
        Optional<Map<String, Object>> david = rows.stream()
                .filter(row -> "David".equals(row.get("name")))
                .findFirst();

        assertThat(david).isPresent();
        assertThat(david.get()).doesNotContainKey("department_name");
    }

    @Test
    void testRightJoin() {
        DynamicReportResult joined = ReportTransformer.rightJoin(
                employees, departments, "department_id", "department_id");

        // Should include Marketing department even though no employees
        assertThat(joined.count()).isEqualTo(4);

        List<Map<String, Object>> rows = joined.getRows();
        Optional<Map<String, Object>> marketing = rows.stream()
                .filter(row -> "Marketing".equals(row.get("right_department_name")))
                .findFirst();

        assertThat(marketing).isPresent();
    }

    @Test
    void testUnion() {
        // Create two employee results
        List<Map<String, Object>> emp1Data = new ArrayList<>();
        Map<String, Object> emp1 = new HashMap<>();
        emp1.put("id", 1);
        emp1.put("name", "Alice");
        emp1Data.add(emp1);

        List<Map<String, Object>> emp2Data = new ArrayList<>();
        Map<String, Object> emp2 = new HashMap<>();
        emp2.put("id", 2);
        emp2.put("name", "Bob");
        emp2Data.add(emp2);

        DynamicReportResult result1 = new DynamicReportResult(emp1Data, new HashMap<>(), "emp1");
        DynamicReportResult result2 = new DynamicReportResult(emp2Data, new HashMap<>(), "emp2");

        DynamicReportResult unioned = ReportTransformer.union(result1, result2);

        assertThat(unioned.count()).isEqualTo(2);
        assertThat(unioned.getRows().get(0)).containsEntry("name", "Alice");
        assertThat(unioned.getRows().get(1)).containsEntry("name", "Bob");
    }

    @Test
    void testUnionDistinct() {
        // Create two results with duplicate data
        List<Map<String, Object>> data1 = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("id", 1);
        row1.put("name", "Alice");
        data1.add(row1);

        List<Map<String, Object>> data2 = new ArrayList<>();
        Map<String, Object> row2 = new HashMap<>();
        row2.put("id", 1);
        row2.put("name", "Alice");  // Duplicate
        data2.add(row2);

        DynamicReportResult result1 = new DynamicReportResult(data1, new HashMap<>(), "r1");
        DynamicReportResult result2 = new DynamicReportResult(data2, new HashMap<>(), "r2");

        DynamicReportResult unioned = ReportTransformer.unionDistinct(result1, result2);

        assertThat(unioned.count()).isEqualTo(1);
    }

    @Test
    void testCrossJoin() {
        // Create small datasets for cross join
        List<Map<String, Object>> data1 = new ArrayList<>();
        Map<String, Object> a1 = new HashMap<>();
        a1.put("letter", "A");
        data1.add(a1);

        Map<String, Object> a2 = new HashMap<>();
        a2.put("letter", "B");
        data1.add(a2);

        List<Map<String, Object>> data2 = new ArrayList<>();
        Map<String, Object> n1 = new HashMap<>();
        n1.put("number", 1);
        data2.add(n1);

        Map<String, Object> n2 = new HashMap<>();
        n2.put("number", 2);
        data2.add(n2);

        DynamicReportResult letters = new DynamicReportResult(data1, new HashMap<>(), "letters");
        DynamicReportResult numbers = new DynamicReportResult(data2, new HashMap<>(), "numbers");

        DynamicReportResult crossed = ReportTransformer.crossJoin(letters, numbers);

        assertThat(crossed.count()).isEqualTo(4); // 2 × 2 = 4
    }

    @Test
    void testPivot() {
        // Create sales data by month and product
        List<Map<String, Object>> salesData = new ArrayList<>();

        Map<String, Object> sale1 = new HashMap<>();
        sale1.put("product", "Widget");
        sale1.put("month", "Jan");
        sale1.put("amount", 100);
        salesData.add(sale1);

        Map<String, Object> sale2 = new HashMap<>();
        sale2.put("product", "Widget");
        sale2.put("month", "Feb");
        sale2.put("amount", 150);
        salesData.add(sale2);

        Map<String, Object> sale3 = new HashMap<>();
        sale3.put("product", "Gadget");
        sale3.put("month", "Jan");
        sale3.put("amount", 200);
        salesData.add(sale3);

        DynamicReportResult sales = new DynamicReportResult(
                salesData, new HashMap<>(), "sales");

        List<Map<String, Object>> pivoted = ReportTransformer.pivot(
                sales, "product", "month", "amount");

        assertThat(pivoted).hasSize(2);

        // Find Widget row
        Optional<Map<String, Object>> widget = pivoted.stream()
                .filter(row -> "Widget".equals(row.get("product")))
                .findFirst();

        assertThat(widget).isPresent();
        assertThat(widget.get()).containsEntry("Jan", 100);
        assertThat(widget.get()).containsEntry("Feb", 150);
    }

    @Test
    void testWithRunningTotal() {
        List<Map<String, Object>> salesData = new ArrayList<>();

        Map<String, Object> day1 = new HashMap<>();
        day1.put("day", 1);
        day1.put("revenue", 100.0);
        salesData.add(day1);

        Map<String, Object> day2 = new HashMap<>();
        day2.put("day", 2);
        day2.put("revenue", 150.0);
        salesData.add(day2);

        Map<String, Object> day3 = new HashMap<>();
        day3.put("day", 3);
        day3.put("revenue", 200.0);
        salesData.add(day3);

        DynamicReportResult sales = new DynamicReportResult(
                salesData, new HashMap<>(), "daily_sales");

        DynamicReportResult withRunningTotal = ReportTransformer.withRunningTotal(
                sales, "revenue", "running_total");

        List<Map<String, Object>> rows = withRunningTotal.getRows();
        assertThat(rows.get(0).get("running_total")).isEqualTo(100.0);
        assertThat(rows.get(1).get("running_total")).isEqualTo(250.0);  // 100 + 150
        assertThat(rows.get(2).get("running_total")).isEqualTo(450.0);  // 100 + 150 + 200
    }

    @Test
    void testJoinWithConflictingColumnNames() {
        // Both have "id" and "name" columns - should use prefixes
        List<Map<String, Object>> data1 = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("id", 1);
        row1.put("name", "Alice");
        row1.put("dept_id", 10);
        data1.add(row1);

        List<Map<String, Object>> data2 = new ArrayList<>();
        Map<String, Object> row2 = new HashMap<>();
        row2.put("id", 10);
        row2.put("name", "Engineering");
        data2.add(row2);

        DynamicReportResult r1 = new DynamicReportResult(data1, new HashMap<>(), "emp");
        DynamicReportResult r2 = new DynamicReportResult(data2, new HashMap<>(), "dept");

        DynamicReportResult joined = ReportTransformer.innerJoin(
                r1, r2, "dept_id", "id");

        Map<String, Object> result = joined.first();

        // Conflicting columns should have prefixes
        assertThat(result).containsKey("left_id");
        assertThat(result).containsKey("right_id");
        assertThat(result).containsKey("left_name");
        assertThat(result).containsKey("right_name");

        // Non-conflicting column should not have prefix
        assertThat(result).containsKey("dept_id");
    }
}
