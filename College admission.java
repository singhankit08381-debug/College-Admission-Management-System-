import java.sql.*;
import java.io.*;
import java.util.*;

public class CollegeAdmissionSystem {

    //  Main Function (Menu System)
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        try (Connection con = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/college_admission", "root", "yourpassword")) {

            while (true) {
                System.out.println("\n===== College Admission Management System =====");
                System.out.println("1. Register Student");
                System.out.println("2. View All Students");
                System.out.println("3. Calculate Merit & Auto-Approve");
                System.out.println("4. View All Applications");
                System.out.println("5. Approve/Reject Manually");
                System.out.println("6. Generate Admission List (CSV)");
                System.out.println("7. Exit");
                System.out.print("Enter your choice: ");
                int ch = sc.nextInt();

                switch (ch) {
                    case 1 -> registerStudent(con, sc);
                    case 2 -> viewStudents(con);
                    case 3 -> calculateMerit(con);
                    case 4 -> viewApplications(con);
                    case 5 -> manualApproval(con, sc);
                    case 6 -> generateCSV(con);
                    case 7 -> {
                        System.out.println("Exiting... Goodbye!");
                        System.exit(0);
                    }
                    default -> System.out.println(" Invalid choice! Try again.");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //  Student Registration
    public static void registerStudent(Connection con, Scanner sc) throws Exception {
        sc.nextLine(); // clear buffer
        System.out.print("Enter Name: ");
        String name = sc.nextLine();
        System.out.print("Enter Email: ");
        String email = sc.nextLine();
        System.out.print("Enter Marks: ");
        float marks = sc.nextFloat();
        sc.nextLine();
        System.out.print("Enter Preferred Course: ");
        String course = sc.nextLine();

        String query = "INSERT INTO students(name, email, marks, course_preference) VALUES(?,?,?,?)";
        PreparedStatement ps = con.prepareStatement(query);
        ps.setString(1, name);
        ps.setString(2, email);
        ps.setFloat(3, marks);
        ps.setString(4, course);
        ps.executeUpdate();

        System.out.println(" Student Registered Successfully!");
    }

    //  View All Students
    public static void viewStudents(Connection con) throws Exception {
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery("SELECT * FROM students");

        System.out.println("\n--- Registered Students ---");
        while (rs.next()) {
            System.out.println("ID: " + rs.getInt("student_id") +
                               " | Name: " + rs.getString("name") +
                               " | Email: " + rs.getString("email") +
                               " | Marks: " + rs.getFloat("marks") +
                               " | Course: " + rs.getString("course_preference"));
        }
    }

    //  Merit Calculation and Auto-Approval
    public static void calculateMerit(Connection con) throws Exception {
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(
                "SELECT s.student_id, s.name, s.marks, c.course_id, c.course_name, c.cutoff_marks " +
                "FROM students s JOIN courses c ON s.course_preference = c.course_name");

        while (rs.next()) {
            int sid = rs.getInt("student_id");
            int cid = rs.getInt("course_id");
            float marks = rs.getFloat("marks");
            float cutoff = rs.getFloat("cutoff_marks");
            String status = (marks >= cutoff) ? "Approved" : "Rejected";

            String insert = "INSERT INTO applications(student_id, course_id, status) VALUES(?,?,?)";
            PreparedStatement ps = con.prepareStatement(insert);
            ps.setInt(1, sid);
            ps.setInt(2, cid);
            ps.setString(3, status);
            ps.executeUpdate();
        }

        System.out.println(" Merit list calculated and applications updated!");
    }

    // View All Applications
    public static void viewApplications(Connection con) throws Exception {
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(
                "SELECT a.app_id, s.name, c.course_name, a.status " +
                "FROM applications a " +
                "JOIN students s ON a.student_id = s.student_id " +
                "JOIN courses c ON a.course_id = c.course_id");

        System.out.println("\n--- Application List ---");
        while (rs.next()) {
            System.out.println("App ID: " + rs.getInt("app_id") +
                               " | Name: " + rs.getString("name") +
                               " | Course: " + rs.getString("course_name") +
                               " | Status: " + rs.getString("status"));
        }
    }

    //  Admin Manual Approval
    public static void manualApproval(Connection con, Scanner sc) throws Exception {
        System.out.print("Enter Application ID: ");
        int id = sc.nextInt();
        sc.nextLine();
        System.out.print("Enter new status (Approved/Rejected): ");
        String status = sc.nextLine();

        String query = "UPDATE applications SET status=? WHERE app_id=?";
        PreparedStatement ps = con.prepareStatement(query);
        ps.setString(1, status);
        ps.setInt(2, id);
        ps.executeUpdate();

        System.out.println("Application updated successfully!");
    }

    //  Generate Admission List in CSV
    public static void generateCSV(Connection con) throws Exception {
        PrintWriter writer = new PrintWriter(new File("admission_list.csv"));
        writer.println("Student ID,Name,Course,Status");

        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(
                "SELECT s.student_id, s.name, c.course_name, a.status " +
                "FROM applications a " +
                "JOIN students s ON a.student_id = s.student_id " +
                "JOIN courses c ON a.course_id = c.course_id");

        while (rs.next()) {
            writer.println(rs.getInt("student_id") + "," +
                           rs.getString("name") + "," +
                           rs.getString("course_name") + "," +
                           rs.getString("status"));
        }

        writer.close();
        System.out.println("Admission list generated: admission_list.csv");
    }
}
