package myflink.MongoDB;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.types.Row;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class MongoDBSink_ctr extends RichSinkFunction<Row> {
    private static final long serialVersionUID = 1L;
    MongoClient mongoClient = null;
    String collectionName;

    public MongoDBSink_ctr(String collectionName) {
        this.collectionName=collectionName;
    }

    @Override
    public void invoke(Row value, Context context){

        try {
            if (mongoClient != null) {

                MongoDatabase db = mongoClient.getDatabase("Flink");
                MongoCollection collection = db.getCollection(collectionName);
                
                Object timestamp = value.getField(5);
                assert timestamp instanceof java.time.LocalDateTime;

                List<Document> list = new ArrayList<Document>();
                Document doc = new Document();
                doc.put("BRAND", value.getField(0));
                doc.put("WALL_ID", value.getField(1));
                doc.put("WALLGROUP_ID", value.getField(2));
                doc.put("CAMPAIGN_ID", value.getField(3));
                doc.put("EVENT_TYPE", value.getField(4));
                doc.put("ISOTIMESTAMP",timestamp.toString());
                doc.put("IMPRESSIONS", value.getField(6));
                doc.put("CLICKS", value.getField(13));
                list.add(doc);

                collection.insertMany(list);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        mongoClient=getConnect();
    }

    @Override
    public void close() throws Exception {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    public static MongoClient getConnect(){

        MongoClient mongoClient = new MongoClient("localhost", 27017);

        MongoClientOptions.builder().maxConnectionIdleTime(600000).build();

        return mongoClient;
    }
}
