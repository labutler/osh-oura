package com.sample.impl.sensor.drivername;

import org.sensorhub.api.config.DisplayInfo;

import java.util.Date;
import java.util.Calendar;


public class DataFilter {

    @DisplayInfo(desc="Minimum time stamp of requested data")
    public Date startTime = getStartTime();

    @DisplayInfo(desc="Maximum time stamp of requested data")
    public Date endTime = new Date();

    public Date getStartTime(){
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -14);
        return cal.getTime();
    }
}