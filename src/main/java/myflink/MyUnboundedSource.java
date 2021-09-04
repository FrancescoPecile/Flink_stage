//package myflink;
//
//import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
//import org.apache.flink.streaming.api.scala.DataStream;
//import org.apache.flink.types.Row;
//
//public class MyUnboundedSource extends RichParallelSourceFunction<DataStream<Row>> {
//
//    private boolean running;
//
//    @Override
//    public void run(SourceContext<DataStream<Row>> ctx) throws Exception {
//        while (running) {
//            // Call some method that returns the next record, if available.
//            DataStream<Row> record = getNextRecordOrNull();
//            if (record != null) {
//                ctx.collect(record);
//            } else {
//                Thread.sleep(100);
//            }
//        }
//    }
//
//    private DataStream<Row> getNextRecordOrNull() {
//
//    }
//
//    @Override
//    public void cancel() {
//        running = false;
//    }
//}
