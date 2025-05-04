package app.handlers;

import ratpack.handling.Chain;

public class VendorHandler {
    public static void addRoutes(Chain chain) throws Exception {
        chain
            .path(":vendorId/devices", ctx ->
                ctx.byMethod(m -> m
                    // List all vendor devices
                    .get(() -> {
                        String vendorId = ctx.getPathTokens().get("vendorId");
                        ctx.render("List all vendor devices");
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