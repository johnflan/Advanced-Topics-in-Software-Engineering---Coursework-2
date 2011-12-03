package com.acmetelecom;

public class CallEnd extends CallEvent implements iCallEnd {
    public CallEnd(String caller, String callee) {
        super(caller, callee, System.currentTimeMillis());
    }
}
