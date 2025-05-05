package app;

import app.config.*;
import app.handlers.*;
import app.services.*;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import ratpack.server.RatpackServer;
import com.mongodb.reactivestreams.client.MongoDatabase;

public class Main {
    public static void main(String[] args) throws Exception {
        MongoClient mongoClient = MongoClients.create(MongoProvider.getDBUrl());
        MongoDatabase mongoDatabase = mongoClient.getDatabase(MongoProvider.getDBName());

        RatpackServer.start(server -> server
            .registryOf(registry -> {
                registry.add(MongoClient.class, mongoClient);
                registry.add(MongoDatabase.class, mongoDatabase);
                registry.add(VendorService.class, new VendorService(mongoDatabase));
                registry.add(DeviceService.class, new DeviceService(mongoDatabase));
            })
            .handlers(chain -> chain
                .all(ctx -> {
                    System.out.println(ctx.getRequest().getMethod() + " " + ctx.getRequest().getPath());
                    ctx.next();
                })
                .prefix("api", api -> api
                    .get(ctx -> ctx.render("Hello, Ratpack! This is Demo Smart Things by Ajie"))
                    .prefix("vendors", VendorHandler::addRoutes)
                    .prefix("users", UserHandler::addRoutes)
                    .prefix("devices", DeviceHandler::addRoutes)
                    .prefix("admin", AdminHandler::addRoutes)
                    .prefix("translation", TranslationHandler::addRoutes)
                )
            )
        );
    }
}