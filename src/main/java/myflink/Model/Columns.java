package myflink.Model;

import java.sql.Timestamp;

public class Columns
{
    public String BRAND;
    public String WALL_ID;
    public String WALLGROUP_ID;
    public String CAMPAIGN_ID;
    public String EVENT_TYPE;
    public Timestamp ISOTIMESTAMP;
    public String PRESTOTIMESTAMP;

    public String getBRAND() { return BRAND; }
    @Override
    public String toString(){
        return BRAND  + WALL_ID +
                WALLGROUP_ID +  CAMPAIGN_ID +
                EVENT_TYPE + ISOTIMESTAMP + PRESTOTIMESTAMP ;
    }

    public Columns() {}
}