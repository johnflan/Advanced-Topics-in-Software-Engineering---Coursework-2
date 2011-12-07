package com.acmetelecom;

import com.acmetelecom.logcall.LogCall;

public class StartCall extends LogCall{
	
    public StartCall(CallLogInterface billingSystem) {
		super(billingSystem);
	}
    
	@SuppressWarnings("deprecation")
	@Override
	protected void placeCallWithoutTimestamp() {
		callLogger.callInitiated(super.caller, super.callee);
		
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void placeCallWithTimestamp() {
		callLogger.callInitiated(super.caller, super.callee, super.timeStamp);		
	}

	
}
