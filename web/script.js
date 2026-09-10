let students = [];

async function addStudent() {
    const name = document.getElementById("studentName").value.trim();
    const subject = document.getElementById("studentSubject").value.trim();
    const marksText = document.getElementById("studentMarks").value;
    const score = Number(marksText);

    if (name === "") {
        showMessage("Enter a student name.", true);
        return;
    }

    if (subject === "") {
        showMessage("Enter a subject.", true);
        return;
    }

    if (marksText === "" || score < 0 || score > 100) {
        showMessage("Score must be between 0 and 100.", true);
        return;
    }

    try {
        const response = await fetch("/students", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ name: name, subject: subject, score: score })
        });
        const result = await response.json();

        if (!response.ok) {
            showMessage(result.error, true);
            return;
        }

        document.getElementById("studentName").value = "";
        document.getElementById("studentMarks").value = "";
        showMessage("Score saved successfully.", false);
        await loadStudents();
    } catch (error) {
        showMessage("Could not connect to the Java server.", true);
    }
}

async function loadStudents() {
    try {
        const response = await fetch("/students");
        students = await response.json();
        localStorage.setItem("gradeManagerStudents", JSON.stringify(students));
        displayStudents();
        displaySummary();
    } catch (error) {
        const savedStudents = localStorage.getItem("gradeManagerStudents");
        students = savedStudents ? JSON.parse(savedStudents) : [];
        displayStudents();
        displaySummary();
        showMessage("Server unavailable. Showing saved browser data.", true);
    }
}

function displayStudents() {
    const table = document.getElementById("studentTable");
    const emptyState = document.getElementById("emptyState");
    table.innerHTML = "";
    emptyState.style.display = students.length === 0 ? "flex" : "none";

    for (const student of students) {
        const scoreBadges = student.scores.map(score => `
            <button class="score-badge" title="Remove ${escapeHtml(score.subject)} score"
                    onclick="removeScore('${encodeURIComponent(student.name)}', '${encodeURIComponent(score.subject)}')">
                <span>${escapeHtml(score.subject)}</span>
                <strong>${score.score}</strong>
            </button>
        `).join("");

        table.innerHTML += `
            <tr>
                <td><strong>${escapeHtml(student.name)}</strong></td>
                <td><div class="score-list">${scoreBadges}</div></td>
                <td><strong class="average-value">${Number(student.average).toFixed(2)}</strong></td>
                <td><span class="grade-badge grade-${student.grade}">${student.grade}</span></td>
                <td><button class="remove-button" onclick="removeStudent('${encodeURIComponent(student.name)}')">Remove</button></td>
            </tr>
        `;
    }
}

function displaySummary() {
    const scoredStudents = students.filter(student => student.scores.length > 0);
    document.getElementById("studentCount").innerText = students.length;

    if (scoredStudents.length === 0) {
        document.getElementById("average").innerText = "0.00";
        document.getElementById("highest").innerText = "-";
        document.getElementById("lowest").innerText = "-";
        document.getElementById("highestName").innerText = "No scores yet";
        document.getElementById("lowestName").innerText = "No scores yet";
        return;
    }

    const total = scoredStudents.reduce((sum, student) => sum + student.scores.reduce((scoreSum, score) => scoreSum + score.score, 0), 0);
    const count = scoredStudents.reduce((sum, student) => sum + student.scores.length, 0);
    const highest = scoredStudents.reduce((best, student) => student.average > best.average ? student : best);
    const lowest = scoredStudents.reduce((best, student) => student.average < best.average ? student : best);

    document.getElementById("average").innerText = (total / count).toFixed(2);
    document.getElementById("highest").innerText = Number(highest.average).toFixed(2);
    document.getElementById("lowest").innerText = Number(lowest.average).toFixed(2);
    document.getElementById("highestName").innerText = highest.name;
    document.getElementById("lowestName").innerText = lowest.name;
}

async function removeScore(name, subject) {
    if (!confirm("Remove this subject score?")) {
        return;
    }

    await fetch(`/students?name=${name}&subject=${subject}`, { method: "DELETE" });
    await loadStudents();
}

async function removeStudent(name) {
    if (!confirm("Remove this entire student?")) {
        return;
    }

    await fetch(`/students?name=${name}`, { method: "DELETE" });
    await loadStudents();
}

async function clearStudents() {
    if (students.length === 0 || !confirm("Clear every student and score?")) {
        return;
    }

    await fetch("/students?clear=true", { method: "DELETE" });
    await loadStudents();
}

function exportCsv() {
    if (students.length === 0) {
        showMessage("There is no data to export.", true);
        return;
    }

    const rows = [["Student", "Subject", "Score", "Average", "Grade"]];
    for (const student of students) {
        for (const score of student.scores) {
            rows.push([student.name, score.subject, score.score, Number(student.average).toFixed(2), student.grade]);
        }
    }

    const csv = rows.map(row => row.map(value => `"${String(value).replaceAll('"', '""')}"`).join(",")).join("\n");
    const link = document.createElement("a");
    link.href = URL.createObjectURL(new Blob([csv], { type: "text/csv" }));
    link.download = "student-grades.csv";
    link.click();
    URL.revokeObjectURL(link.href);
}

function showMessage(message, isError) {
    const messageElement = document.getElementById("formMessage");
    messageElement.innerText = message;
    messageElement.className = isError ? "form-message error" : "form-message success";
}

function escapeHtml(value) {
    return value.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

loadStudents();
