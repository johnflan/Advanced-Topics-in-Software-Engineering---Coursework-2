package com.acmetelecom.logcall;

public abstract class LogCall implements CallFrom, CallFromTo {
	
	final protected CallLogInterface callLogger;
	
	protected long timeStamp;
	protected String caller;
	protected String callee;
	protected boolean timeStampSet = false;
	
	public LogCall(CallLogInterface  callLogger){
		this.callLogger = callLogger;
	}

	@Override
	/* 
	 * @param caller callers phone number
	 */
	public CallFromTo from(String caller) {
		this.caller = caller;
		return this;
	}
	
	@Override
	/* 
	 * @param currentTime Current time in milliseconds
	 */
	public CallFrom atTime(long currentTime) {
		this.timeStamp = currentTime;
		timeStampSet = true;
		return this;
	}
	
	@Override
	/* 
	 * @param callees callee's phone number
	 */
	public void to(String callee) {
		this.callee = callee;
		
		if (timeStampSet)
			placeCallWithTimestamp();
		else
			placeCallWithoutTimestamp();
	}
	
	
	abstract protected void placeCallWithoutTimestamp();
	
	abstract protected void placeCallWithTimestamp();

}
