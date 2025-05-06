package app.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import ratpack.exec.Promise;
import ratpack.handling.Chain;
import ratpack.jackson.Jackson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TranslationHandler {
    public static void addRoutes(Chain chain) throws Exception {
        chain
            .path("", ctx ->
                ctx.byMethod(m -> m
                    // Translate text to one country
                    .get(() -> {
                        ctx.getRequest().getBody().then(body -> {
                            Map<String, Object> bodymap = new ObjectMapper().readValue(body.getText(), HashMap.class);
                            if (bodymap.get("text") == null || bodymap.get("language") == null) {
                                HashMap<String, Object> errorResponse = new HashMap<>();
                                errorResponse.put("status", "error");
                                errorResponse.put("message", "400 Bad Request : Text and Language is required");
                                ctx.getResponse().status(400);
                                ctx.render(Jackson.json(errorResponse));
                            }
                            HashMap<String, Object> stringMap = new HashMap<>();
                            stringMap.put("text", bodymap.get("text"));
                            stringMap.put("language", bodymap.get("language"));
                            stringMap.put("translatedText", "Text ini di dalam bahasa yang kamu pahami");
                            ctx.getResponse().status(200);
                            ctx.render(Jackson.json(stringMap));
                        });
                    })
            ));
    }
}