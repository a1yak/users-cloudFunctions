package functions;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedWriter;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class UserFunction implements HttpFunction {

    private static final Gson gson = new Gson();
    private static final Firestore firestore = FirestoreOptions.getDefaultInstance().getService();
    private static final String COLLECTION_NAME = "users";

    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        String method = request.getMethod();
        JsonObject body = gson.fromJson(request.getReader(), JsonObject.class);
        String path = request.getPath();

        try {
            switch (method) {
                case "POST":
                    // Create user
                    User user = gson.fromJson(body, User.class);
                    user.setId(UUID.randomUUID().toString());
                    createUser(user);
                    writeResponse(response, user);
                    break;

                case "GET":
                    // Read user(s)
                    if (path.contains("/users/")) {
                        String id = path.substring(path.lastIndexOf('/') + 1);
                        User foundUser = getUserById(id);
                        if (foundUser != null) {
                            writeResponse(response, foundUser);
                        } else {
                            response.setStatusCode(404);
                            writeResponse(response, "User not found");
                        }
                    } else {
                        List<User> users = getAllUsers();
                        writeResponse(response, users);
                    }
                    break;

                case "PUT":
                    // Update user
                    String id = body.get("id").getAsString();
                    if (getUserById(id) != null) {
                        User updatedUser = gson.fromJson(body, User.class);
                        updateUser(updatedUser);
                        writeResponse(response, updatedUser);
                    } else {
                        response.setStatusCode(404);
                        writeResponse(response, "User not found");
                    }
                    break;

                case "DELETE":
                    // Delete user
                    String deleteId = path.substring(path.lastIndexOf('/') + 1);
                    if (deleteUser(deleteId)) {
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
        } catch (Exception e) {
            response.setStatusCode(500);
            writeResponse(response, "Error: " + e.getMessage());
        }
    }

    private void createUser(User user) throws ExecutionException, InterruptedException {
        firestore.collection(COLLECTION_NAME).document(user.getId()).set(user).get();
    }

    private User getUserById(String id) throws ExecutionException, InterruptedException {
        DocumentSnapshot document = firestore.collection(COLLECTION_NAME).document(id).get().get();
        if (document.exists()) {
            return document.toObject(User.class);
        }
        return null;
    }

    private List<User> getAllUsers() throws ExecutionException, InterruptedException {
        List<User> users = new ArrayList<>();
        ApiFuture<QuerySnapshot> query = firestore.collection(COLLECTION_NAME).get();
        for (DocumentSnapshot document : query.get().getDocuments()) {
            users.add(document.toObject(User.class));
        }
        return users;
    }

    private void updateUser(User user) throws ExecutionException, InterruptedException {
        firestore.collection(COLLECTION_NAME).document(user.getId()).set(user).get();
    }

    private boolean deleteUser(String id) throws ExecutionException, InterruptedException {
        DocumentReference document = firestore.collection(COLLECTION_NAME).document(id);
        if (document.get().get().exists()) {
            document.delete().get();
            return true;
        }
        return false;
    }

    private void writeResponse(HttpResponse response, Object message) throws Exception {
        response.setContentType("application/json");
        try (BufferedWriter writer = response.getWriter()) {
            writer.write(gson.toJson(message));
        }
    }
}
