package myflink;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

public class Env {
    public static Configuration conf = new Configuration();
    public static StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(conf);
    public static StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);
}
