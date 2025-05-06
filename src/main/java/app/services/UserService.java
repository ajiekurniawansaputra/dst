package app.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.mongodb.reactivestreams.client.MongoCollection;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static com.mongodb.client.model.Updates.set;

import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import org.bson.Document;

public class UserService {
    private final MongoDatabase db;

    public UserService(MongoDatabase db) {
        this.db = db;
    }

    public Mono<Map<String, Object>> getActiveUserById(String userId) {
        MongoCollection<Document> user = db.getCollection("User");
        ObjectId objectId = new ObjectId(userId);
        return Mono.from(user.find(eq("_id", objectId)).projection(fields(include("name","stats"))).first())
                .map(doc -> {
                    System.out.println("packing User's data");
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("name", doc.getString("name"));
                    userMap.put("stats", doc.get("stats", Document.class));
                    return userMap;
                })
                .defaultIfEmpty(new HashMap<>())
                .doOnSuccess(map -> System.out.print("Successfully fetched"+map))
                .doOnError(e -> System.out.printf("Error fetching data : "+e));
    }

    public Mono<Map<String, Object>> getUsersById(String userId) {
        MongoCollection<Document> user = db.getCollection("User");
        ObjectId objectId = new ObjectId(userId);
        return Mono.from(user.find(eq("_id", objectId)).projection(fields(include("_id", "name","dob","address","country","stats","registeredDevices"))).first())
            .map(doc -> {
                System.out.println("packing User's data");
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("userId", doc.getObjectId("_id").toString());
                userMap.put("name", doc.getString("name"));
                userMap.put("dob", doc.getString("dob"));
                userMap.put("address", doc.getString("address"));
                userMap.put("country", doc.getString("country"));
                userMap.put("stats", doc.get("stats", Document.class));
                userMap.put("registeredDevices", doc.getList("registeredDevices", Document.class));
                return userMap;
            })
            .defaultIfEmpty(new HashMap<>())
            .doOnSuccess(map -> System.out.print("Successfully fetched"+map))
            .doOnError(e -> System.out.printf("Error fetching data : "+e));
    }

    public Mono<List<Map<String, Object>>> getAllUsers() {
        MongoCollection<Document> users = db.getCollection("User");
        return Flux.from(users.find())
                .map(doc -> {
                    Map<String, Object> user = new HashMap<>();
                    user.put("userId", doc.getObjectId("_id").toString());
                    user.put("name", doc.getString("name"));
                    user.put("dob", doc.getString("dob"));
                    user.put("address", doc.getString("address"));
                    user.put("country", doc.getString("country"));
                    user.put("registeredDevices", doc.getList("registeredDevices", Document.class));
                    user.put("stats", doc.get("stats", Document.class));
                    user.put("updatedAt", doc.getString("updatedAt"));
                    user.put("createdAt", doc.getString("createdAt"));
                    return user;
                })
                .collectList()
                .doOnSuccess(list -> System.out.printf("Successfully fetched : "+list.size()))
                .doOnError(e -> System.out.printf("Error fetching data : "+e));
    }

    public Mono<Map<String, Object>> postOneUser(Map<String,Object> bodymap) throws JsonProcessingException {
        MongoCollection<Document> user = db.getCollection("User");

        System.out.println("service post two");
        Document stats = new Document("registeredDeviceCount", 0);
        Document document = new Document("name", bodymap.get("name"))
                .append("dob", bodymap.get("dob"))
                .append("address", bodymap.get("address"))
                .append("country", bodymap.get("country"))
                .append("registeredDevices", new ArrayList<>())
                .append("stats", stats)
                .append("createdAt", "1990-05-10")
                .append("updatedAt", "1990-05-10");
        System.out.println("service post three");
        return Mono.from(user.insertOne(document))
                .map( doc -> {
                    Map<String, Object> device = new HashMap<>();
                    device.put("userId", doc.getInsertedId().asObjectId().getValue().toHexString());
                    device.put("name", bodymap.get("name"));
                    device.put("address", bodymap.get("address"));
                    device.put("dob", bodymap.get("dob"));
                    device.put("country", bodymap.get("country"));
                    return device;
                })
                .doOnSuccess(list -> System.out.print("Successfully posted"))
                .doOnError(e -> System.out.printf("Error fetching data : "+e));
    }

    public Mono<Map<String, Object>> updateUsersRegisteredDevice(String userId, String deviceId, String value, String registeredDeviceCount){
        System.out.println("updatedddd1");
        System.out.println(Integer.parseInt(registeredDeviceCount)+1);
        Document newDevice = new Document("deviceId", new ObjectId(deviceId))
                .append("value", Integer.parseInt(value))
                .append("lastUsed", LocalDate.now().toString())
                .append("registeredAt", LocalDate.now().toString());
        MongoCollection<Document> users = db.getCollection("User");
        MongoCollection<Document> devicedb = db.getCollection("Device");
        return Mono.from(
                users.updateOne(
                    eq("_id", new ObjectId(userId)),
                    Updates.combine(
                        Updates.inc("stats.registeredDeviceCount", 1),
                        Updates.push("registeredDevices", newDevice),
                        set("updatedAt", LocalDateTime.now().toString())
                    )
                ))
                .flatMap(updateResult -> {
                    System.out.println("User updated");
                    System.out.println(updateResult);
                    System.out.println("were going to update the device stats");
                    System.out.println(deviceId);
                    return Mono.from(
                            devicedb.updateOne(
                                    eq("_id", new ObjectId(deviceId)),
                                    Updates.combine(
                                        Updates.inc("stats.registeredUserCount", 1),
                                        set("updatedAt", LocalDateTime.now().toString())
                                    )
                            ));
                })
                .map( doc -> {
                    System.out.println("updatedddd");
                    System.out.println(doc);
                    Map<String, Object> device_data = new HashMap<>();
                    device_data.put("userId", userId);
                    device_data.put("deviceId", deviceId);
                    return device_data;
                })
                .doOnSuccess(list -> System.out.print("Successfully posted"))
                .doOnError(e -> System.out.printf("Error fetching data : "+e));
    }

    public Mono<Map<String, Object>> updateDeleteUsersRegisteredDevice(String userId, String deviceId, String value, String registeredDeviceCount){
        System.out.println("updatedddd1");
        System.out.println(Integer.parseInt(registeredDeviceCount)-1);
        MongoCollection<Document> users = db.getCollection("User");
        MongoCollection<Document> device = db.getCollection("Device");
        return Mono.from(
                        users.updateOne(
                                eq("_id", new ObjectId(userId)),
                                Updates.combine(
                                        Updates.inc("stats.registeredDeviceCount", -1),
                                        //user mungkin seharusnya punya id sendiri
                                        Updates.pull("registeredDevices", new Document("deviceId", new ObjectId(deviceId))),
                                        set("updatedAt", LocalDateTime.now().toString())
                                )
                        ))
                .flatMap(
                    updateResult -> {
                        System.out.println("User updated");
                        System.out.println(updateResult);

                        return Mono.from(
                            device.updateOne(
                                eq("_id", new ObjectId(deviceId)),
                                Updates.combine(
                                    Updates.inc("stats.registeredUserCount", -1),
                                    set("updatedAt", LocalDateTime.now().toString())
                                )
                            ));
                    }
                )
                .map( doc -> {
                    System.out.println("updatedddd");
                    System.out.println(doc);
                    Map<String, Object> device_data = new HashMap<>();
                    device_data.put("userId", userId);
                    device_data.put("deviceId", deviceId);
                    return device_data;
                })
                .doOnSuccess(list -> System.out.print("Successfully posted"))
                .doOnError(e -> System.out.printf("Error fetching data : "+e));
    }

    public Mono<Boolean> userDeviceExists(String userId, String deviceId) {
        MongoCollection<Document> user = db.getCollection("User");
        ObjectId objectId = new ObjectId(userId);
        ObjectId userDeviceobjectId = new ObjectId(deviceId);
        return Mono.from(user.find(
                        and(
                                eq("_id", objectId),
                                eq("registeredDevices.deviceId", userDeviceobjectId)
                        )
                ).first())
                .map(doc -> true)
                .defaultIfEmpty(false);
    }

    public Mono<Map<String, Object>> changeDeviceValue(String userId, String deviceId, int value){
        MongoCollection<Document> users = db.getCollection("User");
        ObjectId userIdObj = new ObjectId(userId);
        ObjectId targetDeviceIdObj = new ObjectId(deviceId);

        Document filter = new Document("_id", userIdObj);
        Document update = new Document("$set", new Document("registeredDevices.$[device].value", value));
        List<Bson> arrayFilters = Arrays.asList(
                Filters.eq("device.deviceId", targetDeviceIdObj)
        );
        UpdateOptions options = new UpdateOptions().arrayFilters(arrayFilters);

        return Mono.from(users.updateOne(filter, update, options))
                .map( doc -> {
                    System.out.println("updatedddd");
                    System.out.println(doc);
                    Map<String, Object> device_data = new HashMap<>();
                    device_data.put("userId", userId);
                    device_data.put("deviceId", deviceId);
                    device_data.put("value", value);
                    return device_data;
                })
                .doOnSuccess(list -> System.out.print("Successfully posted"))
                .doOnError(e -> System.out.printf("Error fetching data : "+e));
    }

}