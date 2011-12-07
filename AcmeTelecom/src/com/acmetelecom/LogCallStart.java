package com.acmetelecom;

import com.acmetelecom.logcall.CallLogInterface;
import com.acmetelecom.logcall.LogCall;

public class LogCallStart extends LogCall{
	
    public LogCallStart(CallLogInterface billingSystem) {
		super(billingSystem);
	}
    
	@Override
	protected void placeCallWithoutTimestamp() {
		callLogger.callInitiated(super.caller, super.callee);
		
	}

	@Override
	protected void placeCallWithTimestamp() {
		callLogger.callInitiated(super.caller, super.callee, super.timeStamp);		
	}	
}
