package app.handlers;

import app.services.DeviceService;

import ratpack.handling.Chain;
import ratpack.exec.Promise;
import ratpack.jackson.Jackson;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class DeviceHandler {
    public static void addRoutes(Chain chain) {
        chain
            .path("available", ctx ->
                ctx.byMethod(m -> m
                    // Get all available devices translated to user’s language
                    .get(() -> {
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

                        //Business Logic Service
                        DeviceService deviceService = ctx.get(DeviceService.class);
                        Promise<List<Map<String, Object>>> devicePromise =
                            Promise.async(downstream -> deviceService.getAllDevices(lang)
                                .subscribe(
                                    downstream::success,
                                    downstream::error
                                ));

                        //Response Data
                        devicePromise
                            .map( result -> {
                                HashMap<String, Object> stringMap = new HashMap<>();
                                stringMap.put("status", "success");
                                stringMap.put("message", "List of device");
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