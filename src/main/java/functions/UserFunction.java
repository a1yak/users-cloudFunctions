package functions;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UserFunction implements HttpFunction {

    private static final Gson gson = new Gson();
    private static final Map<String, User> users = new HashMap<>();

    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        String method = request.getMethod();
        JsonObject body = gson.fromJson(request.getReader(), JsonObject.class);

        String path = request.getPath();

        switch (method) {
            case "POST":
                // Create user
                User user = gson.fromJson(body, User.class);
                user.setId(UUID.randomUUID().toString());
                users.put(user.getId(), user);
                writeResponse(response, user);
                break;

            case "GET":
                // Read user(s)
                if (path.contains("/users/")) {
                    String id = path.substring(path.lastIndexOf('/') + 1);
                    User foundUser = users.get(id);
                    if (foundUser != null) {
                        writeResponse(response, foundUser);
                    } else {
                        response.setStatusCode(404);
                        writeResponse(response, "User not found");
                    }
                } else {
                    writeResponse(response, users.values());
                }
                break;

            case "PUT":
                // Update user
                String id = body.get("id").getAsString();
                if (users.containsKey(id)) {
                    User updatedUser = gson.fromJson(body, User.class);
                    users.put(id, updatedUser);
                    writeResponse(response, updatedUser);
                } else {
                    response.setStatusCode(404);
                    writeResponse(response, "User not found");
                }
                break;

            case "DELETE":
                // Delete user
                String deleteId = path.substring(path.lastIndexOf('/') + 1);
                if (users.remove(deleteId) != null) {
                    writeResponse(response, "User deleted successfully");
                } else {
                    response.setStatusCode(404);
                    writeResponse(response, "User not found");
                }
                break;

            default:
                response.setStatusCode(405);
                writeResponse(response, "Method not allowed");
        }
    }

    private void writeResponse(HttpResponse response, Object message) throws Exception {
        response.setContentType("application/json");
        try (BufferedWriter writer = response.getWriter()) {
            writer.write(gson.toJson(message));
        }
    }
}
