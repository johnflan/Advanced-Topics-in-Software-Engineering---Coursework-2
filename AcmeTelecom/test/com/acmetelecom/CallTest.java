package com.acmetelecom;

import static org.junit.Assert.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import junit.framework.TestCase;

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
	
	@Test
	public void callDurationAndTimeTests() {
		
		context.checking(new Expectations() {{
			allowing (callStart).time(); will(returnValue(millisStart));
			allowing (callEnd).time(); will(returnValue(millisEnd));
		}});
		
		assertEquals(durationSeconds, call.durationSeconds());
	}
	
	@Test
	public void calleeNameTest(){
		
		context.checking(new Expectations() {{
			allowing (callStart).getCallee(); will(returnValue("CalleeName"));
		}});
		
		assertEquals("CalleeName",call.callee());
	}
	
	@Test
	public void dateFormatTest(){
		
		context.checking(new Expectations(){{
			allowing (callStart).time(); will(returnValue(millisStart));
		}});
		
		assertEquals("01/01/11 00:00", call.date());
		assertFalse(call.date().equals("00:00 01/01/11"));
		assertFalse(call.date().equals("1/1/11 00:00"));
	}
}
