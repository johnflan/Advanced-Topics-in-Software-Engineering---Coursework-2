package com.acmetelecom;

import java.text.SimpleDateFormat;
import java.util.Date;

public interface iCall {

    public String callee();

    public int durationSeconds();

    public String date();

    public Date startTime();

    public Date endTime();
    
}
