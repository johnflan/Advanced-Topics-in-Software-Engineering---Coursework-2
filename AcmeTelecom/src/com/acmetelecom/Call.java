package com.acmetelecom;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;

public class Call implements iCall{

    private iCallStart start;
    private iCallEnd end;

    public Call(iCallStart start, iCallEnd end) {
        this.start = start;
        this.end = end;
    }
    
    
    public String callee() {
        return start.getCallee();
    }

    public int durationSeconds() {
        return (int) new Duration(new DateTime(start.time()), new DateTime(end.time())).getStandardSeconds();
    }
    
    public long durationPeakSeconds() {
    	
    	DateTime startTime=startTime();
    	DateTime endTime=endTime();
    	System.out.println(startTime.toString());
    	System.out.println(endTime.toString());
  		DateTime peakStart=new DateTime(startTime.withTime(DaytimePeakPeriod.PEAK_RATE_START_TIME, 0,0,0));
  		DateTime peakEnd=new DateTime(startTime.withTime(DaytimePeakPeriod.OFF_PEAK_RATE_START_TIME,0,0,0));
  		Interval startToEndInterval=new Interval(startTime, endTime);
  		long peakSeconds=0;
  		long daysBetween=new Duration(startTime, endTime).getStandardDays();
  		for (int i=0;i<=daysBetween;i++){
//  			System.out.println(i);
  			Interval peakHours=new Interval(peakStart.plusDays(i),peakEnd.plusDays(i));
  			
  			
  			Interval interval = startToEndInterval.overlap(peakHours);
  			if (interval != null){
  				Duration dur = interval.toDuration();
  				peakSeconds+=dur.getStandardSeconds();
  			}

//	  			System.out.println("Overlap: "+startToEndInterval.overlap(peakHours));
//	  			System.out.println("Duration: "+dur);
  			
  		
  		}

     	return peakSeconds;
    }
    
    public long durationOffPeakSeconds() { 	
   	  long between=new Duration(startTime(),endTime()).getStandardSeconds();
    	return between-durationPeakSeconds();
    }

    public String date() {
        return SimpleDateFormat.getInstance().format(new Date(start.time()));
    }

    public DateTime startTime() {
        return new DateTime(start.time());
    }

    public DateTime endTime() {
        return new DateTime(end.time());
    }
}
