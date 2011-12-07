package com.acmetelecom.helpers;

public interface ICallDuration {
	public ITestOrCall thatLastsForSeconds(int seconds);
	public ITestOrCall withoutEnd();
	public ITestOrCall withoutStart();
}
