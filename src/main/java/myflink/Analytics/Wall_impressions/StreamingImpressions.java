package myflink.Analytics.Wall_impressions;

import myflink.Date;
import myflink.Env;
import myflink.MapperString;
import myflink.Model.Columns;
import myflink.MongoDB.MongoDBSinkWall;
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.io.FilePathFilter;
import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.api.java.io.TextInputFormat;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy;
import org.apache.flink.streaming.api.functions.source.FileProcessingMode;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.table.api.GroupWindowedTable;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.Tumble;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.types.Row;

import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

import static org.apache.flink.table.api.Expressions.*;

public class StreamingImpressions {

    public static StreamExecutionEnvironment streamEnv = StreamExecutionEnvironment.
            getExecutionEnvironment().enableCheckpointing(1200,CheckpointingMode.EXACTLY_ONCE);

    public static StreamTableEnvironment StreamTableEnv = StreamTableEnvironment.create(streamEnv);

    public static void main(String[] args) throws Exception {
        streamEnv.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
        StreamTableEnv.getConfig().setLocalTimeZone(ZoneId.of("Europe/Berlin"));

        DataStream<String> Ds = ReadS3();
        DataStream<Columns> Stream = MapperString.MapmeString(Ds);
        Table resultTableToday = ComputeTable(Stream);

//        KeyedStream<Columns,String> keyed = Stream.keyBy(Columns::getBRAND);
//        keyed.window(TumblingProcessingTimeWindows.of(Time.minutes(20)));
//        Table resultTableToday = ComputeTable(keyed);

        DataStream<Row> resultStream30 = TableToStream(resultTableToday);

//        final StreamingFileSink<Row> sink = StreamingFileSink
//                .forRowFormat(new Path("mongodb://127.0.0.1/Flink.streaming"), new SimpleStringEncoder<Row>("UTF-8"))
//                .withRollingPolicy(
//                        DefaultRollingPolicy.builder()
//                                .withRolloverInterval(TimeUnit.MINUTES.toMillis(15))
//                                .withInactivityInterval(TimeUnit.MINUTES.toMillis(5))
//                                .withMaxPartSize(1024 * 1024 * 1024)
//                                .build())
//                .build();
//
//        resultStream30.addSink(sink);
//        MongoDBSinkWall mongoDBSink=new MongoDBSinkWall("streamME");
//        resultStream30.addSink(mongoDBSink);

        resultStream30.print();
       streamEnv.execute("flink");
//       .setRuntimeMode(RuntimeExecutionMode.STREAMING)
    }

    public static DataStream<String> ReadS3 () {

        TextInputFormat textInputDs1 = new TextInputFormat(new Path(Date.url));
        textInputDs1.setNestedFileEnumeration(true);
        return streamEnv.readFile(textInputDs1,"s3://lsred-analytics/data-json/2021/test/",FileProcessingMode.PROCESS_CONTINUOUSLY,10000);
//        .assignTimestampsAndWatermarks(WatermarkStrategy.forMonotonousTimestamps());
    }

    public static Table ComputeTable (DataStream<Columns> dataStream) {

        Table inputTable1 = StreamTableEnv.fromDataStream(
                dataStream,$("BRAND"),$("WALL_ID"),
                $("WALLGROUP_ID"),$("CAMPAIGN_ID"),$("EVENT_TYPE")
                ,$("ISOTIMESTAMP").rowtime(),$("PRESTOTIMESTAMP"));

        StreamTableEnv.createTemporaryView("InputTable1", inputTable1);

        return StreamTableEnv.sqlQuery(
                " SELECT window_start, window_end, BRAND,WALL_ID,WALLGROUP_ID,CAMPAIGN_ID,EVENT_TYPE, " +
                        " COUNT(EVENT_TYPE = 'wall-impression') AS IMPRESSIONS30 " +
                        " FROM TABLE (TUMBLE(TABLE InputTable1, DESCRIPTOR(ISOTIMESTAMP), INTERVAL '2' MINUTES))"+
                        " WHERE EVENT_TYPE = 'wall-impression'"+
                        " GROUP BY window_start, window_end, BRAND, WALL_ID, WALLGROUP_ID, CAMPAIGN_ID, EVENT_TYPE"
        );
    }

    public static DataStream<Row> TableToStream (Table dsRow){
        return StreamTableEnv.toChangelogStream(dsRow);
    }
}
