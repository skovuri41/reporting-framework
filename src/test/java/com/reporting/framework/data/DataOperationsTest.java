package com.reporting.framework.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for DataOperations.
 */
class DataOperationsTest {

    private DataSet employees;
    private DataSet departments;

    @BeforeEach
    void setUp() {
        // Create employees data
        List<DataRow> empData = new ArrayList<>();

        empData.add(DataRow.builder()
                .put("employee_id", 1)
                .put("name", "Alice")
                .put("department_id", 10)
                .build());

        empData.add(DataRow.builder()
                .put("employee_id", 2)
                .put("name", "Bob")
                .put("department_id", 20)
                .build());

        empData.add(DataRow.builder()
                .put("employee_id", 3)
                .put("name", "Charlie")
                .put("department_id", 10)
                .build());

        employees = new DataSet(empData, new HashMap<>(), "employees");

        // Create departments data
        List<DataRow> deptData = new ArrayList<>();

        deptData.add(DataRow.builder()
                .put("department_id", 10)
                .put("department_name", "Engineering")
                .build());

        deptData.add(DataRow.builder()
                .put("department_id", 20)
                .put("department_name", "Sales")
                .build());

        deptData.add(DataRow.builder()
                .put("department_id", 30)
                .put("department_name", "Marketing")
                .build());

        departments = new DataSet(deptData, new HashMap<>(), "departments");
    }

    @Test
    void testInnerJoin() {
        DataSet joined = DataOperations.innerJoin(
                employees, departments, "department_id", "department_id");

        assertThat(joined.count()).isEqualTo(3);

        // Verify joined data
        List<DataRow> rows = joined.getRows();
        assertThat(rows.get(0).getString("name")).isEqualTo("Alice");
        assertThat(rows.get(0).getString("department_name")).isEqualTo("Engineering");
    }

    @Test
    void testLeftJoin() {
        // Add an employee with no matching department
        List<DataRow> empData = new ArrayList<>(employees.getRows());
        empData.add(DataRow.builder()
                .put("employee_id", 4)
                .put("name", "David")
                .put("department_id", 99)  // No matching department
                .build());

        DataSet empWithOrphan = new DataSet(empData, new HashMap<>(), "employees");

        DataSet joined = DataOperations.leftJoin(
                empWithOrphan, departments, "department_id", "department_id");

        assertThat(joined.count()).isEqualTo(4);

        // David should be included even though his department doesn't exist
        List<DataRow> rows = joined.getRows();
        Optional<DataRow> david = rows.stream()
                .filter(row -> "David".equals(row.getString("name")))
                .findFirst();

        assertThat(david).isPresent();
        assertThat(david.get().has("department_name")).isFalse();
    }

    @Test
    void testRightJoin() {
        DataSet joined = DataOperations.rightJoin(
                employees, departments, "department_id", "department_id");

        // Should include Marketing department even though no employees
        assertThat(joined.count()).isEqualTo(4);

        List<DataRow> rows = joined.getRows();
        Optional<DataRow> marketing = rows.stream()
                .filter(row -> "Marketing".equals(row.getString("right_department_name")))
                .findFirst();

        assertThat(marketing).isPresent();
    }

    @Test
    void testFullOuterJoin() {
        // Add orphan employee
        List<DataRow> empData = new ArrayList<>(employees.getRows());
        empData.add(DataRow.builder()
                .put("employee_id", 4)
                .put("name", "David")
                .put("department_id", 99)
                .build());

        DataSet empWithOrphan = new DataSet(empData, new HashMap<>(), "employees");

        DataSet joined = DataOperations.fullOuterJoin(
                empWithOrphan, departments, "department_id", "department_id");

        // Should include: Alice, Bob, Charlie (matched), David (left unmatched), Marketing (right unmatched)
        assertThat(joined.count()).isEqualTo(5);
    }

    @Test
    void testUnion() {
        // Create two employee results
        List<DataRow> emp1Data = new ArrayList<>();
        emp1Data.add(DataRow.builder()
                .put("id", 1)
                .put("name", "Alice")
                .build());

        List<DataRow> emp2Data = new ArrayList<>();
        emp2Data.add(DataRow.builder()
                .put("id", 2)
                .put("name", "Bob")
                .build());

        DataSet result1 = new DataSet(emp1Data, new HashMap<>(), "emp1");
        DataSet result2 = new DataSet(emp2Data, new HashMap<>(), "emp2");

        DataSet unioned = DataOperations.union(result1, result2);

        assertThat(unioned.count()).isEqualTo(2);
        assertThat(unioned.getRows().get(0).getString("name")).isEqualTo("Alice");
        assertThat(unioned.getRows().get(1).getString("name")).isEqualTo("Bob");
    }

    @Test
    void testUnionDistinct() {
        // Create two results with duplicate data
        List<DataRow> data1 = new ArrayList<>();
        DataRow row = DataRow.builder()
                .put("id", 1)
                .put("name", "Alice")
                .build();
        data1.add(row);

        List<DataRow> data2 = new ArrayList<>();
        data2.add(row);  // Same DataRow instance (will be equal)

        DataSet result1 = new DataSet(data1, new HashMap<>(), "r1");
        DataSet result2 = new DataSet(data2, new HashMap<>(), "r2");

        DataSet unioned = DataOperations.unionDistinct(result1, result2);

        assertThat(unioned.count()).isEqualTo(1);
    }

    @Test
    void testCrossJoin() {
        // Create small datasets for cross join
        List<DataRow> data1 = new ArrayList<>();
        data1.add(DataRow.builder().put("letter", "A").build());
        data1.add(DataRow.builder().put("letter", "B").build());

        List<DataRow> data2 = new ArrayList<>();
        data2.add(DataRow.builder().put("number", 1).build());
        data2.add(DataRow.builder().put("number", 2).build());

        DataSet letters = new DataSet(data1, new HashMap<>(), "letters");
        DataSet numbers = new DataSet(data2, new HashMap<>(), "numbers");

        DataSet crossed = DataOperations.crossJoin(letters, numbers);

        assertThat(crossed.count()).isEqualTo(4); // 2 × 2 = 4
    }

    @Test
    void testPivot() {
        // Create sales data by month and product
        List<DataRow> salesData = new ArrayList<>();

        salesData.add(DataRow.builder()
                .put("product", "Widget")
                .put("month", "Jan")
                .put("amount", 100)
                .build());

        salesData.add(DataRow.builder()
                .put("product", "Widget")
                .put("month", "Feb")
                .put("amount", 150)
                .build());

        salesData.add(DataRow.builder()
                .put("product", "Gadget")
                .put("month", "Jan")
                .put("amount", 200)
                .build());

        DataSet sales = new DataSet(salesData, new HashMap<>(), "sales");

        List<DataRow> pivoted = DataOperations.pivot(
                sales, "product", "month", "amount");

        assertThat(pivoted).hasSize(2);

        // Find Widget row
        Optional<DataRow> widget = pivoted.stream()
                .filter(row -> "Widget".equals(row.getString("product")))
                .findFirst();

        assertThat(widget).isPresent();
        assertThat(widget.get().getInt("Jan")).isEqualTo(100);
        assertThat(widget.get().getInt("Feb")).isEqualTo(150);
    }

    @Test
    void testWithRunningTotal() {
        List<DataRow> salesData = new ArrayList<>();

        salesData.add(DataRow.builder()
                .put("day", 1)
                .put("revenue", 100.0)
                .build());

        salesData.add(DataRow.builder()
                .put("day", 2)
                .put("revenue", 150.0)
                .build());

        salesData.add(DataRow.builder()
                .put("day", 3)
                .put("revenue", 200.0)
                .build());

        DataSet sales = new DataSet(salesData, new HashMap<>(), "daily_sales");

        DataSet withRunningTotal = DataOperations.withRunningTotal(
                sales, "revenue", "running_total");

        List<DataRow> rows = withRunningTotal.getRows();
        assertThat(rows.get(0).getDouble("running_total")).isEqualTo(100.0);
        assertThat(rows.get(1).getDouble("running_total")).isEqualTo(250.0);  // 100 + 150
        assertThat(rows.get(2).getDouble("running_total")).isEqualTo(450.0);  // 100 + 150 + 200
    }

    @Test
    void testJoinWithConflictingColumnNames() {
        // Both have "id" and "name" columns - should use prefixes
        List<DataRow> data1 = new ArrayList<>();
        data1.add(DataRow.builder()
                .put("id", 1)
                .put("name", "Alice")
                .put("dept_id", 10)
                .build());

        List<DataRow> data2 = new ArrayList<>();
        data2.add(DataRow.builder()
                .put("id", 10)
                .put("name", "Engineering")
                .build());

        DataSet r1 = new DataSet(data1, new HashMap<>(), "emp");
        DataSet r2 = new DataSet(data2, new HashMap<>(), "dept");

        DataSet joined = DataOperations.innerJoin(r1, r2, "dept_id", "id");

        DataRow result = joined.first();

        // Conflicting columns should have prefixes
        assertThat(result.has("left_id")).isTrue();
        assertThat(result.has("right_id")).isTrue();
        assertThat(result.has("left_name")).isTrue();
        assertThat(result.has("right_name")).isTrue();

        // Non-conflicting column should not have prefix
        assertThat(result.has("dept_id")).isTrue();
    }

    @Test
    void testAggregationFunctions() {
        List<DataRow> salesData = new ArrayList<>();

        salesData.add(DataRow.builder().put("amount", 100.0).build());
        salesData.add(DataRow.builder().put("amount", 200.0).build());
        salesData.add(DataRow.builder().put("amount", 150.0).build());

        // Test SUM
        Object sum = DataOperations.aggregate(salesData, "amount", DataOperations.AggregationFunction.SUM);
        assertThat((Double) sum).isEqualTo(450.0);

        // Test AVG
        Object avg = DataOperations.aggregate(salesData, "amount", DataOperations.AggregationFunction.AVG);
        assertThat((Double) avg).isEqualTo(150.0);

        // Test COUNT
        Object count = DataOperations.aggregate(salesData, "amount", DataOperations.AggregationFunction.COUNT);
        assertThat((Long) count).isEqualTo(3L);

        // Test MIN
        Object min = DataOperations.aggregate(salesData, "amount", DataOperations.AggregationFunction.MIN);
        assertThat((Double) min).isEqualTo(100.0);

        // Test MAX
        Object max = DataOperations.aggregate(salesData, "amount", DataOperations.AggregationFunction.MAX);
        assertThat((Double) max).isEqualTo(200.0);
    }
}
