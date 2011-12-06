package com.acmetelecom;

import com.acmetelecom.logcall.LogCall;

public class StartCall extends LogCall{
	
    public StartCall(BillingSystem billingSystem) {
		super(billingSystem);
	}

	@Override
	protected void placeCallWithoutTimestamp() {
		billingSystem.callInitiated(super.caller, super.callee);
		
	}

	@Override
	protected void placeCallWithTimestamp() {
		billingSystem.callInitiated(super.caller, super.callee, super.timeStamp);		
	}

	
}
