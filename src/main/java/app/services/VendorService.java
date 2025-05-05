package app.services;

import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;

public class VendorService {
    private final MongoDatabase db;

    public VendorService(MongoDatabase db) {
        this.db = db;
    }
    
    public Mono<Map<String, Object>> getVendorById(String vendorId) {
        MongoCollection<Document> vendor = db.getCollection("Vendor");
        ObjectId objectId = new ObjectId(vendorId);
        return Mono.from(vendor.find(eq("_id", objectId)).projection(fields(include("name"))).first())
                .map(doc -> {
                    System.out.println("packing vendor's data");
                    String vendorName = doc.getString("name");
                    Map<String, Object> vendorMap = new HashMap<>();
                    vendorMap.put("vendorName", vendorName);
                    return vendorMap;
                })
                .defaultIfEmpty(new HashMap<>())
                .doOnSuccess(map -> System.out.print("Successfully fetched"+map))
                .doOnError(e -> System.out.printf("Error fetching data : "+e));
    }
}