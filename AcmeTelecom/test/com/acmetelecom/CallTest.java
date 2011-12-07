package com.acmetelecom;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.jmock.*;
import org.joda.time.DateTime;

import com.acmetelecom.Call;
import com.acmetelecom.CallEndInterface;
import com.acmetelecom.CallStartInterface;
import com.acmetelecom.customer.CentralCustomerDatabase;
import com.acmetelecom.customer.CentralTariffDatabase;


public class CallTest{
	Mockery context = new Mockery();
	CallStartInterface callStart;
	CallEndInterface callEnd;
	Call call;
	BillingSystem billingSystem;

	long millisStart;
	long millisEnd;
	long durationSeconds;
	
	@Before
	public void setUp() throws Exception{
		callStart = context.mock(CallStartInterface.class);
		callEnd = context.mock(CallEndInterface.class);
		call = new Call(callStart, callEnd);
		billingSystem = new BillingSystem(CentralCustomerDatabase.getInstance(), CentralTariffDatabase.getInstance(), new BillGenerator());
		
		//Peak & Off Peak periods for the tests
		DaytimePeakPeriod.PEAK_RATE_START_TIME = 7;
		DaytimePeakPeriod.OFF_PEAK_RATE_START_TIME = 19;
		
		DateTime dt = new DateTime(2011,1,1, 0, 0);
		millisStart = dt.getMillis();
		millisEnd = millisStart + 60000;
		durationSeconds = (millisEnd - millisStart) / 1000;
	}
	
	//Checks the call class methods for duration and time
	@Test
	public void checkCallDurationAndTime() {
		
		context.checking(new Expectations() {{
			oneOf (callStart).time(); will(returnValue(millisStart));
			oneOf (callEnd).time(); will(returnValue(millisEnd));
		}});
		
		assertEquals(durationSeconds, call.durationSeconds());
	}
	
	//Checks the name of the callee
	@Test
	public void checkCalleeName(){
		
		context.checking(new Expectations() {{
			oneOf (callStart).getCallee(); will(returnValue("CalleeName"));
		}});
		
		assertEquals("CalleeName",call.callee());
	}
	
	//Checks the format of the start date that the call returns
	@Test
	public void checkDateFormat(){
		
		context.checking(new Expectations(){{
			oneOf (callStart).time(); will(returnValue(millisStart));
		}});
		
		String realFormat = call.date();
		
		assertEquals("01/01/11 00:00:00", realFormat);
		assertFalse(realFormat.equals("00:00:00 01/01/11"));
		assertFalse(realFormat.equals("1/1/11 00:00"));
	}
	
	//Checks if the peak seconds and off peak seconds of a call are correct
	@Test
	public void checkPeakAndOffPeakSeconds(){
		DateTime dt = new DateTime(2011,1,1,6,0,0);
		millisStart = dt.getMillis();
		millisEnd = dt.plusMinutes(90).getMillis();
		
		//peakSeconds=30m, offPeakSeconds=1h
		long peakSeconds = 30 * 60;
		long offPeakSeconds = 60 * 60;
		
		context.checking(new Expectations(){{
			allowing (callStart).time(); will(returnValue(millisStart));
			allowing (callEnd).time(); will(returnValue(millisEnd));
		}});
		
		assertEquals(peakSeconds,call.durationPeakSeconds());
		
		context.checking(new Expectations(){{
			exactly(2).of (callStart).time(); will(returnValue(millisStart));
			exactly(2).of (callEnd).time(); will(returnValue(millisEnd));
		}});
		
		assertEquals(offPeakSeconds,call.durationOffPeakSeconds());
	}
}
