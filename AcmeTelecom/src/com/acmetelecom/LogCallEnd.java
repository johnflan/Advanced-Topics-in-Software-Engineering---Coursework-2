package com.acmetelecom;

import com.acmetelecom.logcall.CallLogInterface;
import com.acmetelecom.logcall.LogCall;

public class LogCallEnd extends LogCall {

	public LogCallEnd(CallLogInterface billingSystem) {
		super(billingSystem);
	}
	@SuppressWarnings("deprecation")
	@Override
	protected void placeCallWithoutTimestamp() {
		callLogger.callCompleted(super.caller, super.callee);

	}

	@SuppressWarnings("deprecation")
	@Override
	protected void placeCallWithTimestamp() {
		callLogger.callCompleted(super.caller, super.callee, super.timeStamp);
	}

}
