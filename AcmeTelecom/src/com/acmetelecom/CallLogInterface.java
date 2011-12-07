package com.acmetelecom;

public interface CallLogInterface {

	    public void callInitiated(String caller, String callee);
	    
	    public void callInitiated(String caller, String callee, long timeStamp);

	    public void callCompleted(String caller, String callee);
	    
	    public void callCompleted(String caller, String callee, long timeStamp);
}
