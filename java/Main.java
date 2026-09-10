import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URLDecoder;

import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        StudentManager manager = new StudentManager();

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

            server.createContext("/students", exchange -> handleStudents(exchange, manager));
            server.createContext("/", Main::serveFile);

            server.start();
            System.out.println("Server started at http://localhost:8080");
        } catch (IOException exception) {
            System.out.println("Could not start server: " + exception.getMessage());
        }
    }

    private static void handleStudents(HttpExchange exchange, StudentManager manager) throws IOException {
        addCorsHeader(exchange);

        if (exchange.getRequestMethod().equals("GET")) {
            sendStudents(exchange, manager);
            return;
        }

        if (exchange.getRequestMethod().equals("POST")) {
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String name = getJsonText(requestBody, "name");
            String subject = getJsonText(requestBody, "subject");
            int marks = getJsonNumber(requestBody, "score");

            String error = manager.addScore(name, subject, marks);
            if (error.isEmpty()) {
                sendResponse(exchange, 200, "{\"message\":\"Score added successfully\"}");
            } else {
                sendResponse(exchange, 400, "{\"error\":\"" + error + "\"}");
            }
            return;
        }

        if (exchange.getRequestMethod().equals("DELETE")) {
            Map<String, String> query = getQueryParameters(exchange.getRequestURI().getQuery());
            boolean removed;

            if (query.containsKey("clear")) {
                manager.clearStudents();
                removed = true;
            } else if (query.containsKey("subject")) {
                removed = manager.removeScore(query.get("name"), query.get("subject"));
            } else {
                removed = manager.removeStudent(query.get("name"));
            }

            if (removed) {
                sendResponse(exchange, 200, "{\"message\":\"Removed successfully\"}");
            } else {
                sendResponse(exchange, 404, "{\"error\":\"Record not found\"}");
            }
            return;
        }

        sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
    }

    private static void sendStudents(HttpExchange exchange, StudentManager manager) throws IOException {
        StringBuilder json = new StringBuilder("[");

        for (int index = 0; index < manager.getStudents().size(); index++) {
            Student student = manager.getStudents().get(index);
            json.append("{\"name\":\"")
                    .append(student.getName())
                    .append("\",\"average\":")
                    .append(formatNumber(student.getAverage()))
                    .append(",\"grade\":\"")
                    .append(student.getLetterGrade())
                    .append("\",\"scores\":[");

            for (int gradeIndex = 0; gradeIndex < student.getGrades().size(); gradeIndex++) {
                Grade grade = student.getGrades().get(gradeIndex);
                json.append("{\"subject\":\"")
                        .append(grade.getSubject())
                        .append("\",\"score\":")
                        .append(grade.getScore())
                        .append("}");

                if (gradeIndex < student.getGrades().size() - 1) {
                    json.append(",");
                }
            }

            json.append("]}");

            if (index < manager.getStudents().size() - 1) {
                json.append(",");
            }
        }

        json.append("]");
        sendResponse(exchange, 200, json.toString());
    }

    private static String getJsonText(String json, String key) {
        String searchText = "\"" + key + "\":\"";
        int start = json.indexOf(searchText);

        if (start == -1) {
            return "";
        }

        start = start + searchText.length();
        int end = json.indexOf("\"", start);
        return end == -1 ? "" : json.substring(start, end);
    }

    private static int getJsonNumber(String json, String key) {
        String searchText = "\"" + key + "\":";
        int start = json.indexOf(searchText);

        if (start == -1) {
            return -1;
        }

        start = start + searchText.length();
        int end = start;

        while (end < json.length() && Character.isDigit(json.charAt(end))) {
            end++;
        }

        try {
            return Integer.parseInt(json.substring(start, end));
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private static String formatNumber(double number) {
        return String.format(java.util.Locale.US, "%.2f", number);
    }

    private static Map<String, String> getQueryParameters(String query) {
        Map<String, String> parameters = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return parameters;
        }

        for (String part : query.split("&")) {
            String[] values = part.split("=", 2);
            if (values.length == 2) {
                parameters.put(decode(values[0]), decode(values[1]));
            }
        }

        return parameters;
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            return value;
        }
    }

    private static void serveFile(HttpExchange exchange) throws IOException {
        String fileName = exchange.getRequestURI().getPath();

        if (fileName.equals("/")) {
            fileName = "/index.html";
        }

        Path filePath = Paths.get(".", fileName.substring(1));

        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            sendResponse(exchange, 404, "File not found");
            return;
        }

        String contentType = "text/plain";
        if (fileName.endsWith(".html")) {
            contentType = "text/html";
        } else if (fileName.endsWith(".css")) {
            contentType = "text/css";
        } else if (fileName.endsWith(".js")) {
            contentType = "application/javascript";
        }

        byte[] content = Files.readAllBytes(filePath);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, content.length);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(content);
        }
    }

    private static void addCorsHeader(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Content-Type", "application/json");
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(responseBytes);
        }
    }
}
