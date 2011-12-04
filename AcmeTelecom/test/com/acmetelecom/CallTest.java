package com.acmetelecom;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.jmock.*;
import org.joda.time.DateTime;

import com.acmetelecom.Call;
import com.acmetelecom.iCallEnd;
import com.acmetelecom.iCallStart;


public class CallTest{
	Mockery context = new Mockery();
	final iCallStart callStart = context.mock(iCallStart.class);
	final iCallEnd callEnd = context.mock(iCallEnd.class);
	
	final Call call = new Call(callStart, callEnd);
	
	long millisStart;
	long millisEnd;
	long durationSeconds;
	
	@Before
	public void setUp() throws Exception{
		DateTime dt = new DateTime(2011,1,1, 0, 0);
		millisStart = dt.getMillis();
		millisEnd = millisStart + 60000;
		durationSeconds = (millisEnd - millisStart) / 1000;
	}
	
	//Checks the call class methods for duration and time
	@Test
	public void callDurationAndTimeTests() {
		
		context.checking(new Expectations() {{
			oneOf (callStart).time(); will(returnValue(millisStart));
			oneOf (callEnd).time(); will(returnValue(millisEnd));
		}});
		
		assertEquals(durationSeconds, call.durationSeconds());
	}
	
	//Checks the name of the callee
	@Test
	public void calleeNameTest(){
		
		context.checking(new Expectations() {{
			oneOf (callStart).getCallee(); will(returnValue("CalleeName"));
		}});
		
		assertEquals("CalleeName",call.callee());
	}
	
	//Checks the format of the start date that the call returns
	@Test
	public void dateFormatTest(){
		
		context.checking(new Expectations(){{
			oneOf (callStart).time(); will(returnValue(millisStart));
		}});
		
		String realFormat = call.date();
		
		assertEquals("01/01/11 00:00", realFormat);
		assertFalse(realFormat.equals("00:00 01/01/11"));
		assertFalse(realFormat.equals("1/1/11 00:00"));
	}
	
}
