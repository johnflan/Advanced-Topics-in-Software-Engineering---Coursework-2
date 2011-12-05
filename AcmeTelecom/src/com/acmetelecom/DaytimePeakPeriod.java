package com.acmetelecom;

import org.joda.time.DateTime;

class DaytimePeakPeriod {
	
	
	public static int PEAK_RATE_START_TIME;
	public static int OFF_PEAK_RATE_START_TIME;
	

    public boolean offPeak(DateTime time) {  

        int hour = time.getHourOfDay();
        return hour < PEAK_RATE_START_TIME || hour >= OFF_PEAK_RATE_START_TIME;
      }
    
}
