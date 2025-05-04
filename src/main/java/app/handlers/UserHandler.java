package app.handlers;

import ratpack.handling.Chain;

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
                    .get(() -> {
                        ctx.render("Get all registered devices (translated)");
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