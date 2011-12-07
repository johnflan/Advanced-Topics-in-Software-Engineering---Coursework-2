package com.acmetelecom;

public class CallEnd extends CallEvent implements CallEndInterface {
    public CallEnd(String caller, String callee) {
        super(caller, callee, System.currentTimeMillis());
    }
    
    public CallEnd(String caller, String callee, long timeStamp) {
        super(caller, callee, timeStamp);
    }
}
