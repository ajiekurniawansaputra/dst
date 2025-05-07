package app.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.mongodb.reactivestreams.client.MongoCollection;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;

import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
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
                    device.put("createdAt", doc.getDate("createdAt"));
                    device.put("updatedAt", doc.getDate("updatedAt"));
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

    public Mono<List<Map<String, Object>>> getAllAdminDevices() {
        MongoCollection<Document> devices = db.getCollection("Device");
        return Flux.from(devices.find())
                .map(doc -> {
                    System.out.println("Get String");
                    Map<String, Object> device = new HashMap<>();
                    device.put("deviceId", doc.getObjectId("_id").toString());
                    device.put("vendorId", doc.getObjectId("vendorId").toString());
                    device.put("brandName", doc.getString("brandName"));
                    device.put("deviceName", doc.getString("deviceName"));
                    device.put("description", doc.getString("description"));
                    device.put("configuration", doc.get("configuration", Document.class));
                    device.put("stats", doc.get("stats", Document.class));
                    device.put("updatedAt", doc.getDate("updatedAt"));
                    device.put("createdAt", doc.getDate("createdAt"));
                    return device;
                })
                .collectList()
                .doOnSuccess(list -> System.out.printf("Successfully fetched : "+list.size()))
                .doOnError(e -> System.out.printf("Error fetching data : "+e));
    }

    public Mono<Map<String, Object>> postOneDevice(Map<String,Object> bodymap, String vendorId) throws JsonProcessingException {
        MongoCollection<Document> devices = db.getCollection("Device");
        System.out.println("service post one");
        Map<String, Object> configurationMap = (Map<String, Object>) bodymap.get("configuration");

        System.out.println("service post two");
        System.out.println(vendorId);
        Document stats = new Document("registeredUserCount", 0);
        Document configuration = new Document("min", configurationMap.get("min"))
                .append("max", configurationMap.get("max"))
                .append("default", configurationMap.get("default"));
        Document document = new Document("vendorId", new ObjectId(vendorId))
                .append("brandName", bodymap.get("brandName"))
                .append("deviceName", bodymap.get("deviceName"))
                .append("description", bodymap.get("description"))
                .append("configuration", configuration)
                .append("stats", stats)
                .append("translations", empty())
                .append("createdAt", LocalDateTime.now())
                .append("updatedAt", LocalDateTime.now());
        System.out.println("service post three");
        return Mono.from(devices.insertOne(document))
                .map( doc -> {
                    Map<String, Object> device = new HashMap<>();
                    device.put("deviceId", doc.getInsertedId().asObjectId().getValue().toHexString());
                    device.put("brandName", bodymap.get("brandName"));
                    device.put("deviceName", bodymap.get("deviceName"));
                    device.put("description", bodymap.get("description"));
                    return device;
                })
                .doOnSuccess(list -> System.out.print("Successfully posted"))
                .doOnError(e -> System.out.printf("Error fetching data : "+e));
    }

    public Mono<Map<String, Object>> getOneDevice(String deviceId){
        MongoCollection<Document> device = db.getCollection("Device");
        ObjectId objectId = new ObjectId(deviceId);
        return Mono.from(device.find(eq("_id", objectId)).projection(fields(include("stats", "deviceName", "configuration"))).first())
                .map(doc -> {
                    System.out.println("packing Device's data");
                    Map<String, Object> deviceMap = new HashMap<>();
                    deviceMap.put("deviceName", doc.getString("deviceName"));
                    deviceMap.put("stats", doc.get("stats", Document.class));
                    deviceMap.put("configuration", doc.get("configuration", Document.class));
                    System.out.println("doc");System.out.println(doc);
                    return deviceMap;
                })
                .defaultIfEmpty(new HashMap<>())
                .doOnSuccess(map -> System.out.print("Successfully fetched"+map))
                .doOnError(e -> System.out.printf("Error fetching data : "+e));
    }

    public Mono<Map<String, Object>> deleteOneDevice(String deviceId, String vendorId){
        MongoCollection<Document> device = db.getCollection("Device");
        ObjectId deviceIddObj = new ObjectId(deviceId);
        ObjectId vendorIddObj = new ObjectId(vendorId);

        Document filter = new Document("_id", deviceIddObj)
                .append("stats.registeredUserCount", 0)
                .append("vendorId", vendorIddObj);

        return Mono.from(device.deleteOne(filter))
                .flatMap(result -> {
                    if (result.getDeletedCount() > 0) {
                        Map<String, Object> deviceMap = new HashMap<>();
                        deviceMap.put("deviceId", deviceId);
                        return Mono.just(deviceMap);
                    } else {
                        return Mono.error(new IllegalStateException(
                                "Device could not be deleted. It may have registered users or owned by other vendor."
                        ));
                    }
                })
                .doOnSuccess(map -> System.out.print("Successfully fetched"+map))
                .doOnError(e -> System.out.printf("Error fetching data : "+e));
    }

    public Mono<Map<String, Object>> updateDevice(Map<String,Object> bodymap, String deviceId, String vendorId){
        MongoCollection<Document> devices = db.getCollection("Device");
        Map<String, Object> configurationMap = (Map<String, Object>) bodymap.get("configuration");
        ObjectId deviceIddObj = new ObjectId(deviceId);

        Document filter = new Document("_id", deviceIddObj)
                .append("vendorId",new ObjectId(vendorId));
        Document setFields = new Document();

        // Top-level optional fields
        if (bodymap.containsKey("brandName")) {
            setFields.append("brandName", bodymap.get("brandName"));
        }
        if (bodymap.containsKey("deviceName")) {
            setFields.append("deviceName", bodymap.get("deviceName"));
        }
        if (bodymap.containsKey("description")) {
            setFields.append("description", bodymap.get("description"));
        }
        if (configurationMap != null) {
            if (configurationMap.containsKey("min")) {
                setFields.append("configuration.min", configurationMap.get("min"));
            }
            if (configurationMap.containsKey("max")) {
                setFields.append("configuration.max", configurationMap.get("max"));
            }
            if (configurationMap.containsKey("default")) {
                setFields.append("configuration.default", configurationMap.get("default"));
            }
        }
        setFields.append("updatedAt", LocalDateTime.now());

        Document update = new Document("$set", setFields);
        return Mono.from(devices.updateOne(filter, update))
                .flatMap( doc -> {
                    if (doc.getMatchedCount() > 0) {
                        Map<String, Object> deviceData = new HashMap<>();
                        deviceData.put("deviceId", deviceId);
                        return Mono.just(deviceData);
                    } else {
                        return Mono.error(new IllegalStateException(
                                "You are not authorized update this device, only device's vendor can update device's data."
                        ));
                    }
                })
                .doOnSuccess(list -> System.out.print("Successfully posted"))
                .doOnError(e -> System.out.printf("Error fetching data : "+e));
    }
}
