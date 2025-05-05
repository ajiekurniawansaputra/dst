package app.services;

import com.mongodb.reactivestreams.client.MongoDatabase;
import com.mongodb.reactivestreams.client.MongoCollection;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;

import org.bson.types.ObjectId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

import org.bson.Document;

public class DeviceService {
    private final MongoDatabase db;

    public DeviceService(MongoDatabase db) {
        this.db = db;
    }

    public Mono<List<Map<String, Object>>> getAllDevices(String language) {
        MongoCollection<Document> devices = db.getCollection("Device");
        return Flux.from(devices.find().projection(fields(include("_id","brandName","deviceName","description", "translations"))))
            .map(doc -> {
                System.out.println("Get String");
                String deviceId = doc.getObjectId("_id").toString();
                String brandName = doc.getString("brandName");
                String deviceName = doc.getString("deviceName");
                String description = doc.getString("description");
                Document translations = doc.get("translations", Document.class);
                // API REQUEST TRANSLATION
                /*
                Karena translate API hanya mock kita gunakan document dalam db saja untuk kebutuhan demo,
                Jika ada translate API maka bagian ini diganti API Request.
                */
                System.out.println("Get Trans"); //Fallback to EN
                if (translations != null && translations.containsKey(language)) {
                    System.out.println("Found Trans");
                    Document languageTranslation = translations.get(language, Document.class);
                    deviceName = languageTranslation.getString("deviceName");
                    description = languageTranslation.getString("description");
                }
                System.out.println("Make Map");
                Map<String, Object> device = new HashMap<>();
                device.put("deviceId", deviceId);
                device.put("brandName", brandName);
                device.put("deviceName", deviceName);
                device.put("description", description);
                return device;
            })
            .collectList()
            .doOnSuccess(list -> System.out.printf("Successfully fetched : "+list.size()))
            .doOnError(e -> System.out.printf("Error fetching data : "+e));
    }

    public Mono<List<Map<String, Object>>> getAllVendorDevices(String vendorId) {
        MongoCollection<Document> devices = db.getCollection("Device");
        ObjectId objectId = new ObjectId(vendorId);
        return Flux.from(devices.find(eq("vendorId", objectId)).projection(fields(include("_id","brandName","deviceName","description", "configuration", "stats", "createdAt", "updatedAt"))))
                .map(doc -> {
                    Map<String, Object> device = new HashMap<>();
                    device.put("deviceId", doc.getObjectId("_id").toString());
                    device.put("brandName", doc.getString("brandName"));
                    device.put("deviceName", doc.getString("deviceName"));
                    device.put("description", doc.getString("description"));
                    device.put("configuration", doc.get("configuration", Document.class));
                    device.put("stats", doc.get("stats", Document.class));
                    device.put("createdAt", doc.getString("createdAt"));
                    device.put("updatedAt", doc.getString("updatedAt"));
                    return device;
                })
                .collectList()
                .doOnSuccess(list -> System.out.printf("Successfully fetched : "+list.size()))
                .doOnError(e -> System.out.printf("Error fetching data : "+e));
    }

    public Mono<Object> getAllUserDevices(String userId, String lang){
        MongoCollection<Document> user = db.getCollection("User");
        MongoCollection<Document> device = db.getCollection("Device");

        return Mono.from(user.find(eq("_id", new ObjectId(userId))).first())
                .flatMap(userDocument -> {
                    List<Document> registeredDevices = userDocument.getList("registeredDevices", Document.class);
                    if (registeredDevices == null || registeredDevices.isEmpty()) {
                        return Mono.just(Collections.emptyList());
                    }

                    Map<ObjectId, Integer> deviceValues = registeredDevices.stream()
                            .collect(Collectors.toMap(
                                    rd -> rd.getObjectId("deviceId"),
                                    rd -> rd.getInteger("value")
                            ));

                    List<ObjectId> deviceIds = new ArrayList<>(deviceValues.keySet());

                    return Flux.from(device.find(in("_id", deviceIds)))
                            .map(deviceDocument -> {
                                ObjectId id = deviceDocument.getObjectId("_id");
                                Integer value = deviceValues.get(id);

                                Document conf = deviceDocument.get("configuration", Document.class);
                                Map<String, Object> config = new HashMap<>();
                                config.put("min", conf.get("min"));
                                config.put("max", conf.get("max"));
                                config.put("value", value);

                                Document trans = deviceDocument.get("translations", Document.class);
                                if (trans.containsKey(lang)){
                                    Document translation = trans.get(lang, Document.class);
                                    deviceDocument.put("deviceName",translation.getString("deviceName"));
                                    deviceDocument.put("description",translation.getString("description"));
                                }

                                Map<String, Object> deviceInformation = new HashMap<>();
                                deviceInformation.put("deviceId", id.toString());
                                deviceInformation.put("brandName", deviceDocument.getString("brandName"));
                                deviceInformation.put("deviceName", deviceDocument.getString("deviceName"));
                                deviceInformation.put("description", deviceDocument.getString("description"));
                                deviceInformation.put("configuration", config);

                                return deviceInformation;
                            })
                            .collectList();
                });
    }
}
