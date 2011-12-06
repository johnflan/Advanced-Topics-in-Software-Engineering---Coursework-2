package com.acmetelecom.logcall;

public interface CallFrom {
	
	public CallFromTo from(String caller);
	
	public CallFrom atTime(long callDurationMs);

}
