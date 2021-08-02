package myflink.Insights;

import myflink.Analytics.Product_ctr.Product_ctrYesterday;
import myflink.Analytics.Product_ctr.Product_ctrToday;
import myflink.Env;
import myflink.MongoDB.MongoDBSinkWall2Day;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.table.api.Table;
import org.apache.flink.types.Row;

public class Product_ctr2Days {


   public static void main(String[] args) throws Exception {
      Table result29 = Product_ctrYesterday.resultPd29;
      Table result30 = Product_ctrToday.resultPd30;
      Table result29_30 = result29.join(result30);

      Env.tableEnv.createTemporaryView("result29_30", result29_30);

      Table resultQuery29_30 = Env.tableEnv.sqlQuery(
              " SELECT BRAND29,WALL_ID29,WALLGROUP_ID29,CAMPAIGN_ID29,EVENT_TYPE29," +
                      " SUM(IMPRESSIONS30) - SUM(IMPRESSIONS29) AS IMPDIFFERENCE," +
                      " SUM(CLICKS30) - SUM(CLICKS29) AS CLICKDIFFERENCE"+
                      " FROM result29_30" +
                      " WHERE BRAND29 = BRAND30=BRAND=BRAND1 AND WALL_ID29 = WALL_ID30 = WALL_ID1 = WALL_ID" +
                      " AND WALLGROUP_ID29 = WALLGROUP_ID30 = WALLGROUP_ID1 = WALLGROUP_ID" +
                      " AND CAMPAIGN_ID29 = CAMPAIGN_ID30 = CAMPAIGN_ID1 = CAMPAIGN_ID " +
                      "AND EVENT_TYPE29 = EVENT_TYPE30 = EVENT_TYPE1 = EVENT_TYPE "+
                      " GROUP BY BRAND29,WALL_ID29, WALLGROUP_ID29, CAMPAIGN_ID29, EVENT_TYPE29"
      );


      DataStream<Row> resultStream30 = TableToStream(resultQuery29_30);

      MongoDBSinkWall2Day mongoDBSink_2930=new MongoDBSinkWall2Day("product_ctr2930Select");
      resultStream30.addSink(mongoDBSink_2930);

      Env.env.execute("Flink");
   }

   public static DataStream<Row> TableToStream (Table n1){

      return Env.tableEnv.toChangelogStream(n1);
   }

}
