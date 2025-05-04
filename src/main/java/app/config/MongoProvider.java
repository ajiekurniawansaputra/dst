package app.config;

public class MongoProvider {
    private static final String dbUrl = "mongodb+srv://DemoSmartThingsAccess:ytmxfPnuUraCSYpv@demosmartthings.gfo2hjx.mongodb.net/?retryWrites=true&w=majority&appName=DemoSmartThings";
    private static final String dbName = "DemoSmartThingsDB";

    public static String getDBUrl() {
        return dbUrl;
    }

    public static String getDBName() {
        return dbName;
    }
}
