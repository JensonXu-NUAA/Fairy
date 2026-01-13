package cn.nuaa.jensonxu.fairy.integration.mcp.utils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ZoneUtils {

    public static String getTimeByZoneId(String zoneId) {
        ZoneId zid = ZoneId.of(zoneId);  // Get the time zone using ZoneId
        ZonedDateTime zonedDateTime = ZonedDateTime.now(zid);  // Get the current time in this time zone
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");  // Defining a formatter
        return zonedDateTime.format(formatter);
    }

}
