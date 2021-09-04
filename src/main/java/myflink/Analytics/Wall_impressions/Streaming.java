package myflink.Analytics.Wall_impressions;

import myflink.Date;
import myflink.MapperString;
import myflink.Model.Columns;
import myflink.MongoDB.MongoDBSinkWall;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.java.io.TextInputFormat;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.FileProcessingMode;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.runtime.operators.window.assigners.TumblingWindowAssigner;
import org.apache.flink.types.Row;

import java.time.Duration;
import java.time.ZoneId;

import static org.apache.flink.table.api.Expressions.$;

public class Streaming {

    public static StreamExecutionEnvironment streamEnv = StreamExecutionEnvironment.
            getExecutionEnvironment().enableCheckpointing(1200,CheckpointingMode.EXACTLY_ONCE);

    public static StreamTableEnvironment StreamTableEnv = StreamTableEnvironment.create(streamEnv);

    public static void main(String[] args) throws Exception {

        StreamTableEnv.getConfig().setLocalTimeZone(ZoneId.of("Europe/Berlin"));

        DataStream<String> Ds = ReadS3();
        DataStream<Columns> Stream = MapperString.MapmeString(Ds);

        WatermarkStrategy<Columns> wmStrategy = WatermarkStrategy
                .<Columns>forMonotonousTimestamps()
                .withTimestampAssigner((event, timestamp) -> event.ISOTIMESTAMP.getTime());

        DataStream<Columns> DTimestamp = Stream.assignTimestampsAndWatermarks(wmStrategy);

//        DTimestamp.windowAll(TumblingEventTimeWindows.of(Time.days(1)));
        Table resultTableToday = ComputeTable(Stream);
        DataStream<Row> resultStream30 = TableToStream(resultTableToday);
        MongoDBSinkWall mongoDBSink=new MongoDBSinkWall("streamingPLEASE");
        resultStream30.addSink(mongoDBSink);

        streamEnv.execute("flink");
    }

    public static DataStream<String> ReadS3 () {

        TextInputFormat textInputDs1 = new TextInputFormat(new Path(Date.url));
        textInputDs1.setNestedFileEnumeration(true);
        return streamEnv.readFile(textInputDs1,"s3://lsred-analytics/data-json/2021/06/30/01/",FileProcessingMode.PROCESS_CONTINUOUSLY,10000);
//                .assignTimestampsAndWatermarks(WatermarkStrategy.forBoundedOutOfOrderness(Duration.ofSeconds(20)));
    }

    public static Table ComputeTable (DataStream<Columns> dataStream) {

        Table inputTable1 = StreamTableEnv.fromDataStream(
                dataStream,$("BRAND"),$("WALL_ID"),
                $("WALLGROUP_ID"),$("CAMPAIGN_ID"),$("EVENT_TYPE"),
                $("ISOTIMESTAMP").rowtime());

        StreamTableEnv.createTemporaryView("InputTable1", inputTable1);

        return StreamTableEnv.sqlQuery(
                " SELECT BRAND,WALL_ID,WALLGROUP_ID,CAMPAIGN_ID,EVENT_TYPE,ISOTIMESTAMP AS DATA, " +
                        " COUNT(EVENT_TYPE = 'wall-impression') AS IMPRESSIONS30 " +
                        " FROM TABLE (TUMBLE(TABLE InputTable1, DESCRIPTOR(ISOTIMESTAMP), INTERVAL '10' DAYS))"+
                        " WHERE EVENT_TYPE = 'wall-impression'"+
                        " GROUP BY window_start, window_end,BRAND, WALL_ID, WALLGROUP_ID, CAMPAIGN_ID, EVENT_TYPE,ISOTIMESTAMP");
    }

    public static DataStream<Row> TableToStream (Table dsRow){
        return StreamTableEnv.toChangelogStream(dsRow);
    }
}