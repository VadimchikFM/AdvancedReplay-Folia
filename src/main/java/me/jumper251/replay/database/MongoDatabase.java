package me.jumper251.replay.database;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import me.jumper251.replay.database.utils.Database;
import me.jumper251.replay.database.utils.DatabaseService;
import me.jumper251.replay.utils.LogUtils;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class MongoDatabase extends Database {
    private final String connectionString;
    private final MongoService service;
    private MongoClient client;

    public MongoDatabase(String host, int port, String database, String collectionName, String user, String password) {
        super(host, port, database, user, password);
        String connectionString1 = "mongodb://";
        if (user == null || user.isEmpty()) connectionString1 = connectionString1 + host + ":" + port;
        else if (password == null || password.isEmpty())
            connectionString1 = connectionString1 + user + "@" + host + ":" + port;
        else
            connectionString1 = connectionString1 + user + ":" + password + "@" + host + ":" + port;

        connectionString = connectionString1;

        connect();

        service = new MongoService(this, collectionName);
    }

    @Override
    public void connect() {
        if (client != null) {
            client.close();
            client = null;
        }
        client = MongoClients.create(connectionString);
        try {
            getDB();
            LogUtils.log("Successfully conntected to MongoDB");
        } catch (Exception e) {
            disconnect();
            LogUtils.log("Failed to connect to MongoDB: " + e.getMessage());
        }
    }

    public com.mongodb.client.MongoDatabase getDB() {
        return client.getDatabase(database);
    }

    public void update(String collectionName, Document query, Document updateDoc) {
        try {
            com.mongodb.client.MongoDatabase database = getDB();
            MongoCollection<Document> collection = database.getCollection(collectionName);

            collection.findOneAndUpdate(
                    query,
                    new Document("$set", updateDoc),
                    new FindOneAndUpdateOptions().upsert(true)
            );
        } catch (Exception e) {
            System.err.println(e);
        }
    }


    public Document queryOne(String collectionName, Document query) {
        try {
            com.mongodb.client.MongoDatabase database = getDB();
            MongoCollection<Document> collection = database.getCollection(collectionName);

            return collection.find(query).first();
        } catch (Exception e) {
            System.err.println(e);
        }
        return null;
    }

    public List<Document> queryMany(String collectionName, Document query) {
        List<Document> resultList = new ArrayList<>();
        try {
            com.mongodb.client.MongoDatabase database = getDB();
            MongoCollection<Document> collection = database.getCollection(collectionName);

            collection.find(query).into(resultList);
        } catch (Exception e) {
            System.err.println(e);
        }
        return resultList;
    }


    @Override
    public void disconnect() {
        if (client != null) {
            client.close();
            client = null;
        }
    }

    @Override
    public DatabaseService getService() {
        return service;
    }

    @Override
    public String getDataSourceName() {
        throw new UnsupportedOperationException();
    }
}
