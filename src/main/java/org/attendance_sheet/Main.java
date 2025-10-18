package org.attendance_sheet;

import javafx.application.Application;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Lecture Attendance Sheet Application
 * Allows faculty to record and manage student attendance for courses
 */
public class Main extends Application {

    // Database configuration
    private static final String DB_URL = "jdbc:mysql://localhost:3306/attendance_sheets_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "password";
    private static final int STUDENT_ROWS = 20;

    // UI Components - Form Fields
    private TextField facultyField, deptField, courseField, lecturerField;
    private TextField coordField, telField, timeField;
    private TextArea commentsArea;
    private RadioButton dayRadio, weekendRadio;
    private DatePicker datePicker;
    private ToggleGroup programGroup;

    // UI Components - Table
    private TableView<String[]> table;

    // State tracking
    private int sheetId = 0;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Lecture Attendance Sheet");

        VBox header = createHeader();
        GridPane infoGrid = createInfoForm();
        TableView<String[]> studentTable = createStudentTable();
        VBox commentsBox = createCommentsSection();
        HBox buttonBox = createButtonBar();

        VBox center = new VBox(10, infoGrid, studentTable, commentsBox, buttonBox);
        center.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(center);

        Scene scene = new Scene(root, 900, 750);
        primaryStage.setScene(scene);
        primaryStage.show();

        setupAutoLoadListeners();
    }

    /**
     * Creates the header section with university name and title
     */
    private VBox createHeader() {
        VBox header = new VBox(5);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(15));

        Label title = new Label("AVANCE INTERNATIONAL UNIVERSITY");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 20));

        Label subtitle = new Label("Bachelor of IT Lecture Attendance Sheet");
        subtitle.setFont(Font.font("Arial", FontWeight.NORMAL, 14));

        header.getChildren().addAll(title, subtitle);
        return header;
    }

    /**
     * Creates the course information form with all input fields
     */
    private GridPane createInfoForm() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));

        // Program selection (Day/Weekend)
        programGroup = new ToggleGroup();
        dayRadio = new RadioButton("Day");
        weekendRadio = new RadioButton("Weekend");
        dayRadio.setToggleGroup(programGroup);
        weekendRadio.setToggleGroup(programGroup);
        HBox programBox = new HBox(10, dayRadio, weekendRadio);

        // Initialize input fields
        facultyField = new TextField();
        deptField = new TextField();
        courseField = new TextField();
        lecturerField = new TextField();
        datePicker = new DatePicker(LocalDate.now());
        timeField = new TextField(LocalTime.now().toString().substring(0, 5));
        coordField = new TextField();
        telField = new TextField();

        // Add fields to grid
        grid.add(new Label("Program:"), 0, 0);
        grid.add(programBox, 1, 0);
        grid.add(new Label("Faculty:"), 0, 1);
        grid.add(facultyField, 1, 1);
        grid.add(new Label("Department:"), 2, 1);
        grid.add(deptField, 3, 1);
        grid.add(new Label("Course Unit:"), 0, 2);
        grid.add(courseField, 1, 2);
        grid.add(new Label("Lecturer:"), 2, 2);
        grid.add(lecturerField, 3, 2);
        grid.add(new Label("Date:"), 0, 3);
        grid.add(datePicker, 1, 3);
        grid.add(new Label("Time:"), 2, 3);
        grid.add(timeField, 3, 3);
        grid.add(new Label("Coordinator:"), 0, 4);
        grid.add(coordField, 1, 4);
        grid.add(new Label("Tel:"), 2, 4);
        grid.add(telField, 3, 4);

        return grid;
    }

    /**
     * Creates the student table with editable name and registration number columns
     */
    private TableView<String[]> createStudentTable() {
        table = new TableView<>();
        table.setEditable(true);

        // Column 1: Row number (auto-generated)
        TableColumn<String[], Integer> noCol = new TableColumn<>("No");
        noCol.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(table.getItems().indexOf(data.getValue()) + 1));
        noCol.setPrefWidth(50);

        // Column 2: Student name (editable)
        TableColumn<String[], String> nameCol = new TableColumn<>("Student Name");
        nameCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue()[0]));
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setOnEditCommit(e -> e.getRowValue()[0] = e.getNewValue());
        nameCol.setPrefWidth(300);

        // Column 3: Registration number (editable)
        TableColumn<String[], String> regCol = new TableColumn<>("Reg. No.");
        regCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue()[1]));
        regCol.setCellFactory(TextFieldTableCell.forTableColumn());
        regCol.setOnEditCommit(e -> e.getRowValue()[1] = e.getNewValue());
        regCol.setPrefWidth(150);

        table.getColumns().addAll(noCol, nameCol, regCol);

        // Initialize with empty rows
        ObservableList<String[]> students = FXCollections.observableArrayList();
        for (int i = 0; i < STUDENT_ROWS; i++) {
            students.add(new String[]{"", ""});
        }
        table.setItems(students);

        return table;
    }

    /**
     * Creates the comments section for lecturer notes
     */
    private VBox createCommentsSection() {
        VBox box = new VBox(5);
        box.setPadding(new Insets(10));

        Label label = new Label("Lecturer's Comments:");
        commentsArea = new TextArea();
        commentsArea.setPrefRowCount(3);

        box.getChildren().addAll(label, commentsArea);
        return box;
    }

    /**
     * Creates the button bar with save functionality
     */
    private HBox createButtonBar() {
        Button saveBtn = new Button("Save to Database");
        saveBtn.setOnAction(e -> saveToDatabase());
        saveBtn.setStyle("-fx-font-size: 14px; -fx-padding: 10px 20px;");

        HBox box = new HBox(saveBtn);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10));
        return box;
    }

    /**
     * Sets up listeners to automatically load existing data when form fields change
     */
    private void setupAutoLoadListeners() {
        programGroup.selectedToggleProperty().addListener((obs, old, newVal) -> loadExistingData());
        facultyField.textProperty().addListener((obs, old, newVal) -> loadExistingData());
        deptField.textProperty().addListener((obs, old, newVal) -> loadExistingData());
        courseField.textProperty().addListener((obs, old, newVal) -> loadExistingData());
        lecturerField.textProperty().addListener((obs, old, newVal) -> loadExistingData());
        datePicker.valueProperty().addListener((obs, old, newVal) -> loadExistingData());
        timeField.textProperty().addListener((obs, old, newVal) -> loadExistingData());
    }

    /**
     * Loads existing attendance data if a matching record exists in the database
     */
    private void loadExistingData() {
        if (!isFormComplete()) return;

        String program = dayRadio.isSelected() ? "Day" : "Weekend";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT * FROM attendance_sheets WHERE program = ? AND faculty = ? " +
                    "AND department = ? AND course_unit = ? AND lecturer = ? " +
                    "AND submitted_at = ? AND submitted_at_time = ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, program);
            stmt.setString(2, facultyField.getText());
            stmt.setString(3, deptField.getText());
            stmt.setString(4, courseField.getText());
            stmt.setString(5, lecturerField.getText());
            stmt.setDate(6, Date.valueOf(datePicker.getValue()));
            stmt.setString(7, timeField.getText());

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                loadSheetData(rs, conn);
            } else {
                clearLoadedData();
            }
        } catch (SQLException e) {
            showAlert("Error", "Database error: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    /**
     * Checks if all required form fields are filled
     */
    private boolean isFormComplete() {
        return programGroup.getSelectedToggle() != null &&
                !facultyField.getText().isEmpty() &&
                !deptField.getText().isEmpty() &&
                !courseField.getText().isEmpty() &&
                !lecturerField.getText().isEmpty() &&
                datePicker.getValue() != null &&
                !timeField.getText().isEmpty();
    }

    /**
     * Loads sheet and student data from database
     */
    private void loadSheetData(ResultSet rs, Connection conn) throws SQLException {
        sheetId = rs.getInt("id");
        coordField.setText(rs.getString("class_coordinator_name"));
        telField.setText(rs.getString("class_coordinator_telephone"));
        commentsArea.setText(rs.getString("comments"));

        // Load students
        String studSql = "SELECT student_name, reg_no FROM students WHERE attendance_sheet_id = ?";
        PreparedStatement studStmt = conn.prepareStatement(studSql);
        studStmt.setInt(1, sheetId);
        ResultSet studRs = studStmt.executeQuery();

        ObservableList<String[]> students = FXCollections.observableArrayList();
        while (studRs.next()) {
            students.add(new String[]{
                    studRs.getString("student_name"),
                    studRs.getString("reg_no")
            });
        }

        // Fill remaining rows with empty entries
        for (int i = students.size(); i < STUDENT_ROWS; i++) {
            students.add(new String[]{"", ""});
        }
        table.setItems(students);
    }

    /**
     * Clears previously loaded data when form changes
     */
    private void clearLoadedData() {
        if (sheetId > 0) {
            coordField.clear();
            telField.clear();
            commentsArea.clear();

            ObservableList<String[]> students = FXCollections.observableArrayList();
            for (int i = 0; i < STUDENT_ROWS; i++) {
                students.add(new String[]{"", ""});
            }
            table.setItems(students);
            sheetId = 0;
        }
    }

    /**
     * Saves or updates attendance data in the database
     */
    private void saveToDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String program = dayRadio.isSelected() ? "Day" : "Weekend";

            int sheetIdLocal = getExistingSheetId(conn, program);
            boolean isUpdate = (sheetIdLocal > 0);

            if (isUpdate) {
                updateAttendanceSheet(conn, sheetIdLocal);
            } else {
                sheetIdLocal = insertAttendanceSheet(conn, program);
            }

            int[] studentCounts = saveStudents(conn, sheetIdLocal, isUpdate);

            showSuccessMessage(isUpdate, studentCounts);

        } catch (SQLException e) {
            showAlert("Error", "Database error: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    /**
     * Checks if an attendance sheet already exists for the given parameters
     */
    private int getExistingSheetId(Connection conn, String program) throws SQLException {
        String sql = "SELECT id FROM attendance_sheets WHERE program = ? AND faculty = ? " +
                "AND department = ? AND course_unit = ? AND lecturer = ? " +
                "AND submitted_at = ? AND submitted_at_time = ?";

        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, program);
        stmt.setString(2, facultyField.getText());
        stmt.setString(3, deptField.getText());
        stmt.setString(4, courseField.getText());
        stmt.setString(5, lecturerField.getText());
        stmt.setDate(6, Date.valueOf(datePicker.getValue()));
        stmt.setString(7, timeField.getText());

        ResultSet rs = stmt.executeQuery();
        return rs.next() ? rs.getInt("id") : 0;
    }

    /**
     * Updates an existing attendance sheet
     */
    private void updateAttendanceSheet(Connection conn, int sheetId) throws SQLException {
        String sql = "UPDATE attendance_sheets SET class_coordinator_name = ?, " +
                "class_coordinator_telephone = ?, comments = ? WHERE id = ?";

        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, coordField.getText());
        stmt.setString(2, telField.getText());
        stmt.setString(3, commentsArea.getText());
        stmt.setInt(4, sheetId);
        stmt.executeUpdate();
    }

    /**
     * Inserts a new attendance sheet and returns its ID
     */
    private int insertAttendanceSheet(Connection conn, String program) throws SQLException {
        String sql = "INSERT INTO attendance_sheets (program, faculty, department, course_unit, " +
                "lecturer, submitted_at, submitted_at_time, class_coordinator_name, " +
                "class_coordinator_telephone, comments) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, program);
        stmt.setString(2, facultyField.getText());
        stmt.setString(3, deptField.getText());
        stmt.setString(4, courseField.getText());
        stmt.setString(5, lecturerField.getText());
        stmt.setDate(6, Date.valueOf(datePicker.getValue()));
        stmt.setString(7, timeField.getText());
        stmt.setString(8, coordField.getText());
        stmt.setString(9, telField.getText());
        stmt.setString(10, commentsArea.getText());
        stmt.executeUpdate();

        ResultSet rs = stmt.getGeneratedKeys();
        return rs.next() ? rs.getInt(1) : 0;
    }

    /**
     * Saves student data and returns counts of added/updated students
     */
    private int[] saveStudents(Connection conn, int sheetId, boolean isUpdate) throws SQLException {
        Set<String> currentRegNos = getCurrentRegNumbers();

        if (isUpdate) {
            deleteRemovedStudents(conn, sheetId, currentRegNos);
        }

        return insertOrUpdateStudents(conn, sheetId);
    }

    /**
     * Gets all non-empty registration numbers from the table
     */
    private Set<String> getCurrentRegNumbers() {
        Set<String> regNos = new HashSet<>();
        for (String[] row : table.getItems()) {
            if (!row[1].isEmpty()) {
                regNos.add(row[1]);
            }
        }
        return regNos;
    }

    /**
     * Deletes students that were removed from the table
     */
    private void deleteRemovedStudents(Connection conn, int sheetId, Set<String> currentRegNos)
            throws SQLException {
        String sql = "SELECT reg_no FROM students WHERE attendance_sheet_id = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, sheetId);
        ResultSet rs = stmt.executeQuery();

        Set<String> dbRegNos = new HashSet<>();
        while (rs.next()) {
            dbRegNos.add(rs.getString("reg_no"));
        }

        for (String dbReg : dbRegNos) {
            if (!currentRegNos.contains(dbReg)) {
                deleteStudent(conn, sheetId, dbReg);
            }
        }
    }

    /**
     * Deletes a single student record
     */
    private void deleteStudent(Connection conn, int sheetId, String regNo) throws SQLException {
        String sql = "DELETE FROM students WHERE attendance_sheet_id = ? AND reg_no = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, sheetId);
        stmt.setString(2, regNo);
        stmt.executeUpdate();
    }

    /**
     * Inserts or updates student records
     */
    private int[] insertOrUpdateStudents(Connection conn, int sheetId) throws SQLException {
        String sql = "INSERT INTO students (attendance_sheet_id, student_name, reg_no) " +
                "VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE student_name = ?";

        PreparedStatement stmt = conn.prepareStatement(sql);
        int insertCount = 0;
        int updateCount = 0;

        for (String[] row : table.getItems()) {
            String name = row[0];
            String reg = row[1];

            if (!name.isEmpty() && !reg.isEmpty()) {
                stmt.setInt(1, sheetId);
                stmt.setString(2, name);
                stmt.setString(3, reg);
                stmt.setString(4, name);

                int rows = stmt.executeUpdate();
                if (rows == 1) insertCount++;
                else if (rows == 2) updateCount++;
            }
        }

        return new int[]{insertCount, updateCount};
    }

    /**
     * Shows success message with operation details
     */
    private void showSuccessMessage(boolean isUpdate, int[] studentCounts) {
        String message = isUpdate ? "Attendance updated successfully!" : "Attendance saved successfully!";
        if (studentCounts[0] > 0 || studentCounts[1] > 0) {
            message += "\n(" + studentCounts[0] + " student(s) added, " +
                    studentCounts[1] + " updated)";
        }
        showAlert("Success", message, Alert.AlertType.INFORMATION);
    }

    /**
     * Displays an alert dialog
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}