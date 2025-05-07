package app.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import ratpack.exec.Promise;
import ratpack.handling.Chain;
import ratpack.jackson.Jackson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TranslationHandler {
    public static void addRoutes(Chain chain) throws Exception {
        chain
            .path("", ctx ->
                ctx.byMethod(m -> m
                    // Translate text to one country
                    .post(() -> {
                        ctx.getRequest().getBody().then(body -> {
                            Map<String, Object> bodymap = new ObjectMapper().readValue(body.getText(), HashMap.class);
                            if (bodymap.get("text") == null || bodymap.get("country") == null) {
                                HashMap<String, Object> errorResponse = new HashMap<>();
                                errorResponse.put("status", "error");
                                errorResponse.put("message", "400 Bad Request : Text and Country is required");
                                ctx.getResponse().status(400);
                                ctx.render(Jackson.json(errorResponse));
                            }
                            HashMap<String, Object> stringMap = new HashMap<>();
                            stringMap.put("text", bodymap.get("text"));
                            stringMap.put("country", bodymap.get("country"));
                            stringMap.put("translation", "Text ini di dalam bahasa yang kamu pahami");
                            ctx.getResponse().status(200);
                            ctx.render(Jackson.json(stringMap));
                        });
                    })
            ))
            .path("all", ctx ->
                ctx.byMethod(m -> m
                    // Translate text to all country
                    .post(() -> {
                        ctx.getRequest().getBody().then(body -> {
                            Map<String, Object> bodymap = new ObjectMapper().readValue(body.getText(), HashMap.class);
                            if (bodymap.get("text") == null) {
                                HashMap<String, Object> errorResponse = new HashMap<>();
                                errorResponse.put("status", "error");
                                errorResponse.put("message", "400 Bad Request : Text is required");
                                ctx.getResponse().status(400);
                                ctx.render(Jackson.json(errorResponse));
                            }
                            HashMap<String, Object> stringMap = new HashMap<>();
                            stringMap.put("text", bodymap.get("text"));

                            String[] languages = {"id", "fr", "de", "es", "zh", "ar", "ru", "ja", "hi", "pt"};
                            String[] messages = {
                                    "Teks ini dalam bahasa yang kamu pahami", // id - Indonesian
                                    "Ce texte est dans une langue que vous comprenez", // fr - French
                                    "Dieser Text ist in einer Sprache, die Sie verstehen", // de - German
                                    "Este texto está en un idioma que entiendes", // es - Spanish
                                    "这段文字是你能理解的语言", // zh - Chinese
                                    "هذا النص بلغة تفهمها", // ar - Arabic
                                    "Этот текст на языке, который вы понимаете", // ru - Russian
                                    "このテキストはあなたが理解できる言語です", // ja - Japanese
                                    "यह पाठ उस भाषा में है जिसे आप समझते हैं", // hi - Hindi
                                    "Este texto está em um idioma que você entende" // pt - Portuguese
                            };
                            List<HashMap<String, Object>> translations = new ArrayList<>();
                            for (int i = 0; i < languages.length; i++) {
                                HashMap<String, Object> translationMap = new HashMap<>();
                                translationMap.put("country", languages[i]);
                                translationMap.put("translation", messages[i]);
                                translations.add(translationMap);
                            }
                            stringMap.put("translations", translations);

                            ctx.getResponse().status(200);
                            ctx.render(Jackson.json(stringMap));
                        });
                    })
            ));
    }
}