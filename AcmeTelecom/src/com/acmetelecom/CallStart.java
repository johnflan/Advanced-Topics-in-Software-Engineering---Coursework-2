package com.acmetelecom;

public class CallStart extends CallEvent implements CallStartInterface {
    public CallStart(String caller, String callee) {
        super(caller, callee, System.currentTimeMillis());
    }
    
    public CallStart(String caller, String callee, long timeStamp) {
        super(caller, callee, timeStamp);
    }
    
}
