package com.acmetelecom;

import com.acmetelecom.logcall.CallLogInterface;
import com.acmetelecom.logcall.LogCall;

public class LogCallEnd extends LogCall {

	public LogCallEnd(CallLogInterface billingSystem) {
		super(billingSystem);
	}
	
	@Override
	protected void placeCallWithoutTimestamp() {
		callLogger.callCompleted(super.caller, super.callee);

	}

	@Override
	protected void placeCallWithTimestamp() {
		callLogger.callCompleted(super.caller, super.callee, super.timeStamp);
	}

}
