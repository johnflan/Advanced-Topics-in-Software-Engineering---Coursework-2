package com.acmetelecom;

import com.acmetelecom.logcall.LogCall;

public class EndCall extends LogCall {

	public EndCall(CallLogInterface billingSystem) {
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
