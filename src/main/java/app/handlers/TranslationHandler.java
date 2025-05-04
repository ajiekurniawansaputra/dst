package app.handlers;

import ratpack.handling.Chain;

public class TranslationHandler {
    public static void addRoutes(Chain chain) throws Exception {
        chain
            .path("", ctx ->
                ctx.byMethod(m -> m
                    // Translate text to one country
                    .get(() -> {
                        ctx.render("Translate text to one country");
                    })
                )
            );
    }
}