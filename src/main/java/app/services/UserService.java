package app.services;

import com.mongodb.reactivestreams.client.MongoDatabase;
import com.mongodb.reactivestreams.client.MongoCollection;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;

import org.bson.types.ObjectId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;

public class UserService {
    private final MongoDatabase db;

    public UserService(MongoDatabase db) {
        this.db = db;
    }

    public Mono<Map<String, Object>> getActiveUserById(String userId) {
        MongoCollection<Document> user = db.getCollection("User");
        ObjectId objectId = new ObjectId(userId);
        return Mono.from(user.find(eq("_id", objectId)).projection(fields(include("name"))).first())
                .map(doc -> {
                    System.out.println("packing User's data");
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("name", doc.getString("name"));
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
}
