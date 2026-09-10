import java.util.ArrayList;

public class StudentManager {
    private ArrayList<Student> students = new ArrayList<>();

    public String addScore(String name, String subject, int score) {
        if (name == null || name.trim().isEmpty()) {
            return "Student name is required";
        }

        if (subject == null || subject.trim().isEmpty()) {
            return "Subject is required";
        }

        if (score < 0 || score > 100) {
            return "Score must be between 0 and 100";
        }

        Student student = findStudent(name.trim());
        if (student == null) {
            student = new Student(name.trim());
            students.add(student);
        }

        if (!student.addGrade(subject.trim(), score)) {
            return "This subject already exists for this student";
        }

        return "";
    }

    public ArrayList<Student> getStudents() {
        return students;
    }

    public boolean removeScore(String name, String subject) {
        Student student = findStudent(name);
        return student != null && student.removeGrade(subject);
    }

    public boolean removeStudent(String name) {
        Student student = findStudent(name);
        return student != null && students.remove(student);
    }

    public void clearStudents() {
        students.clear();
    }

    public double calculateClassAverage() {
        if (students.isEmpty()) {
            return 0;
        }

        int total = 0;
        int scoreCount = 0;

        for (Student student : students) {
            for (Grade grade : student.getGrades()) {
                total += grade.getScore();
                scoreCount++;
            }
        }

        return scoreCount == 0 ? 0 : (double) total / scoreCount;
    }

    public Student getHighestStudent() {
        Student highest = null;

        for (Student student : students) {
            if (!student.getGrades().isEmpty()
                    && (highest == null || student.getAverage() > highest.getAverage())) {
                highest = student;
            }
        }

        return highest;
    }

    public Student getLowestStudent() {
        Student lowest = null;

        for (Student student : students) {
            if (!student.getGrades().isEmpty()
                    && (lowest == null || student.getAverage() < lowest.getAverage())) {
                lowest = student;
            }
        }

        return lowest;
    }

    private Student findStudent(String name) {
        for (Student student : students) {
            if (student.getName().equalsIgnoreCase(name)) {
                return student;
            }
        }

        return null;
    }
}
