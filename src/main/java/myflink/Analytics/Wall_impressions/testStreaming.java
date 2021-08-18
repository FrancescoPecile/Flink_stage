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
import org.apache.flink.table.api.*;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.descriptors.TableDescriptor;
import org.apache.flink.types.Row;

import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

import static org.apache.flink.table.api.Expressions.*;


public class testStreaming {



    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment()
                .enableCheckpointing(1200,CheckpointingMode.EXACTLY_ONCE);;
        StreamTableEnvironment tEnv = StreamTableEnvironment.create(env);

        TextInputFormat textInputDs1 = new TextInputFormat(new Path(Date.url));
        textInputDs1.setNestedFileEnumeration(true);
        textInputDs1.setCharsetName("UTF-8");
        DataStream<String> ds = env.readFile(textInputDs1, "s3://lsred-analytics/data-json/2021/test/", FileProcessingMode.PROCESS_CONTINUOUSLY, 10000)
                .assignTimestampsAndWatermarks(WatermarkStrategy.forMonotonousTimestamps());;
        DataStream<Columns> Stream = MapperString.MapmeString(ds);

        //FUNZIONA DA QUI PARTE LA TABELLA

        final Schema schema = Schema.newBuilder()
                .column("BRAND", DataTypes.STRING())
                .column("WALL_ID", DataTypes.STRING())
                .column("WALLGROUP_ID", DataTypes.STRING())
                .column("CAMPAIGN_ID", DataTypes.STRING())
                .column("EVENT_TYPE", DataTypes.STRING())
                .column("ISOTIMESTAMP", DataTypes.TIMESTAMP_WITH_LOCAL_TIME_ZONE(3))
                .column("PRESTOTIMESTAMP", DataTypes.STRING())
                .build();

        tEnv.createTemporaryView("CsvSinkTable",Stream,schema);

//        Table inputTable1 = tEnv.fromDataStream(
//                Stream,$("BRAND"),$("WALL_ID"),
//                $("WALLGROUP_ID"),$("CAMPAIGN_ID"),$("EVENT_TYPE")
//                ,$("ISOTIMESTAMP").rowtime(),$("PRESTOTIMESTAMP"));
//
////        GroupWindowedTable windowedTable = inputTable1.window(
////                Tumble.over(lit(10).minutes())
////                        .on($("ISOTIMESTAMP"))
////                        .as("ISOTIMESTA"));
//
//        tEnv.createTemporaryView("InputTable1",inputTable1);

         Table result = tEnv.sqlQuery(
                " SELECT window_start, window_end, BRAND,WALL_ID,WALLGROUP_ID,CAMPAIGN_ID,EVENT_TYPE, " +
//                        " SELECT BRAND,WALL_ID,WALLGROUP_ID,CAMPAIGN_ID,EVENT_TYPE, " +
                        " COUNT(EVENT_TYPE = 'wall-impression') AS IMPRESSIONS30 " +
                        " FROM TABLE (TUMBLE(TABLE CsvSinkTable, DESCRIPTOR(ISOTIMESTAMP), INTERVAL '2' MINUTES))"+
//                        "FROM CsvSinkTable " +
                        " WHERE EVENT_TYPE = 'wall-impression'"+
                        " GROUP BY window_start, window_end, BRAND, WALL_ID, WALLGROUP_ID, CAMPAIGN_ID, EVENT_TYPE"
//                 " GROUP BY  BRAND, WALL_ID, WALLGROUP_ID, CAMPAIGN_ID, EVENT_TYPE"
        );

        DataStream<Row> dsRow = tEnv.toAppendStream(result, Row.class);
//        DataStream<Row> uffa = tEnv.toChangelogStream(result);
//        uffa.print();
        dsRow.print();
//        windowedTable.groupBy("ISOTIMESTA").select().execute().print();
        env.execute("flink");
    }

}
