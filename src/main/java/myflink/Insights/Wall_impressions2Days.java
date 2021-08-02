package myflink.Insights;

import myflink.Env;
import myflink.Analytics.Wall_impressions.Wall_impressionsYesterday;
import myflink.Analytics.Wall_impressions.Wall_impressionsToday;
import myflink.MongoDB.MongoDBSinkWall2Day;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.table.api.Table;
import org.apache.flink.types.Row;

public class Wall_impressions2Days {


   public static void main(String[] args) throws Exception {
      Table result29 = Wall_impressionsYesterday.resultTableYesterday;
      Table result30 = Wall_impressionsToday.resultTableToday;
      Table result29_30 = result29.join(result30);

      Env.tableEnv.createTemporaryView("result29_30", result29_30);

      Table resultQuery29_30 = Env.tableEnv.sqlQuery(
              " SELECT BRAND29,WALL_ID29,WALLGROUP_ID29,CAMPAIGN_ID29,EVENT_TYPE29," +
                      " (100*(SUM(IMPRESSIONS30)-SUM(IMPRESSIONS29))/SUM(IMPRESSIONS29))AS DIFFERENCE"+
                      " FROM result29_30" +
                      " WHERE BRAND29 = BRAND30 AND WALL_ID29 = WALL_ID30 AND WALLGROUP_ID29 = WALLGROUP_ID30" +
                      " AND CAMPAIGN_ID29 = CAMPAIGN_ID30 AND EVENT_TYPE29 = EVENT_TYPE30 "+
                      " GROUP BY BRAND29,WALL_ID29, WALLGROUP_ID29, CAMPAIGN_ID29, EVENT_TYPE29"
      );

      DataStream<Row> resultStream30 = TableToStream(resultQuery29_30);

      MongoDBSinkWall2Day mongoDBSink_2930=new MongoDBSinkWall2Day("wall_impressions2930Select%");
      resultStream30.addSink(mongoDBSink_2930);

      Env.env.execute("Flink");
   }

   public static DataStream<Row> TableToStream (Table n1){

      return Env.tableEnv.toChangelogStream(n1);
   }

}
