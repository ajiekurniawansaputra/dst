package app.handlers;

import app.services.DeviceService;
import app.services.VendorService;
import ratpack.exec.Promise;
import ratpack.handling.Chain;
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
                        String vendorId = ctx.getPathTokens().get("vendorId");
                        ctx.render("Created device for vendor " + vendorId);
                    })
                )
            )
            .path(":vendorId/devices/:deviceId", ctx ->
                ctx.byMethod(m -> m
                    // Update device by vendor
                    .patch(() -> {
                        String vendorId = ctx.getPathTokens().get("vendorId");
                        String deviceId = ctx.getPathTokens().get("deviceId");
                        ctx.render("Update device");
                    })
                    // Delete device if not registered
                    .delete(() -> {
                        String vendorId = ctx.getPathTokens().get("vendorId");
                        String deviceId = ctx.getPathTokens().get("deviceId");
                        ctx.render("Delete device if not registered");
                    })
                )
            );
    }
}