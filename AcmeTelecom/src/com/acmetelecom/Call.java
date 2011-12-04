package com.acmetelecom;

import java.text.SimpleDateFormat;
import java.util.Date;

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
        return (int) (((end.time() - start.time()) / 1000));
    }

    public String date() {
        return SimpleDateFormat.getInstance().format(new Date(start.time()));
    }

    public Date startTime() {
        return new Date(start.time());
    }

    public Date endTime() {
        return new Date(end.time());
    }
}
