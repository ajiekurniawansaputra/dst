package app.handlers;

import app.services.*;

import ratpack.exec.Promise;
import ratpack.handling.Chain;
import ratpack.jackson.Jackson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserHandler {
    public static void addRoutes(Chain chain) throws Exception {
        chain
            .path("register", ctx ->
                ctx.byMethod(m -> m
                    // Register a new SmartThings user
                    .post(() -> {
                        ctx.render("Register a new SmartThings user");
                    })
                )
            )
            .path(":userId/devices", ctx ->
                ctx.byMethod(m -> m
                    // Get all registered devices (translated)
                    .get(() ->
                    {
                        //Request Data
                        String lang = ctx.getRequest().getQueryParams().get("lang");
                        if (lang == null || lang.isEmpty()) {
                            HashMap<String, Object> errorResponse = new HashMap<>();
                            errorResponse.put("status", "error");
                            errorResponse.put("message", "400 Bad Request : Language parameter is required");
                            ctx.getResponse().status(400);
                            ctx.render(Jackson.json(errorResponse));
                            return;
                        }
                        if (lang.length()>2){
                            HashMap<String, Object> stringMap = new HashMap<>();
                            stringMap.put("status", "error");
                            stringMap.put("message", "422 Unprocessable Entity : Max 2 Letter (country code) Ex: id, en, fr");
                            ctx.getResponse().status(422);
                            ctx.render(Jackson.json(stringMap));
                        }
                        String userId = ctx.getPathTokens().get("userId"); //24 character mongoDB id
                        if (userId == null || userId.isEmpty()) {
                            HashMap<String, Object> errorResponse = new HashMap<>();
                            errorResponse.put("status", "error");
                            errorResponse.put("message", "400 Bad Request : Vendor id is Required");
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
                        DeviceService deviceService = ctx.get(DeviceService.class);

                        Promise<Map<String, Object>> devicePromise = Promise.async(downstream ->
                            userService.getActiveUserById(userId)
                            .subscribe(
                                usersDevice -> {
                                    if (usersDevice.isEmpty()) {
                                        HashMap<String, Object> errorMap = new HashMap<>();
                                        errorMap.put("status", "error");
                                        errorMap.put("message", "User not found");
                                        ctx.getResponse().status(404);
                                        ctx.render(Jackson.json(errorMap));
                                    } else {
                                        // get device data from vendors
                                        deviceService.getAllUserDevices(userId, lang)
                                            .subscribe(
                                                devices -> {
                                                    HashMap<String, Object> resultMap = new HashMap<>();
                                                    resultMap.put("userName", usersDevice.get("name"));
                                                    resultMap.put("devices", devices);
                                                    downstream.success(resultMap);
                                                },
                                                downstream::error
                                            );
                                    }
                                },
                                downstream::error
                            )
                        );

                        //Response Data
                        devicePromise
                                .map( result -> {
                                    HashMap<String, Object> stringMap = new HashMap<>();
                                    stringMap.put("status", "success");
                                    stringMap.put("message", "List of User's Device");
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
            .path(":userId/devices/:deviceId", ctx ->
                ctx.byMethod(m -> m
                    // Register a device to a user
                    .post(() -> {
                        ctx.render("Register a device to a user");
                    })
                    // Unregister a device from a user
                    .delete(() -> {
                        ctx.render("Unregister a device from a user");
                    })
                )
            )
            .path(":userId/devices/:deviceId/control", ctx ->
                ctx.byMethod(m -> m
                    // Change the device value
                    .patch(() -> {
                        ctx.render("Change the device value");
                    })
                )
            );
    }
}