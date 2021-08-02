package myflink.Analytics.Product_ctr;


import myflink.Env;
import myflink.Date;
import myflink.MapperString;
import myflink.Model.Columns;
import myflink.MongoDB.MongoDBSink_ctr;
import org.apache.flink.api.java.io.TextInputFormat;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.table.api.Table;
import org.apache.flink.types.Row;

import static org.apache.flink.table.api.Expressions.$;

public class Product_ctrYesterday {

    public static DataStream<String> Ds = ReadS3(Date.today29,Date.today28,Date.today);
    public static DataStream<Columns> StreamPd29 = MapperString.MapmeString(Ds);
    public static Table resultPd29 = ComputeTable(StreamPd29);

    public static void main(String[] args) throws Exception {

        DataStream<Row> resultStreamPd29 = TableToStream(resultPd29);
        MongoDBSink_ctr mongoDBSink_ctr=new MongoDBSink_ctr("product_ctrYesterday");
        resultStreamPd29.addSink(mongoDBSink_ctr);

        Env.env.execute("Flink");
    }

    public static DataStream<String> ReadS3 (String today,String yesterday, String tomorrow) {

        TextInputFormat textInputDs1 = new TextInputFormat(new Path(Date.url));
        textInputDs1.setNestedFileEnumeration(true);

        TextInputFormat textInputDs2 = new TextInputFormat(new Path(Date.url));
        textInputDs2.setNestedFileEnumeration(true);

        TextInputFormat textInputDs3 = new TextInputFormat(new Path(Date.url));
        textInputDs3.setNestedFileEnumeration(true);

        DataStream<String> Ds1 = Env.env.readFile(textInputDs1,Date.url + today + "/");
        DataStream<String> Ds2 = Env.env.readFile(textInputDs2,Date.url + yesterday + "/23/");
        DataStream<String> Ds3 = Env.env.readFile(textInputDs3,Date.url + tomorrow + "/00/");
        return Ds1.union(Ds2,Ds3);
    }

    public static Table ComputeTable (DataStream<Columns> n1) {

        Table inputTable1 = Env.tableEnv.fromDataStream(
                n1,$("BRAND"),$("WALL_ID"),
                $("WALLGROUP_ID"),$("CAMPAIGN_ID"),$("EVENT_TYPE")
                ,$("ISOTIMESTAMP").rowtime(),$("PRESTOTIMESTAMP"));
        inputTable1.printSchema();

        Env.tableEnv.createTemporaryView("InputTable1", inputTable1);

        Table resultTable1 = Env.tableEnv.sqlQuery(
                " SELECT BRAND AS BRAND1, WALL_ID AS WALL_ID1, WALLGROUP_ID AS WALLGROUP_ID1, " +
                        "CAMPAIGN_ID AS CAMPAIGN_ID1, EVENT_TYPE AS EVENT_TYPE1,ISOTIMESTAMP AS DATA1, " +
                        " COUNT(EVENT_TYPE = 'product-click') AS CLICKS29 " +
                        " FROM TABLE (TUMBLE(TABLE InputTable1, DESCRIPTOR(ISOTIMESTAMP), INTERVAL '20' MINUTES))"+
                        " WHERE(EVENT_TYPE= 'product-click') " +
                        "AND PRESTOTIMESTAMP BETWEEN '2021-06-30' AND '2021-06-30 24:00:00' "+
                        " GROUP BY window_start, window_end, BRAND, WALL_ID, WALLGROUP_ID, CAMPAIGN_ID, EVENT_TYPE, ISOTIMESTAMP"
        );

        Table resultTable2 = Env.tableEnv.sqlQuery(
                " SELECT BRAND AS BRAND29, WALL_ID AS WALL_ID29 , WALLGROUP_ID AS WALLGROUP_ID29, CAMPAIGN_ID AS CAMPAIGN_ID29, " +
                        "EVENT_TYPE AS EVENT_TYPE29,ISOTIMESTAMP AS DATA29, " +
                        " COUNT(EVENT_TYPE = 'product-impression') AS IMPRESSIONS29 " +
                        " FROM TABLE (TUMBLE(TABLE InputTable1, DESCRIPTOR(ISOTIMESTAMP), INTERVAL '20' MINUTES))"+
                        " WHERE(EVENT_TYPE= 'product-impression') " +
                        " AND PRESTOTIMESTAMP BETWEEN '2021-06-30' AND '2021-06-30 24:00:00' "+
                        " GROUP BY window_start, window_end, BRAND, WALL_ID, WALLGROUP_ID, CAMPAIGN_ID, EVENT_TYPE, ISOTIMESTAMP"
        );
        return resultTable1.join(resultTable2);
    }

    public static DataStream<Row> TableToStream (Table n1){
        return Env.tableEnv.toChangelogStream(n1);
    }
}
