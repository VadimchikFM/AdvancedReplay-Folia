package me.jumper251.replay.database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import me.jumper251.replay.database.utils.DatabaseService;
import me.jumper251.replay.replaysystem.data.CompressionData;
import me.jumper251.replay.replaysystem.data.ReplayInfo;
import org.bson.Document;
import org.bson.types.Binary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MongoService extends DatabaseService {
    private final MongoDatabase database;
    private final String table;

    public MongoService(MongoDatabase database, String collectionName) {
        this.database = database;
        this.table = collectionName;

        this.database.getDB().getCollection(table).createIndex(Indexes.ascending("id"), new IndexOptions().unique(true));
    }

    @Override
    public void createReplayTable() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void addReplay(String id, String creator, int duration, int compression, Long time, byte[] data) {
        Document query = new Document("id", id);
        Document updateDoc = new Document()
                .append("creator", creator)
                .append("duration", duration)
                .append("compression", compression)
                .append("time", time)
                .append("data", data);

        pool.execute(() -> database.update(this.table, query, updateDoc));
    }

    @Override
    public byte[] getReplayData(String id) {
        try {
            Document query = new Document("id", id);
            Document result = database.queryOne(this.table, query);

            if (result != null) {
                Binary binaryData = result.get("data", Binary.class);
                return binaryData != null ? binaryData.getData() : null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void deleteReplay(String id) {
        try {
            Document query = new Document("id", id);

            pool.execute(() -> {
                try {
                    com.mongodb.client.MongoDatabase db = database.getDB();
                    MongoCollection<Document> collection = db.getCollection(this.table);
                    collection.deleteOne(query);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean exists(String id) {
        try {
            com.mongodb.client.MongoDatabase db = database.getDB();
            MongoCollection<Document> collection = db.getCollection(this.table);

            Document query = new Document("id", id);
            long count = collection.countDocuments(query);

            return count > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    @Override
    public List<ReplayInfo> getReplays() {
        List<ReplayInfo> replays = new ArrayList<>();
        try {
            com.mongodb.client.MongoDatabase db = database.getDB();
            MongoCollection<Document> collection = db.getCollection(this.table);

            for (Document doc : collection.find().projection(new Document("id", 1)
                    .append("creator", 1)
                    .append("duration", 1)
                    .append("time", 1)
                    .append("compression", 1)
                    .append("_id", 0))) {
                String id = doc.getString("id");
                String creator = doc.getString("creator");
                int compression = doc.getInteger("compression");
                int duration = doc.getInteger("duration", 0);
                long time = doc.getLong("time");

                replays.add(new ReplayInfo(id, creator, time, compression, duration));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return replays;
    }

}
