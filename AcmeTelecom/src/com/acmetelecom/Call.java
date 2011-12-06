package com.acmetelecom;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeParser;
import org.joda.time.format.DateTimePrinter;

public class Call implements iCall{

    private static final DateTimePrinter DateTimePrinter = null;
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
  		DateTime peakStart=new DateTime(startTime().withTime(DaytimePeakPeriod.PEAK_RATE_START_TIME, 0,0,0));
  		DateTime peakEnd=new DateTime(startTime().withTime(DaytimePeakPeriod.OFF_PEAK_RATE_START_TIME,0,0,0));
  		Interval callInterval;
  		if (startTime().isBefore(endTime()))
  			callInterval=new Interval(startTime(), endTime());
  		else return 0;
  		long peakSeconds=0;
  		long daysBetween=new Duration(startTime(), endTime()).getStandardDays();
  		
  		for (int i=0;i<=daysBetween;i++){
  			Interval peakHours=new Interval(peakStart.plusDays(i),peakEnd.plusDays(i));
  			Interval overlapWithPeak = callInterval.overlap(peakHours);
  			if (overlapWithPeak != null)
  				peakSeconds+=overlapWithPeak.toDuration().getStandardSeconds();
  		}
     	return peakSeconds;
    }
    
    public long durationOffPeakSeconds() { 	
    	long between=0;
    	if (startTime().isBefore(endTime()))
    		between=new Duration(startTime(),endTime()).getStandardSeconds();
    	return between-durationPeakSeconds();
    }

    public String date() {
    	DateTimeFormatter fmt=DateTimeFormat.shortDateTime();
    	return fmt.print(new DateTime(start.time()));
        //return SimpleDateFormat.getInstance().format(new Date(start.time()));
    }

    public DateTime startTime() {
        return new DateTime(start.time());
    }

    public DateTime endTime() {
        return new DateTime(end.time());
    }
}
