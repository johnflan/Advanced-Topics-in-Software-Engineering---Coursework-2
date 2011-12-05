package com.acmetelecom;

import org.joda.time.DateTime;

public interface iCall {

    public String callee();

    public int durationSeconds();

    public String date();

    public DateTime startTime();

    public DateTime endTime();
    
}
