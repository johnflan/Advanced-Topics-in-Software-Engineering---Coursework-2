package com.acmetelecom;

import static org.junit.Assert.*;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;

import com.acmetelecom.customer.TariffLibrary;

public class LogCallTest {
	Mockery context;
	CallLogInterface callLogger;
	StartCall startCall;
	EndCall endCall;
	final String caller = "12345";
	final String callee = "98765";
	final long timeStamp = 1212122323;
	
	@Before
	public void setUp(){
		context = new Mockery();
		callLogger = context.mock(CallLogInterface.class);
	}
	
	@Test
	public void testStartCall() {
		
		
		
		
		context.checking(new Expectations(){{
			oneOf (callLogger).callInitiated(caller, callee);
		}});
		

		startCall = new StartCall(callLogger);
		startCall.from(caller).to(callee);
		
        context.assertIsSatisfied();
	}
	
	@Test
	public void testStartCallWithTimeStamp() {
		
		context.checking(new Expectations(){{
			oneOf (callLogger).callInitiated(caller, callee, timeStamp);
		}});
		
		startCall = new StartCall(callLogger);
		startCall.atTime(timeStamp).from(caller).to(callee);
		
		context.assertIsSatisfied();
	}
	
	@Test
	public void testEndCall(){
		
		context.checking(new Expectations(){{
			oneOf (callLogger).callCompleted(caller, callee);
		}});
		
		endCall = new EndCall(callLogger);
		endCall.from(caller).to(callee);
		
		context.assertIsSatisfied();
	}
	
	@Test
	public void testEndCallWithTimeStamp(){
		
		context.checking(new Expectations(){{
			oneOf (callLogger).callCompleted(caller, callee, timeStamp);
		}});
		
		endCall = new EndCall(callLogger);
		endCall.atTime(timeStamp).from(caller).to(callee);
		
		context.assertIsSatisfied();
	}

}
