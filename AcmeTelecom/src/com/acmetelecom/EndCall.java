package com.acmetelecom;

import com.acmetelecom.logcall.LogCall;

public class EndCall extends LogCall {

	public EndCall(BillingSystem billingSystem) {
		super(billingSystem);
	}

	@Override
	protected void placeCallWithoutTimestamp() {
		billingSystem.callCompleted(super.caller, super.callee);

	}

	@SuppressWarnings("deprecation")
	@Override
	protected void placeCallWithTimestamp() {
		billingSystem.callCompleted(super.caller, super.callee, super.timeStamp);
	}

}
