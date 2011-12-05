package com.acmetelecom;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.Duration;

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
