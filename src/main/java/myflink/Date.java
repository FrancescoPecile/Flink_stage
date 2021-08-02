package myflink;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Date {
    public static DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    public static String url = "s3a://lsred-analytics/data-json/";

    public static LocalDate AddDay(LocalDate n1) {
        return n1.plusDays(1);
    }
    public static LocalDate SubtractDay(LocalDate n1) {
        return n1.minusDays(1);
    }
    public static String dateFormatter(LocalDate n1){
        return n1.format(myFormatObj);
    }

    public static LocalDate today_date = LocalDate.of(2021, 6, 30);
    public static LocalDate today_date29 = SubtractDay(today_date);
    public static LocalDate today_date28 = SubtractDay(today_date29);
    public static LocalDate today_date27 = SubtractDay(today_date28);
    public static LocalDate today_date26 = SubtractDay(today_date27);
    public static LocalDate tomorrow_date30 = AddDay(today_date);

    public static String today = dateFormatter(today_date);
    public static String today29 = dateFormatter(today_date29);
    public static String today28 = dateFormatter(today_date28);
    public static String today27 = dateFormatter(today_date27);
    public static String today26 = dateFormatter(today_date26);
    public static String tomorrow30 = dateFormatter(tomorrow_date30);
}
