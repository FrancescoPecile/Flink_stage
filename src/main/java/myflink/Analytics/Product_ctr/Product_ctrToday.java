package myflink.Analytics.Product_ctr;

import myflink.Date;
import myflink.Env;
import myflink.MapperString;
import myflink.Model.Columns;
import myflink.MongoDB.MongoDBSink_ctr;
import org.apache.flink.api.java.io.TextInputFormat;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.table.api.Table;
import org.apache.flink.types.Row;

import static org.apache.flink.table.api.Expressions.$;

public class Product_ctrToday {

    public static DataStream<String> Ds = ReadS3(Date.today,Date.today29,Date.tomorrow30);
    public static DataStream<Columns> StreamPd30 = MapperString.MapmeString(Ds);
    public static Table resultPd30 = ComputeTable(StreamPd30);

    public static void main(String[] args) throws Exception {

        DataStream<Row> resultStreamPd30 = TableToStream(resultPd30);
        MongoDBSink_ctr mongoDBSink_ctr=new MongoDBSink_ctr("product_ctrTodaySWITCH");
        resultStreamPd30.addSink(mongoDBSink_ctr);

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

        Table inputTable2 = Env.tableEnv.fromDataStream(
                n1,$("BRAND"),$("WALL_ID"),
                $("WALLGROUP_ID"),$("CAMPAIGN_ID"),$("EVENT_TYPE")
                ,$("ISOTIMESTAMP").rowtime(),$("PRESTOTIMESTAMP"));

        Env.tableEnv.createTemporaryView("InputTable2", inputTable2);

        Table resultTable1 = Env.tableEnv.sqlQuery(
                " SELECT BRAND, WALL_ID, WALLGROUP_ID, CAMPAIGN_ID, EVENT_TYPE,ISOTIMESTAMP AS DATA, " +
                        " COUNT(EVENT_TYPE = 'product-click') AS CLICKS30 " +
                        " FROM TABLE (TUMBLE(TABLE InputTable2, DESCRIPTOR(ISOTIMESTAMP), INTERVAL '1' DAY))"+
                        " WHERE(EVENT_TYPE= 'product-click') " +
                        " AND PRESTOTIMESTAMP BETWEEN '2021-06-30' AND '2021-06-30 24:00:00' "+
                        " GROUP BY window_start, window_end, BRAND, WALL_ID, WALLGROUP_ID, CAMPAIGN_ID, EVENT_TYPE, ISOTIMESTAMP"
        );

        Table resultTable2 = Env.tableEnv.sqlQuery(
                " SELECT BRAND AS BRAND30, WALL_ID AS WALL_ID30 , WALLGROUP_ID AS WALLGROUP_ID30, CAMPAIGN_ID AS CAMPAIGN_ID30," +
                        " EVENT_TYPE AS EVENT_TYPE30,ISOTIMESTAMP AS DATA30, " +
                        " COUNT(EVENT_TYPE = 'product-impression') AS IMPRESSIONS30 " +
                        " FROM TABLE (TUMBLE(TABLE InputTable2, DESCRIPTOR(ISOTIMESTAMP), INTERVAL '1' DAY))"+
                        " WHERE(EVENT_TYPE= 'product-impression') " +
                        " AND PRESTOTIMESTAMP BETWEEN '2021-06-30' AND '2021-06-30 24:00:00' "+
                        " GROUP BY window_start, window_end, BRAND, WALL_ID, WALLGROUP_ID, CAMPAIGN_ID, EVENT_TYPE, ISOTIMESTAMP"
        );

        Env.tableEnv.createTemporaryView("resultTable1", resultTable1);
        Env.tableEnv.createTemporaryView("resultTable2", resultTable2);

        return Env.tableEnv.sqlQuery("SELECT * FROM resultTable1 LEFT JOIN resultTable2 ON resultTable2.WALL_ID30 = resultTable1.WALL_ID"
        );

    }

    public static DataStream<Row> TableToStream (Table n1){

        return Env.tableEnv.toChangelogStream(n1);
    }
}
