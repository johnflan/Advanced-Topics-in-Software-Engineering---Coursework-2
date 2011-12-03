package com.acmetelecom;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Date;

import org.junit.Test;


public class DatetimePeakPeriodTest {
	DaytimePeakPeriod dtpp = new DaytimePeakPeriod();
	Date time;
	
	@Test
	public void testPeakChangeAtSevenAM() {
		time = new Date(111,10,26,6,59,59);
		assertTrue("6:59:59am is Peak", dtpp.offPeak(time));
		
		time = new Date(111,10,26,7,0,0);
		assertFalse("7:00:00am is OffPeak", dtpp.offPeak(time));
	}
	
	@Test
	public void testPeakChangeAtSevenPM() {
		time = new Date(111,10,26,18,59,59);
		assertFalse("6:59:59pm is OffPeak", dtpp.offPeak(time));
		
		time = new Date(111,10,26,19,0,0);
		assertTrue("7:00:00pm is Peak", dtpp.offPeak(time));
	}
	
	@Test
	public void testPeakAndOffPeakTimes() {
		time = new Date(111,10,26,3,0,0);
		assertTrue("6:59:59am is Peak", dtpp.offPeak(time));
		
		time = new Date(111,10,26,13,0,0);
		assertFalse("1:00:00pm is OffPeak", dtpp.offPeak(time));
		
		time = new Date(111,10,26,23,0,0);
		assertTrue("7:00:00pm is Peak", dtpp.offPeak(time));
	}
}
