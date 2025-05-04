package app.handlers;

import ratpack.handling.Chain;

public class AdminHandler {
    public static void addRoutes(Chain chain) throws Exception {
        chain
            .path("devices", ctx ->
                ctx.byMethod(m -> m
                    // List all vendor devices with user count
                    .get(() -> {
                        ctx.render("List all vendor devices with user count");
                    })
                )
            )
            .path("users", ctx ->
                ctx.byMethod(m -> m
                    // List all users with number of registered devices
                    .get(() -> {
                        ctx.render("List all users with number of registered devices");
                    })
                )
            )
            .path("users/:userId", ctx ->
                ctx.byMethod(m -> m
                    // View detailed user information
                    .get(() -> {
                        ctx.render("View detailed user information");
                    })
                )
            );
    }
}