package myflink;

import com.google.gson.Gson;
import myflink.Model.Columns;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.streaming.api.datastream.DataStream;

import java.time.Duration;

public class MapperString {
    public static DataStream<Columns> MapmeString (DataStream<String> n1){
        DataStream<Columns> Data =  n1.map((MapFunction<String, Columns>) s -> {
            Gson gson = new Gson();
            Columns columns =  gson.fromJson(s,Columns.class);
            return columns;
        });
        return Data;
    }
}
