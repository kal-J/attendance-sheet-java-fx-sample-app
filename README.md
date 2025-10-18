# Lecture Attendance Sheet Application - Beginner's Guide

## What This Application Does

This is a JavaFX desktop application that helps university lecturers record and manage student attendance. It stores data in a MySQL database and can automatically load previously saved attendance sheets.

## Prerequisites

Before running this application, you need:

1. **Java Development Kit (JDK)** - version 11 or higher
2. **JavaFX SDK** - for building graphical user interfaces
3. **MySQL Database** - for storing attendance data
4. **MySQL JDBC Driver** - for connecting Java to MySQL

## Database Setup

Create a MySQL database with these tables:

```sql
CREATE DATABASE attendance_sheets_db;
USE attendance_sheets_db;

CREATE TABLE attendance_sheets (
    id INT AUTO_INCREMENT PRIMARY KEY,
    program VARCHAR(50),
    faculty VARCHAR(100),
    department VARCHAR(100),
    course_unit VARCHAR(100),
    lecturer VARCHAR(100),
    submitted_at DATE,
    submitted_at_time VARCHAR(10),
    class_coordinator_name VARCHAR(100),
    class_coordinator_telephone VARCHAR(20),
    comments TEXT
);

CREATE TABLE students (
    id INT AUTO_INCREMENT PRIMARY KEY,
    attendance_sheet_id INT,
    student_name VARCHAR(100),
    reg_no VARCHAR(50) UNIQUE,
    FOREIGN KEY (attendance_sheet_id) REFERENCES attendance_sheets(id) ON DELETE CASCADE
);
```

## Understanding the Code Structure

### 1. Application Entry Point

```java
public class Main extends Application {
    public static void main(String[] args) {
        launch(args);
    }
}
```

- `Application` is the base class for all JavaFX applications
- `main()` calls `launch()` which starts the JavaFX application lifecycle
- The `start()` method is automatically called after the application initializes

### 2. The start() Method - Building the UI

```java
@Override
public void start(Stage primaryStage) {
    // Creates the main window
}
```

- `Stage` represents the main window
- This method builds all UI components and displays them

### 3. UI Components Breakdown

#### Header Section
```java
private VBox createHeader() {
    VBox header = new VBox(5);  // Vertical box with 5px spacing
    // ... adds title labels
}
```

**What's happening:**
- `VBox` arranges children vertically (top to bottom)
- `setAlignment(Pos.CENTER)` centers content
- `setPadding()` adds space around edges

#### Form Section
```java
private GridPane createInfoForm() {
    GridPane grid = new GridPane();  // Grid layout
    grid.add(new Label("Faculty:"), 0, 1);  // column 0, row 1
    grid.add(facultyField, 1, 1);            // column 1, row 1
}
```

**What's happening:**
- `GridPane` arranges components in rows and columns (like a table)
- `add(component, column, row)` places components at specific positions
- `TextField` is an input box where users type text
- `RadioButton` allows selecting one option from a group

#### Student Table
```java
private TableView<String[]> createStudentTable() {
    table = new TableView<>();
    table.setEditable(true);  // Users can edit cells
}
```

**Understanding the table columns:**

```java
// Column 1: Auto-numbered rows
TableColumn<String[], Integer> noCol = new TableColumn<>("No");
noCol.setCellValueFactory(data -> 
    new ReadOnlyObjectWrapper<>(table.getItems().indexOf(data.getValue()) + 1)
);
```

- `TableColumn<RowType, ColumnType>` defines a column
- `setCellValueFactory()` tells the column what data to display
- Lambda expression `data -> ...` is a shorthand function
- `indexOf() + 1` gives the row number (1-based)

```java
// Column 2: Editable student name
TableColumn<String[], String> nameCol = new TableColumn<>("Student Name");
nameCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue()[0]));
nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
nameCol.setOnEditCommit(e -> e.getRowValue()[0] = e.getNewValue());
```

- `data.getValue()[0]` gets the first element of the String array (name)
- `TextFieldTableCell` makes the cell editable
- `setOnEditCommit()` saves the new value when user finishes editing

**Data structure:**
- Each row is a `String[]` array: `[name, registration_number]`
- `ObservableList` automatically updates the UI when data changes

### 4. Database Operations

#### Connecting to Database
```java
try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
    // database operations
}
```

- `try-with-resources` automatically closes the connection
- `Connection` represents a link to the database

#### Executing Queries
```java
String sql = "SELECT * FROM attendance_sheets WHERE faculty = ?";
PreparedStatement stmt = conn.prepareStatement(sql);
stmt.setString(1, facultyField.getText());  // Replace ? with value
ResultSet rs = stmt.executeQuery();
```

- `PreparedStatement` prevents SQL injection attacks
- `?` is a placeholder for values
- `setString(position, value)` fills in the placeholders (1-based indexing)
- `ResultSet` contains the query results

#### Reading Results
```java
if (rs.next()) {  // Move to next row
    sheetId = rs.getInt("id");
    coordField.setText(rs.getString("class_coordinator_name"));
}
```

- `next()` moves to the next row (returns false if no more rows)
- `getInt()`, `getString()` retrieve column values by name

#### Inserting Data
```java
String sql = "INSERT INTO attendance_sheets (faculty, department) VALUES (?, ?)";
PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
stmt.setString(1, facultyField.getText());
stmt.setString(2, deptField.getText());
stmt.executeUpdate();

ResultSet rs = stmt.getGeneratedKeys();
if (rs.next()) {
    int newId = rs.getInt(1);  // Get the auto-generated ID
}
```

- `executeUpdate()` for INSERT, UPDATE, DELETE
- `RETURN_GENERATED_KEYS` lets you retrieve auto-generated IDs

### 5. Event Handling

#### Button Click
```java
Button saveBtn = new Button("Save to Database");
saveBtn.setOnAction(e -> saveToDatabase());
```

- `setOnAction()` defines what happens when button is clicked
- `e ->` is a lambda (arrow function) that calls `saveToDatabase()`

#### Auto-loading Data
```java
facultyField.textProperty().addListener((obs, old, newVal) -> loadExistingData());
```

- `textProperty()` is an observable property
- `addListener()` runs code when the property changes
- Triggers every time user types in the field

### 6. Understanding Java Generics in TableView

#### What are Generics?

Generics allow you to specify what type of data a class works with, using angle brackets `<>`. This provides type safety - the compiler can catch errors before runtime.

```java
// Without generics (old Java)
List list = new ArrayList();
list.add("Hello");
list.add(123);
String s = (String) list.get(1);  // Runtime error! It's an Integer

// With generics (modern Java)
List<String> list = new ArrayList<>();
list.add("Hello");
list.add(123);  // Compile error - won't even run!
```

#### TableView<String[]> Explained

```java
TableView<String[]> table = new TableView<>();
```

**Breaking it down:**

- `TableView` - the class name
- `<String[]>` - the **row type** (what each row contains)
- Each row in this table is a `String[]` (array of strings)
- In our case: `["John Doe", "REG001"]` represents one student

**Why String[] ?**
We're using a simple String array to hold student data where:
- `array[0]` = student name
- `array[1]` = registration number

**Alternative approaches:**
```java
// Using a custom Student class (more professional)
public class Student {
    private String name;
    private String regNo;
    // getters and setters
}
TableView<Student> table = new TableView<>();

// Using a Map
TableView<Map<String, String>> table = new TableView<>();

// Using just String (for single-column tables)
TableView<String> table = new TableView<>();
```

#### TableView<> with Diamond Operator

```java
TableView<String[]> table = new TableView<>();
//                                           ^^
//                                    Diamond operator
```

**The empty `<>` is shorthand:**
```java
// Long way (redundant)
TableView<String[]> table = new TableView<String[]>();

// Short way (Java 7+)
TableView<String[]> table = new TableView<>();
```

The compiler infers the type from the left side. Both are equivalent, but `<>` avoids repetition.

#### TableColumn<String[], String> Explained

```java
TableColumn<String[], String> nameCol = new TableColumn<>("Student Name");
```

**Breaking it down:**

- `TableColumn` - represents one column in the table
- `<String[], String>` - **two type parameters**:
    - **First type (`String[]`)** - the row type (must match TableView's row type)
    - **Second type (`String`)** - the column's data type (what this specific column displays)

**Visualizing it:**

```
Row Type: String[]         Column displays: String
    ↓                              ↓
["John Doe", "REG001"]  →  "John Doe"
    ↓                              ↓
["Jane Smith", "REG002"] →  "Jane Smith"
```

**More examples:**

```java
// Column that displays row numbers (Integer)
TableColumn<String[], Integer> noCol = new TableColumn<>("No");

// Column that displays registration number (String)
TableColumn<String[], String> regCol = new TableColumn<>("Reg. No.");

// If using a custom Student class:
TableColumn<Student, String> nameCol = new TableColumn<>("Name");
TableColumn<Student, Integer> ageCol = new TableColumn<>("Age");
```

#### How the Types Connect

```java
// 1. Create table with row type
TableView<String[]> table = new TableView<>();

// 2. Create column that works with those rows
TableColumn<String[], String> nameCol = new TableColumn<>("Student Name");
//           ^^^^^^^^  ^^^^^^
//           Must match  What this column extracts
//           table type

// 3. Tell the column how to extract its data from the row
nameCol.setCellValueFactory(data -> {
    String[] row = data.getValue();  // Gets the String[] row
    return new ReadOnlyStringWrapper(row[0]);  // Return the name (first element)
});

// 4. Add column to table
table.getColumns().add(nameCol);
```

#### Type Safety in Action

```java
// This works - types match
TableView<String[]> table = new TableView<>();
TableColumn<String[], String> col = new TableColumn<>("Name");
table.getColumns().add(col);

// This won't compile - types don't match
TableView<String[]> table = new TableView<>();
TableColumn<Integer[], String> col = new TableColumn<>("Name");
table.getColumns().add(col);  // ERROR: expects TableColumn<String[], ?>
```

#### The CellValueFactory Lambda

```java
nameCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue()[0]));
```

**What's happening:**

1. `data` - contains one row of data (a `String[]`)
2. `data.getValue()` - returns the `String[]` array
3. `data.getValue()[0]` - gets first element (name)
4. `new ReadOnlyStringWrapper(...)` - wraps it in an observable property
5. Lambda returns this for the column to display

**Longer form for clarity:**
```java
nameCol.setCellValueFactory(data -> {
    String[] row = data.getValue();           // Get the row
    String name = row[0];                     // Extract name
    return new ReadOnlyStringWrapper(name);   // Return as observable
});
```

#### Common Generic Patterns in JavaFX

```java
// Lists
ObservableList<String> items = FXCollections.observableArrayList();
ObservableList<Student> students = FXCollections.observableArrayList();

// Choice/Combo boxes
ChoiceBox<String> choice = new ChoiceBox<>();
ComboBox<Student> combo = new ComboBox<>();

// Properties
StringProperty name = new SimpleStringProperty("John");
IntegerProperty age = new SimpleIntegerProperty(25);

// Table columns with different types
TableColumn<Person, String> nameCol = new TableColumn<>();     // String column
TableColumn<Person, Integer> ageCol = new TableColumn<>();     // Integer column
TableColumn<Person, LocalDate> dateCol = new TableColumn<>();  // Date column
```

#### Why Use Generics?

**Without generics:**
```java
TableView table = new TableView();
table.getItems().add("String");
table.getItems().add(123);
table.getItems().add(new Student());  // Mixed types - chaos!

Object item = table.getItems().get(0);
String s = (String) item;  // Need manual casting, can fail at runtime
```

**With generics:**
```java
TableView<Student> table = new TableView<>();
table.getItems().add(new Student());  // OK
table.getItems().add("String");       // Compile error - type safety!

Student item = table.getItems().get(0);  // No casting needed
```

### 7. Key JavaFX Concepts

#### Properties and Binding
```java
datePicker.valueProperty().addListener((obs, old, newVal) -> loadExistingData());
```

- Properties are observable values
- Listeners respond to changes automatically
- This enables reactive UI updates

#### Layout Containers
- **VBox** - vertical stack
- **HBox** - horizontal stack
- **GridPane** - rows and columns
- **BorderPane** - top, bottom, left, right, center regions

#### Observable Collections
```java
ObservableList<String[]> students = FXCollections.observableArrayList();
table.setItems(students);
```

- `ObservableList` notifies the table when data changes
- Table automatically updates to show new data

## Application Flow

1. **Startup**: User launches application → `start()` method builds UI
2. **Data Entry**: User fills in course information
3. **Auto-load**: As user types, app checks if matching record exists
4. **Edit Students**: User clicks cells to edit student names and reg numbers
5. **Save**: User clicks "Save to Database" button
6. **Database Update**: App inserts new record or updates existing one

## Common Java/JavaFX Patterns Used

### Lambda Expressions
```java
// Old way (anonymous class)
button.setOnAction(new EventHandler<ActionEvent>() {
    public void handle(ActionEvent e) {
        doSomething();
    }
});

// New way (lambda)
button.setOnAction(e -> doSomething());
```

### Try-with-Resources
```java
try (Connection conn = getConnection()) {
    // use connection
}  // automatically closed
```

### Method References
```java
table.getItems().forEach(item -> processItem(item));  // Lambda
table.getItems().forEach(this::processItem);          // Method reference
```

## Configuration

Update these constants in the code:
```java
private static final String DB_URL = "jdbc:mysql://localhost:3306/attendance_sheets_db";
private static final String DB_USER = "root";
private static final String DB_PASSWORD = "password";
```

## Running the Application

1. Ensure MySQL is running
2. Create the database and tables
3. Update database credentials in code
4. Add JavaFX to your project classpath
5. Run the `Main` class

## Troubleshooting

**"Connection refused"**
- Check MySQL is running
- Verify database credentials
- Ensure database exists

**"ClassNotFoundException: com.mysql.cj.jdbc.Driver"**
- Add MySQL JDBC driver to classpath

**Table not updating**
- Ensure `setEditable(true)` is called
- Check `setOnEditCommit()` handlers are set

## Next Steps for Learning

1. Study JavaFX layouts (VBox, HBox, GridPane)
2. Learn about Properties and Binding
3. Understand JDBC and PreparedStatements
4. Explore JavaFX CSS styling
5. Learn about MVC (Model-View-Controller) pattern

## Key Takeaways

- JavaFX uses a scene graph (Stage → Scene → Nodes)
- Properties enable reactive programming
- Try-with-resources ensures cleanup
- PreparedStatements prevent SQL injection
- Lambda expressions simplify event handling
- ObservableList keeps UI synchronized with data