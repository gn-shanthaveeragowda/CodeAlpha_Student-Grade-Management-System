import java.util.ArrayList;

public class Student {
    private String name;
    private ArrayList<Grade> grades = new ArrayList<>();

    public Student(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public ArrayList<Grade> getGrades() {
        return grades;
    }

    public boolean addGrade(String subject, int score) {
        for (Grade grade : grades) {
            if (grade.getSubject().equalsIgnoreCase(subject)) {
                return false;
            }
        }

        grades.add(new Grade(subject, score));
        return true;
    }

    public boolean removeGrade(String subject) {
        for (int index = 0; index < grades.size(); index++) {
            if (grades.get(index).getSubject().equalsIgnoreCase(subject)) {
                grades.remove(index);
                return true;
            }
        }

        return false;
    }

    public double getAverage() {
        if (grades.isEmpty()) {
            return 0;
        }

        int total = 0;
        for (Grade grade : grades) {
            total += grade.getScore();
        }

        return (double) total / grades.size();
    }

    public String getLetterGrade() {
        double average = getAverage();

        if (average >= 90) {
            return "A";
        } else if (average >= 80) {
            return "B";
        } else if (average >= 70) {
            return "C";
        } else if (average >= 60) {
            return "D";
        }

        return "F";
    }
}
