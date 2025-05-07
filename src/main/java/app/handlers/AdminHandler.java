package app.handlers;

import app.services.DeviceService;
import app.services.UserService;
import ratpack.exec.Promise;
import ratpack.handling.Chain;
import ratpack.jackson.Jackson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminHandler {
    public static void addRoutes(Chain chain) throws Exception {
        chain
            .path("devices", ctx ->
                ctx.byMethod(m -> m
                    // List all vendor devices with user count
                    .get(() -> {
                        //Business Logic Service
                        DeviceService deviceService = ctx.get(DeviceService.class);
                        Promise<List<Map<String, Object>>> devicePromise =
                                Promise.async(downstream -> deviceService.getAllAdminDevices()
                                        .subscribe(
                                                downstream::success,
                                                downstream::error
                                        ));

                        //Response Data
                        devicePromise
                                .map( result -> {
                                    HashMap<String, Object> stringMap = new HashMap<>();
                                    stringMap.put("status", "success");
                                    stringMap.put("message", "List of All Device wih count");
                                    stringMap.put("data", result);
                                    return stringMap;
                                })
                                .onError(e -> {
                                    HashMap<String, Object> stringMap = new HashMap<>();
                                    stringMap.put("status", "error");
                                    stringMap.put("message", "500 internal server error : "+e.getMessage());
                                    ctx.getResponse().status(500);
                                    ctx.render(Jackson.json(stringMap));
                                })
                                .then(result -> ctx.render(Jackson.json(result)));
                    })
                )
            )
            .path("users", ctx ->
                ctx.byMethod(m -> m
                    // List all users with number of registered devices
                    .get(() -> {
                        //Business Logic Service
                        UserService userService = ctx.get(UserService.class);
                        Promise<List<Map<String, Object>>> userPromise =
                                Promise.async(downstream -> userService.getAllUsers()
                                        .subscribe(
                                                downstream::success,
                                                downstream::error
                                        ));

                        //Response Data
                        userPromise
                                .map( result -> {
                                    HashMap<String, Object> stringMap = new HashMap<>();
                                    stringMap.put("status", "success");
                                    stringMap.put("message", "List of All Users wih count");
                                    stringMap.put("data", result);
                                    return stringMap;
                                })
                                .onError(e -> {
                                    HashMap<String, Object> stringMap = new HashMap<>();
                                    stringMap.put("status", "error");
                                    stringMap.put("message", "500 internal server error : "+e.getMessage());
                                    ctx.getResponse().status(500);
                                    ctx.render(Jackson.json(stringMap));
                                })
                                .then(result -> ctx.render(Jackson.json(result)));
                    })
                )
            )
            .path("users/:userId", ctx ->
                ctx.byMethod(m -> m
                    // View detailed user information
                    .get(() -> {
                        //Request Data
                        String userId = ctx.getPathTokens().get("userId"); //24 character mongoDB id
                        if (userId == null || userId.isEmpty()) {
                            HashMap<String, Object> errorResponse = new HashMap<>();
                            errorResponse.put("status", "error");
                            errorResponse.put("message", "400 Bad Request : User id is Required");
                            ctx.getResponse().status(400);
                            ctx.render(Jackson.json(errorResponse));
                            return;
                        }
                        if (userId.length()!=24){
                            HashMap<String, Object> stringMap = new HashMap<>();
                            stringMap.put("status", "error");
                            stringMap.put("message", "422 Unprocessable Entity : Exactly 24 Character User's ID");
                            ctx.getResponse().status(422);
                            ctx.render(Jackson.json(stringMap));
                        }

                        //Business Logic Service
                        UserService userService = ctx.get(UserService.class);
                        Promise<Map<String, Object>> userPromise = Promise.async(downstream ->
                            userService.getUsersById(userId)
                                .subscribe(
                                    usersData -> {
                                        if (usersData.isEmpty()) {
                                            // User not found
                                            HashMap<String, Object> errorMap = new HashMap<>();
                                            errorMap.put("status", "error");
                                            errorMap.put("message", "User not found");
                                            ctx.getResponse().status(404);
                                            ctx.render(Jackson.json(errorMap));
                                        } else {
                                            // User exists, return
                                            downstream.success(usersData);
                                        }
                                    },
                                    downstream::error
                                )
                        );

                        //Response Data
                        userPromise
                                .map( result -> {
                                    HashMap<String, Object> stringMap = new HashMap<>();
                                    stringMap.put("status", "success");
                                    stringMap.put("message", "Show User Detailed Information");
                                    stringMap.put("data", result);
                                    return stringMap;
                                })
                                .onError(e -> {
                                    HashMap<String, Object> stringMap = new HashMap<>();
                                    stringMap.put("status", "error");
                                    stringMap.put("message", "500 internal server error : "+e.getMessage());
                                    ctx.getResponse().status(500);
                                    ctx.render(Jackson.json(stringMap));
                                })
                                .then(result -> ctx.render(Jackson.json(result)));
                    })
                )
            );
    }
}