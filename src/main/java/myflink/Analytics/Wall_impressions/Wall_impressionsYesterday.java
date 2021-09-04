package myflink.Analytics.Wall_impressions;

import myflink.Env;
import myflink.MapperString;
import myflink.Model.Columns;
import myflink.MongoDB.MongoDBSinkWall;
import org.apache.flink.api.java.io.TextInputFormat;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.table.api.Table;
import org.apache.flink.types.Row;
import myflink.Date;
//TUMBLING WINDOWS CON TIMESTAMP ROWTIME
import static org.apache.flink.table.api.Expressions.$;

public class Wall_impressionsYesterday {

    public static DataStream<String> Ds = ReadS3(Date.today29,Date.today28,Date.today);
    public static DataStream<Columns> Stream = MapperString.MapmeString(Ds);
    public static Table resultTableYesterday = ComputeTable(Stream);

    public static void main(String[] args) throws Exception {
        DataStream<Row> resultStream29 = TableToStream(resultTableYesterday);
        MongoDBSinkWall mongoDBSink = new MongoDBSinkWall("wall_impressionsYesterday");
        resultStream29.addSink(mongoDBSink);

        Env.env.execute("Flink");
    }

    public static DataStream<String> ReadS3 (String today, String yesterday, String tomorrow) {

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

    public static DataStream<Row> TableToStream (Table dsRow){

        return Env.tableEnv.toChangelogStream(dsRow);
    }

    public static Table ComputeTable (DataStream<Columns> dataStream) {

        Table inputTable2 = Env.tableEnv.fromDataStream(
                dataStream,$("BRAND"),$("WALL_ID"),
                $("WALLGROUP_ID"),$("CAMPAIGN_ID"),$("EVENT_TYPE")
                ,$("ISOTIMESTAMP").rowtime(),$("PRESTOTIMESTAMP"));

        Env.tableEnv.createTemporaryView("InputTable2", inputTable2);

        return Env.tableEnv.sqlQuery(
                " SELECT BRAND AS BRAND29, WALL_ID AS WALL_ID29, WALLGROUP_ID AS WALLGROUP_ID29 " +
                        ", CAMPAIGN_ID AS CAMPAIGN_ID29, EVENT_TYPE AS EVENT_TYPE29 ,ISOTIMESTAMP AS DATA29, " +
                        " COUNT(EVENT_TYPE = 'wall-impression') AS IMPRESSIONS29 " +
                        " FROM TABLE (TUMBLE(TABLE InputTable2, DESCRIPTOR(ISOTIMESTAMP), INTERVAL '20' MINUTES))"+
                        " WHERE EVENT_TYPE = 'wall-impression' AND PRESTOTIMESTAMP BETWEEN '2021-06-29' AND '2021-06-29 24:00:00' "+
                        " GROUP BY window_start, window_end, BRAND, WALL_ID, WALLGROUP_ID, CAMPAIGN_ID, EVENT_TYPE, ISOTIMESTAMP"
        );
    }
}

