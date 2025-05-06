package app.handlers;

import app.services.DeviceService;
import app.services.VendorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.result.InsertOneResult;
import ratpack.exec.Promise;
import ratpack.handling.Chain;
import ratpack.http.TypedData;
import ratpack.jackson.Jackson;

import java.util.HashMap;
import java.util.Map;

public class VendorHandler {
    public static void addRoutes(Chain chain) {
        chain
            .path(":vendorId/devices", ctx ->
                ctx.byMethod(m -> m
                    // List all vendor's devices
                    .get(() -> {
                        //Request Data
                        String vendorId = ctx.getPathTokens().get("vendorId"); //24 character mongoDB id
                        if (vendorId == null || vendorId.isEmpty()) {
                            HashMap<String, Object> errorResponse = new HashMap<>();
                            errorResponse.put("status", "error");
                            errorResponse.put("message", "400 Bad Request : Vendor id is Required");
                            ctx.getResponse().status(400);
                            ctx.render(Jackson.json(errorResponse));
                            return;
                        }
                        if (vendorId.length()!=24){
                            HashMap<String, Object> stringMap = new HashMap<>();
                            stringMap.put("status", "error");
                            stringMap.put("message", "422 Unprocessable Entity : Exactly 24 Character Vendor's ID");
                            ctx.getResponse().status(422);
                            ctx.render(Jackson.json(stringMap));
                        }

                        //Business Logic Service, Should be integrated into 1 Service
                        VendorService vendorService = ctx.get(VendorService.class);
                        DeviceService deviceService = ctx.get(DeviceService.class);
                        Promise<Map<String, Object>> devicePromise = Promise.async(downstream ->
                            //give information
                            vendorService.getVendorById(vendorId)
                            .subscribe(
                                vendor -> {
                                System.out.println("is vendor here?");
                                if (vendor.isEmpty()) {
                                    // Vendor not found
                                    System.out.println("render error");
                                    HashMap<String, Object> errorMap = new HashMap<>();
                                    errorMap.put("status", "error");
                                    errorMap.put("message", "Vendor not found");
                                    ctx.getResponse().status(404);
                                    ctx.render(Jackson.json(errorMap));
                                } else {
                                    // Vendor exists, fetch devices
                                    System.out.println("Getting Devicee data");
                                    deviceService.getAllVendorDevices(vendorId)
                                        .subscribe(
                                            devices -> {
                                                HashMap<String, Object> resultMap = new HashMap<>();
                                                resultMap.put("vendorName", vendor.get("vendorName"));
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
                                stringMap.put("message", "List of Vendor's Device");
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
                    // Create device by vendor
                    .post(() -> {
                        String vendorId = ctx.getPathTokens().get("vendorId"); //24 character mongoDB id
                        ctx.getRequest().getBody().then(body -> {
                            //Request Data
                            System.out.println("Request Packing");
                            if (vendorId == null || vendorId.isEmpty()) {
                                HashMap<String, Object> errorResponse = new HashMap<>();
                                errorResponse.put("status", "error");
                                errorResponse.put("message", "400 Bad Request : Vendor id is Required");
                                ctx.getResponse().status(400);
                                ctx.render(Jackson.json(errorResponse));
                                return;
                            }
                            if (vendorId.length()!=24){
                                HashMap<String, Object> stringMap = new HashMap<>();
                                stringMap.put("status", "error");
                                stringMap.put("message", "422 Unprocessable Entity : Exactly 24 Character Vendor's ID");
                                ctx.getResponse().status(422);
                                ctx.render(Jackson.json(stringMap));
                            }
                            Map<String,Object> bodymap = new ObjectMapper().readValue(body.getText(), HashMap.class);
                            if (bodymap.get("brandName") == null || bodymap.get("deviceName") == null || bodymap.get("description") == null || bodymap.get("configuration") == null) {
                                HashMap<String, Object> errorResponse = new HashMap<>();
                                errorResponse.put("status", "error");
                                errorResponse.put("message", "400 Bad Request : Brand Name, Device Name, Description parameter is required");
                                ctx.getResponse().status(400);
                                ctx.render(Jackson.json(errorResponse));
                                return;
                            }
                            Map<String, Object> configurationMap = (Map<String, Object>) bodymap.get("configuration");
                            if ( configurationMap.get("max") == null || configurationMap.get("min") == null ||  configurationMap.get("default") == null){
                                HashMap<String, Object> errorResponse = new HashMap<>();
                                errorResponse.put("status", "error");
                                errorResponse.put("message", "400 Bad Request : Configuration parameter is required");
                                ctx.getResponse().status(400);
                                ctx.render(Jackson.json(errorResponse));
                                return;
                            }
                            //Tambah validasi default jika perlu, untuk sekarang kita lewati

                            //Business Logic
                            System.out.println("Business Logic");
                            DeviceService deviceService = ctx.get(DeviceService.class);

                            Promise<Map<String, Object>> devicePromise = Promise.async(downstream ->
                                    //add 1 querry to valdate vendor's id
                                    deviceService.postOneDevice(bodymap, vendorId)
                                            .subscribe(
                                                    downstream::success,
                                                    downstream::error
                                            )
                            );

                            //Response Data
                            System.out.println("Response Packing");
                            devicePromise
                                    .map( result -> {
                                        HashMap<String, Object> stringMap = new HashMap<>();
                                        stringMap.put("status", "success");
                                        stringMap.put("message", "Device created successfully");
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
            .path(":vendorId/devices/:deviceId", ctx ->
                ctx.byMethod(m -> m
                    // Update device by vendor
                    .patch(() -> {
                        String vendorId = ctx.getPathTokens().get("vendorId");
                        String deviceId = ctx.getPathTokens().get("deviceId");
                        ctx.getRequest().getBody().then(body -> {
                            //Request Data
                            System.out.println("Request Packing");
                            if (vendorId == null || vendorId.isEmpty()) {
                                HashMap<String, Object> errorResponse = new HashMap<>();
                                errorResponse.put("status", "error");
                                errorResponse.put("message", "400 Bad Request : Vendor id is Required");
                                ctx.getResponse().status(400);
                                ctx.render(Jackson.json(errorResponse));
                                return;
                            }
                            if (vendorId.length()!=24){
                                HashMap<String, Object> stringMap = new HashMap<>();
                                stringMap.put("status", "error");
                                stringMap.put("message", "422 Unprocessable Entity : Exactly 24 Character Vendor's ID");
                                ctx.getResponse().status(422);
                                ctx.render(Jackson.json(stringMap));
                            }
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
                            Map<String,Object> bodymap = new ObjectMapper().readValue(body.getText(), HashMap.class);

                            //Business Logic
                            DeviceService deviceService = ctx.get(DeviceService.class);
                            VendorService vendorService = ctx.get(VendorService.class);
                            Promise<Map<String, Object>> devicePromise = Promise.async(
                                downstream ->
                                vendorService.getVendorById(vendorId)
                                    .subscribe(
                                        vendorData -> {
                                            if (vendorData.isEmpty()){
                                                HashMap<String, Object> errorResponse = new HashMap<>();
                                                errorResponse.put("status", "error");
                                                errorResponse.put("message", "400 Bad Request : Vendor not Found");
                                                ctx.getResponse().status(400);
                                                ctx.render(Jackson.json(errorResponse));
                                            }
                                            else {
                                                deviceService.getOneDevice(deviceId)
                                                    .subscribe(
                                                        deviceData ->{
                                                            if(deviceData.isEmpty()){
                                                                HashMap<String, Object> errorResponse = new HashMap<>();
                                                                errorResponse.put("status", "error");
                                                                errorResponse.put("message", "400 Bad Request : User not Found");
                                                                ctx.getResponse().status(400);
                                                                ctx.render(Jackson.json(errorResponse));
                                                            }
                                                            else {
                                                                deviceService.updateDevice(bodymap, deviceId)
                                                                        .subscribe(
                                                                                downstream::success,
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

                            devicePromise
                                    .map( result -> {
                                        HashMap<String, Object> stringMap = new HashMap<>();
                                        stringMap.put("status", "success");
                                        stringMap.put("message", "Device information updated successfully");
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
                    // Delete device if not registered
                    .delete(() -> {
                        //Request Data
                        String vendorId = ctx.getPathTokens().get("vendorId");
                        String deviceId = ctx.getPathTokens().get("deviceId");
                        if (vendorId == null || vendorId.isEmpty()) {
                            HashMap<String, Object> errorResponse = new HashMap<>();
                            errorResponse.put("status", "error");
                            errorResponse.put("message", "400 Bad Request : Vendor id is Required");
                            ctx.getResponse().status(400);
                            ctx.render(Jackson.json(errorResponse));
                            return;
                        }
                        if (vendorId.length()!=24){
                            HashMap<String, Object> stringMap = new HashMap<>();
                            stringMap.put("status", "error");
                            stringMap.put("message", "422 Unprocessable Entity : Exactly 24 Character Vendor's ID");
                            ctx.getResponse().status(422);
                            ctx.render(Jackson.json(stringMap));
                        }
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
                        //Check Vendor
                        //Delete
                        VendorService vendorService = ctx.get(VendorService.class);
                        DeviceService deviceService = ctx.get(DeviceService.class);
                        Promise<Map<String, Object>> devicePromise = Promise.async(
                            downstream ->
                            vendorService.getVendorById(vendorId)
                                .subscribe(
                                    vendorData -> {
                                        if (vendorData.isEmpty()){
                                            HashMap<String, Object> errorResponse = new HashMap<>();
                                            errorResponse.put("status", "error");
                                            errorResponse.put("message", "400 Bad Request : Vendor not Found");
                                            ctx.getResponse().status(400);
                                            ctx.render(Jackson.json(errorResponse));
                                        }
                                        else {
                                            deviceService.deleteOneDevice(deviceId)
                                                .subscribe(
                                                    downstream::success,
                                                    downstream::error
                                                );
                                        }
                                    },
                                    downstream::error
                                )
                        );

                        devicePromise
                                .map( result -> {
                                    HashMap<String, Object> stringMap = new HashMap<>();
                                    stringMap.put("status", "success");
                                    stringMap.put("message", "Device Deleted successfully");
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