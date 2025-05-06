package app.handlers;

import app.services.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import ratpack.exec.Promise;
import ratpack.handling.Chain;
import ratpack.jackson.Jackson;

import java.util.HashMap;
import java.util.Map;

public class UserHandler {
    public static void addRoutes(Chain chain) throws Exception {
        chain
            .path("register", ctx ->
                ctx.byMethod(m -> m
                    // Register a new SmartThings user
                    .post(() -> {
                        ctx.getRequest().getBody().then(body -> {
                            //Request Data
                            System.out.println("Request Packing");
                            Map<String,Object> bodymap = new ObjectMapper().readValue(body.getText(), HashMap.class);
                            if (bodymap.get("name") == null || bodymap.get("dob") == null || bodymap.get("address") == null || bodymap.get("country") == null) {
                                HashMap<String, Object> errorResponse = new HashMap<>();
                                errorResponse.put("status", "error");
                                errorResponse.put("message", "400 Bad Request : Name, DOB, Address, Country parameter is required");
                                ctx.getResponse().status(400);
                                ctx.render(Jackson.json(errorResponse));
                                return;
                            }

                            //Business Logic
                            UserService userService = ctx.get(UserService.class);

                            Promise<Map<String, Object>> userPromise = Promise.async(downstream ->
                                    userService.postOneUser(bodymap)
                                            .subscribe(
                                                    downstream::success,
                                                    downstream::error
                                            )
                            );

                            //Response Data
                            System.out.println("Response Packing");
                            userPromise
                                    .map( result -> {
                                        HashMap<String, Object> stringMap = new HashMap<>();
                                        stringMap.put("status", "success");
                                        stringMap.put("message", "User registered successfully");
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

                        });
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
                        //Request Data
                        String userId = ctx.getPathTokens().get("userId");
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
                        String deviceId = ctx.getPathTokens().get("deviceId");
                        if (deviceId == null || deviceId.isEmpty()) {
                            HashMap<String, Object> errorResponse = new HashMap<>();
                            errorResponse.put("status", "error");
                            errorResponse.put("message", "400 Bad Request : Device id is Required");
                            ctx.getResponse().status(400);
                            ctx.render(Jackson.json(errorResponse));
                            return;
                        }
                        if (deviceId.length()!=24){
                            HashMap<String, Object> stringMap = new HashMap<>();
                            stringMap.put("status", "error");
                            stringMap.put("message", "422 Unprocessable Entity : Exactly 24 Character Device's ID");
                            ctx.getResponse().status(422);
                            ctx.render(Jackson.json(stringMap));
                        }

                        //Business Logic
                        UserService userService = ctx.get(UserService.class);
                        DeviceService deviceService = ctx.get(DeviceService.class);
                        Promise<Map<String, Object>> devicePromise = Promise.async(downstream ->
                            //give information
                            userService.getActiveUserById(userId)
                                .subscribe(
                                    userName -> {
                                        if (userName.isEmpty()) {
                                            HashMap<String, Object> errorMap = new HashMap<>();
                                            errorMap.put("status", "error");
                                            errorMap.put("message", "User not found");
                                            ctx.getResponse().status(404);
                                            ctx.render(Jackson.json(errorMap));
                                        } else {
                                            //Kita ambil data  dari User adalah stat regdevcount berapa

                                            System.out.println("Getting Devicee data");
                                            deviceService.getOneDevice(deviceId)
                                                    .subscribe(
                                                            devices -> {
                                                                if(devices.isEmpty()){
                                                                    HashMap<String, Object> errorMap = new HashMap<>();
                                                                    errorMap.put("status", "error");
                                                                    errorMap.put("message", "Device not found");
                                                                    ctx.getResponse().status(404);
                                                                    ctx.render(Jackson.json(errorMap));
                                                                } else {
                                                                    /// /kita ambil data device yaitu default dan device id
                                                                    HashMap<String, Object> deviceResultMap = new HashMap<>();
                                                                    deviceResultMap.put("deviceName", devices.get("deviceName"));
                                                                    deviceResultMap.put("configuration", devices.get("configuration"));
                                                                    deviceResultMap.put("stats", devices.get("stats"));

                                                                    System.out.println("heeee");
                                                                    Document mydoc = (Document) deviceResultMap.get("configuration");
                                                                    Object defaultValue = mydoc.get("default"); ////////////////////////////////////
                                                                    System.out.println(defaultValue);
                                                                    Document mydocstati = (Document) userName.get("stats");
                                                                    Object sad = mydocstati.get("registeredDeviceCount");
                                                                    System.out.println(mydocstati.get("registeredDeviceCount")); ///////////////////////////////////////

                                                                    System.out.println(mydocstati);
                                                                    System.out.println(mydocstati.get("registeredDeviceCount")); ///////////////////////////////////////
                                                                    System.out.println("registered device and registered user");
                                                                    System.out.println(deviceId);//////////////////////////////////////////////
                                                                    System.out.println(mydoc.get("default")); ///////////////////////////////////////
                                                                    Document mydocstat = (Document) deviceResultMap.get("stats");
                                                                    System.out.println(mydocstat.get("registeredUserCount")); ///////////////////////////////////////
                                                                    userService.userDeviceExists(userId, deviceId)
                                                                        .subscribe(
                                                                                isExist -> {
                                                                                    if (isExist){
                                                                                        HashMap<String, Object> errorMap = new HashMap<>();
                                                                                        errorMap.put("status", "error");
                                                                                        errorMap.put("message", "Device already Registered");
                                                                                        ctx.getResponse().status(404);
                                                                                        ctx.render(Jackson.json(errorMap));
                                                                                    } else {
                                                                                        userService.updateUsersRegisteredDevice(userId, deviceId, defaultValue.toString(), sad.toString())
                                                                                                .subscribe(
                                                                                                        user -> {
                                                                                                            System.out.println("final subscribe");
                                                                                                            HashMap<String, Object> resultMap = new HashMap<>();
                                                                                                            resultMap.put("userId", user.get("userId"));
                                                                                                            resultMap.put("deviceId", user.get("deviceId"));
                                                                                                            downstream.success(resultMap);
                                                                                                        },
                                                                                                        downstream::error
                                                                                                );
                                                                                    }
                                                                                },
                                                                                downstream::error
                                                                        );
                                                                }
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
                                stringMap.put("message", "Device registered to user successfully");
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
                    // Unregister a device from a user
                    .delete(() -> {
                        //Request Data
                        String userId = ctx.getPathTokens().get("userId");
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
                        String deviceId = ctx.getPathTokens().get("deviceId");
                        if (deviceId == null || deviceId.isEmpty()) {
                            HashMap<String, Object> errorResponse = new HashMap<>();
                            errorResponse.put("status", "error");
                            errorResponse.put("message", "400 Bad Request : Device id is Required");
                            ctx.getResponse().status(400);
                            ctx.render(Jackson.json(errorResponse));
                            return;
                        }
                        if (deviceId.length()!=24){
                            HashMap<String, Object> stringMap = new HashMap<>();
                            stringMap.put("status", "error");
                            stringMap.put("message", "422 Unprocessable Entity : Exactly 24 Character Device's ID");
                            ctx.getResponse().status(422);
                            ctx.render(Jackson.json(stringMap));
                        }

                        //Business Logic
                        UserService userService = ctx.get(UserService.class);
                        DeviceService deviceService = ctx.get(DeviceService.class);
                        Promise<Map<String, Object>> devicePromise = Promise.async(downstream ->
                            userService.getActiveUserById(userId)
                                .subscribe(
                                    userName -> {
                                        if (userName.isEmpty()) {
                                            HashMap<String, Object> errorMap = new HashMap<>();
                                            errorMap.put("status", "error");
                                            errorMap.put("message", "User not found");
                                            ctx.getResponse().status(404);
                                            ctx.render(Jackson.json(errorMap));
                                        } else {
                                            System.out.println("Getting Devicee data");
                                            deviceService.getOneDevice(deviceId)
                                                .subscribe(
                                                    devices -> {
                                                        if(devices.isEmpty()){
                                                            HashMap<String, Object> errorMap = new HashMap<>();
                                                            errorMap.put("status", "error");
                                                            errorMap.put("message", "Device not found");
                                                            ctx.getResponse().status(404);
                                                            ctx.render(Jackson.json(errorMap));
                                                        } else {
                                                            /// /kita ambil data device yaitu default dan device id
                                                            HashMap<String, Object> deviceResultMap = new HashMap<>();
                                                            deviceResultMap.put("deviceName", devices.get("deviceName"));
                                                            deviceResultMap.put("configuration", devices.get("configuration"));
                                                            deviceResultMap.put("stats", devices.get("stats"));

                                                            System.out.println("heeee");
                                                            Document mydoc = (Document) deviceResultMap.get("configuration");
                                                            Object defaultValue = mydoc.get("default"); ////////////////////////////////////
                                                            System.out.println(defaultValue);
                                                            Document mydocstati = (Document) userName.get("stats");
                                                            Object registeredDeviceCountData = mydocstati.get("registeredDeviceCount");
                                                            System.out.println(mydocstati.get("registeredDeviceCount")); ///////////////////////////////////////

                                                            System.out.println(mydocstati);
                                                            System.out.println(mydocstati.get("registeredDeviceCount")); ///////////////////////////////////////
                                                            System.out.println("registered device and registered user");
                                                            System.out.println(deviceId);//////////////////////////////////////////////
                                                            System.out.println(mydoc.get("default")); ///////////////////////////////////////
                                                            Document mydocstat = (Document) deviceResultMap.get("stats");
                                                            System.out.println(mydocstat.get("registeredUserCount")); ///////////////////////////////////////

                                                            ////
                                                            userService.userDeviceExists(userId, deviceId)
                                                                .subscribe( isExist -> {
                                                                        if (isExist){
                                                                            userService.updateDeleteUsersRegisteredDevice(userId, deviceId, defaultValue.toString(), registeredDeviceCountData.toString())
                                                                                    .subscribe(
                                                                                            user -> {
                                                                                                System.out.println("final subscribe");
                                                                                                HashMap<String, Object> resultMap = new HashMap<>();
                                                                                                resultMap.put("userId", user.get("userId"));
                                                                                                resultMap.put("deviceId", user.get("deviceId"));
                                                                                                downstream.success(resultMap);
                                                                                            },
                                                                                            downstream::error
                                                                                    );
                                                                        } else {
                                                                            HashMap<String, Object> errorMap = new HashMap<>();
                                                                            errorMap.put("status", "error");
                                                                            errorMap.put("message", "Device not found");
                                                                            ctx.getResponse().status(404);
                                                                            ctx.render(Jackson.json(errorMap));
                                                                        }
                                                                    },
                                                                    downstream::error
                                                                );
                                                        }
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
                                    stringMap.put("message", "Device Unregistered from user successfully");
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