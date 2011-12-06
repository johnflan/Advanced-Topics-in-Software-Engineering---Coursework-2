package com.acmetelecom;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.internal.State;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.acmetelecom.LineItem;
import com.acmetelecom.customer.Customer;
import com.acmetelecom.customer.CustomerDatabase;
import com.acmetelecom.customer.Tariff;
import com.acmetelecom.customer.TariffLibrary;

@SuppressWarnings("all")
public class BillingSystemTest {
	//Classes needed to add fake calls in the log
	private class FakeCallStart extends CallEvent implements iCallStart{
		public FakeCallStart(String caller, String callee, long fakeMillis) {
			super(caller, callee, fakeMillis);
		}
	}
	private class FakeCallEnd extends CallEvent implements iCallEnd{
		public FakeCallEnd(String caller, String callee, long fakeMillis) {
			super(caller, callee, fakeMillis);
		}
	}
	
	Mockery context = new Mockery();
	
	CustomerDatabase customerDatabase;
	TariffLibrary tariffLibrary;
	iBillGenerator billGenerator;
	iCall calls;
	
	BillingSystem billingSystem;
	
	//Customers to be added in the fake database
	Customer customer1 = new Customer("C1_name", "111111111111", "Standard");
	Customer customer1b = new Customer("C1_differentName", "111111111111", "Business");
	Customer customer2 = new Customer("C2_name", "222222222222", "Business");
	Customer customer3 = new Customer("C3_name", "333333333333", "Leisure");
	
	List<Customer> customerList = new ArrayList<Customer>();
	
	//Types of tariff
	Tariff tariff1 = Tariff.Standard;
	Tariff tariff1b = Tariff.Business;
	Tariff tariff2 = Tariff.Business;
	Tariff tariff3 = Tariff.Leisure;
	
	//Dates for 3 calls
	DateTime peakDateTime1;
	DateTime offPeakDateTime1;
	DateTime offPeakDateTime2;
	DateTime peakDateTime2;
	DateTime offPeakDateTime3;
	@Before
	public void setUp(){
		customerDatabase = context.mock(CustomerDatabase.class);
		tariffLibrary = context.mock(TariffLibrary.class);
		calls = context.mock(iCall.class);
		billGenerator = context.mock(iBillGenerator.class);
		billingSystem = new BillingSystem(customerDatabase, tariffLibrary, billGenerator);
		
		//Peak & Off Peak periods for the tests
		DaytimePeakPeriod.PEAK_RATE_START_TIME = 7;
		DaytimePeakPeriod.OFF_PEAK_RATE_START_TIME = 19;
		
		peakDateTime1 = new DateTime(2011,1,1,9,0,0);
		peakDateTime2 = new DateTime(2011,1,1,15,0,0);
		offPeakDateTime1 = new DateTime(2011,1,1,5,30,0);
		offPeakDateTime2 = new DateTime(2011,1,1,6,0,0);
		offPeakDateTime3 = new DateTime(2011,1,1,20,0,0);
	}
	
	//Checks that call events can be added in the log
	@Test
	public void checkCallLogSize() {
		final BillingSystem billingSystem = new BillingSystem();
		
		assertEquals(0,billingSystem.getCallLog().size());
		
		billingSystem.callInitiated("caller", "callee");
		
		assertEquals(1,billingSystem.getCallLog().size());
		
		billingSystem.callCompleted("caller", "callee");
		
		assertEquals(2,billingSystem.getCallLog().size());
		
		billingSystem.createCustomerBills();
		
		assertEquals(0,billingSystem.getCallLog().size());
	}
	
	//This test is wrong because the cost should have been 0.00
	//TODO
	@Test
	public void checkWhatHappensIfSomeoneCallsHimself(){
		customerList.add(customer1);
		List<CallEvent> callLog = new ArrayList<CallEvent>();
		//System.out.println(offPeakDateTime1.getMillis());
		FakeCallStart callStart = new FakeCallStart("111111111111","111111111111",offPeakDateTime1.getMillis());
		callLog.add(callStart);
		FakeCallEnd callEnd=new FakeCallEnd("111111111111","111111111111",offPeakDateTime1.plusSeconds(250).getMillis());
		callLog.add(callEnd);
		billingSystem.setCallLog(callLog);
		
		long peakSeconds=250;
		final String expectedCost = calculateExpectedCost(tariff1,0,peakSeconds);
		
		context.checking(new Expectations(){{
			oneOf (customerDatabase).getCustomers(); will(returnValue(customerList));
			oneOf (tariffLibrary).tarriffFor(customer1); will(returnValue(tariff1));
			oneOf (billGenerator).send(with(customer1), with(any(List.class)), with(expectedCost), with(HtmlPrinter.getInstance()));
		}});
		
		billingSystem.createCustomerBills();
	}
	
	//The result should be the cost from startDate2 until endDate2 (60 seconds)
	@Test
	public void checkWhatHappensIfWeHaveTwoCallStartsAndOneCallEnd(){
		customerList.add(customer1);
		List<CallEvent> callLog = new ArrayList<CallEvent>();
		FakeCallStart callStart = new FakeCallStart("111111111111","222222222222",peakDateTime1.getMillis());
		callLog.add(callStart);
		callStart = new FakeCallStart("111111111111","222222222222",peakDateTime2.getMillis());
		callLog.add(callStart);
		FakeCallEnd callEnd=new FakeCallEnd("111111111111","222222222222",peakDateTime2.plusSeconds(60).getMillis());
		callLog.add(callEnd);
		billingSystem.setCallLog(callLog);
		long peakSeconds=60;
		final String expectedCost = calculateExpectedCost(tariff1,peakSeconds,0);
		
		context.checking(new Expectations(){{
			oneOf (customerDatabase).getCustomers(); will(returnValue(customerList));
			oneOf (tariffLibrary).tarriffFor(customer1); will(returnValue(tariff1));
			oneOf (billGenerator).send(with(customer1), with(any(List.class)), with(expectedCost), with(HtmlPrinter.getInstance()));
		}});
		
		billingSystem.createCustomerBills();
	}
	
	//The result should be the cost from startDate1 until endDate1 (60 seconds)
	@Test
	public void checkWhatHappensIfWeHaveOneCallStartAndTwoCallEnds(){
		customerList.add(customer1);
		
		List<CallEvent> callLog = new ArrayList<CallEvent>();
		FakeCallStart callStart = new FakeCallStart("111111111111","222222222222",peakDateTime1.getMillis());
		callLog.add(callStart);
		FakeCallEnd callEnd = new FakeCallEnd("111111111111","222222222222",peakDateTime1.plusSeconds(25).getMillis());
		callLog.add(callEnd);
		callEnd=new FakeCallEnd("111111111111","222222222222",peakDateTime1.plusSeconds(35).getMillis());
		callLog.add(callEnd);
		billingSystem.setCallLog(callLog);
		
		long peakSeconds=25;
		final String expectedCost = calculateExpectedCost(tariff1,peakSeconds,0);
		
		context.checking(new Expectations(){{
			oneOf (customerDatabase).getCustomers(); will(returnValue(customerList));
			oneOf (tariffLibrary).tarriffFor(customer1); will(returnValue(tariff1));
			oneOf (billGenerator).send(with(customer1), with(any(List.class)), with(expectedCost), with(HtmlPrinter.getInstance()));
		}});
		
		billingSystem.createCustomerBills();
	}
	
	//This is wrong because both costumers are charged for the call
	//AssertFalse proves that both costumers are charged differently according to their price plan
	//TODO
	@Test
	public void checkWhatHappensIfTwoCustomersHaveTheSamePhoneNumber(){
		customerList.add(customer1);
		customerList.add(customer1b);
		
		List<CallEvent> callLog = new ArrayList<CallEvent>();
		FakeCallStart callStart = new FakeCallStart("111111111111","222222222222",offPeakDateTime1.getMillis());
		callLog.add(callStart);
		FakeCallEnd callEnd = new FakeCallEnd("111111111111","222222222222",offPeakDateTime1.plusSeconds(60).getMillis());
		callLog.add(callEnd);
		billingSystem.setCallLog(callLog);
		
		final String expectedCost1 = calculateExpectedCost(tariff1,0,60);
		final String expectedCost1b = calculateExpectedCost(tariff1b,0,60);
		
		context.checking(new Expectations(){{
			oneOf (customerDatabase).getCustomers(); will(returnValue(customerList));
			oneOf (tariffLibrary).tarriffFor(customer1); will(returnValue(tariff1));
			oneOf (tariffLibrary).tarriffFor(customer1b); will(returnValue(tariff1b));
			oneOf (billGenerator).send(with(customer1), with(any(List.class)), with(expectedCost1), with(HtmlPrinter.getInstance()));
			oneOf (billGenerator).send(with(customer1b), with(any(List.class)), with(expectedCost1b), with(HtmlPrinter.getInstance()));
		}});
		
		billingSystem.createCustomerBills();
		context.assertIsSatisfied();
		assertFalse(expectedCost1.equals(expectedCost1b));
	}
	
	//Tests that costs can be calculated for different price plans in the peak time period
	@Test
	public void checkCallCostWhenCallIsInPeakTime(){
		customerList.add(customer1);
		customerList.add(customer2);
		customerList.add(customer3);
		initializeAllCallsForPeak();
		
		final String expectedCost1 = calculateExpectedCost(tariff1,60,0);
		final String expectedCost2 = calculateExpectedCost(tariff2,60,0);
		final String expectedCost3 = calculateExpectedCost(tariff3,60,0);
		
		context.checking(new Expectations(){{
			oneOf (customerDatabase).getCustomers(); will(returnValue(customerList));
			oneOf (tariffLibrary).tarriffFor(customer1); will(returnValue(tariff1));
			oneOf (tariffLibrary).tarriffFor(customer2); will(returnValue(tariff2));
			oneOf (tariffLibrary).tarriffFor(customer3); will(returnValue(tariff3));
			oneOf (billGenerator).send(with(customer1), with(any(List.class)), with(expectedCost1), with(HtmlPrinter.getInstance()));
			oneOf (billGenerator).send(with(customer2), with(any(List.class)), with(expectedCost2), with(HtmlPrinter.getInstance()));
			oneOf (billGenerator).send(with(customer3), with(any(List.class)), with(expectedCost3), with(HtmlPrinter.getInstance()));
		}});
		System.out.println("Test expected cost: " + expectedCost1 + " --- for customer " + customer1.getFullName());
		billingSystem.createCustomerBills();
	}
	
	//Tests that costs can be calculated for different price plans in the off peak time period
	@Test
	public void checkCallCostWhenCallIsInOffPeakTime(){
		customerList.add(customer1);
		customerList.add(customer2);
		customerList.add(customer3);
		initializeAllCallsForOffPeak();
		//Call duration=1 hour
		long offPeakSeconds=1*60*60;
		final String expectedCost1 = calculateExpectedCost(tariff1,0,offPeakSeconds);
		final String expectedCost2 = calculateExpectedCost(tariff2,0,offPeakSeconds);
		final String expectedCost3 = calculateExpectedCost(tariff3,0,offPeakSeconds);
		
		context.checking(new Expectations(){{
			oneOf (customerDatabase).getCustomers(); will(returnValue(customerList));
			oneOf (tariffLibrary).tarriffFor(customer1); will(returnValue(tariff1));
			oneOf (tariffLibrary).tarriffFor(customer2); will(returnValue(tariff2));
			oneOf (tariffLibrary).tarriffFor(customer3); will(returnValue(tariff3));
			oneOf (billGenerator).send(with(customer1), with(any(List.class)), with(expectedCost1), with(HtmlPrinter.getInstance()));
			oneOf (billGenerator).send(with(customer2), with(any(List.class)), with(expectedCost2), with(HtmlPrinter.getInstance()));
			oneOf (billGenerator).send(with(customer3), with(any(List.class)), with(expectedCost3), with(HtmlPrinter.getInstance()));
		}});
		
		billingSystem.createCustomerBills();
	}
	
	//Tests that costs can be calculated for different price plans
	//when the call is in the peak and off peak time periods
	@Test
	public void checkCallCostWhenCallStartsInOffPeakTimeAndEndsInPeakTime(){
		customerList.add(customer1);
		customerList.add(customer2);
		customerList.add(customer3);
		initializeAllCallsForOffPeakToPeak();
		//peakSeconds=2hours, offPeakSeconds=1.5hours
		long peakSeconds=2*60*60;
		long offPeakSeconds=30*60+60*60;
		final String expectedCost1 = calculateExpectedCost(tariff1,peakSeconds,offPeakSeconds);
		final String expectedCost2 = calculateExpectedCost(tariff2,peakSeconds,offPeakSeconds);
		final String expectedCost3 = calculateExpectedCost(tariff3,peakSeconds,offPeakSeconds);
		
		context.checking(new Expectations(){{
			oneOf (customerDatabase).getCustomers(); will(returnValue(customerList));
			oneOf (tariffLibrary).tarriffFor(customer1); will(returnValue(tariff1));
			oneOf (tariffLibrary).tarriffFor(customer2); will(returnValue(tariff2));
			oneOf (tariffLibrary).tarriffFor(customer3); will(returnValue(tariff3));
			oneOf (billGenerator).send(with(customer1), with(any(List.class)), with(expectedCost1), with(HtmlPrinter.getInstance()));
			oneOf (billGenerator).send(with(customer2), with(any(List.class)), with(expectedCost2), with(HtmlPrinter.getInstance()));
			oneOf (billGenerator).send(with(customer3), with(any(List.class)), with(expectedCost3), with(HtmlPrinter.getInstance()));
		}});
		billingSystem.createCustomerBills();
	}
		
	//Tests that costs can be calculated for different price plans
	//when the call is in the peak and off peak time periods
	@Test
	public void checkCallCostWhenCallStartsInPeakTimeAndEndsInOffPeakTime(){
		customerList.add(customer1);
		customerList.add(customer2);
		customerList.add(customer3);
		initializeAllCallsForPeakToOffPeak();
		//peakSeconds=4hours, offPeakSeconds=1hour
		long peakSeconds=4*60*60;
		long offPeakSeconds=1*60*60;
		final String expectedCost1 = calculateExpectedCost(tariff1,peakSeconds,offPeakSeconds);
		final String expectedCost2 = calculateExpectedCost(tariff2,peakSeconds,offPeakSeconds);
		final String expectedCost3 = calculateExpectedCost(tariff3,peakSeconds,offPeakSeconds);
		
		context.checking(new Expectations(){{
			oneOf (customerDatabase).getCustomers(); will(returnValue(customerList));
			oneOf (tariffLibrary).tarriffFor(customer1); will(returnValue(tariff1));
			oneOf (tariffLibrary).tarriffFor(customer2); will(returnValue(tariff2));
			oneOf (tariffLibrary).tarriffFor(customer3); will(returnValue(tariff3));
			oneOf (billGenerator).send(with(customer1), with(any(List.class)), with(expectedCost1), with(HtmlPrinter.getInstance()));
			oneOf (billGenerator).send(with(customer2), with(any(List.class)), with(expectedCost2), with(HtmlPrinter.getInstance()));
			oneOf (billGenerator).send(with(customer3), with(any(List.class)), with(expectedCost3), with(HtmlPrinter.getInstance()));
		}});
		
		billingSystem.createCustomerBills();
	}
	
	//Tests that costs can be calculated for different price plans
	//when the call is in the peak and off peak time periods and lasts more than a day
	@Test
	public void checkCallCostWhenCallStartsInOffPeakTimeAndEndsInOffPeakTimeButLastsMoreThanADay(){
		customerList.add(customer1);
		customerList.add(customer2);
		customerList.add(customer3);
		initializeAllCallsForOffPeakToOffPeak();
		
		//Calls last for one day and 23 hours
		long peakSeconds=24*60*60;
		long offPeakSeconds=23*60*60;
		final String expectedCost1 = calculateExpectedCost(tariff1,peakSeconds,offPeakSeconds);
		final String expectedCost2 = calculateExpectedCost(tariff2,peakSeconds,offPeakSeconds);
		final String expectedCost3 = calculateExpectedCost(tariff3,peakSeconds,offPeakSeconds);
		
		context.checking(new Expectations(){{
			oneOf (customerDatabase).getCustomers(); will(returnValue(customerList));
			oneOf (tariffLibrary).tarriffFor(customer1); will(returnValue(tariff1));
			oneOf (tariffLibrary).tarriffFor(customer2); will(returnValue(tariff2));
			oneOf (tariffLibrary).tarriffFor(customer3); will(returnValue(tariff3));
			oneOf (billGenerator).send(with(customer1), with(any(List.class)), with(expectedCost1), with(HtmlPrinter.getInstance()));
			oneOf (billGenerator).send(with(customer2), with(any(List.class)), with(expectedCost2), with(HtmlPrinter.getInstance()));
			oneOf (billGenerator).send(with(customer3), with(any(List.class)), with(expectedCost3), with(HtmlPrinter.getInstance()));
		}});
		billingSystem.createCustomerBills();
	}
	
	//Tests the cost of a call when the year changes
	@Test
	public void checkCallCostOverChangeOfYear(){
		customerList.add(customer1);
		DateTime dateBefore = new DateTime(2011,12,31,6,0,0);
		DateTime dateAfter = new DateTime(2012,1,1,14,0,0);
		
		List<CallEvent> callLog = new ArrayList<CallEvent>();
		FakeCallStart callStart = new FakeCallStart("111111111111","222222222222",dateBefore.getMillis());
		callLog.add(callStart);
		FakeCallEnd callEnd = new FakeCallEnd("111111111111","222222222222",dateAfter.getMillis());
		callLog.add(callEnd);
		billingSystem.setCallLog(callLog);
		
		//Calls last for one day and 23 hours
		//peakSeconds=19hours, offPeakSeconds=12hours+11hours
		long peakSeconds=19*60*60;
		long offPeakSeconds=13*60*60;
		final String expectedCost = calculateExpectedCost(tariff1,peakSeconds,offPeakSeconds);
		
		context.checking(new Expectations(){{
			oneOf (customerDatabase).getCustomers(); will(returnValue(customerList));
			oneOf (tariffLibrary).tarriffFor(customer1); will(returnValue(tariff1));
			oneOf (billGenerator).send(with(customer1), with(any(List.class)), with(expectedCost), with(HtmlPrinter.getInstance()));
		}});
		billingSystem.createCustomerBills();
	}
	
	//Tests what happens if a CallEnd event is earlier than a CallStart event. Call cost should be 0.0
	@Test
	public void checkCallEndBeforeCallStart(){
		customerList.add(customer1);
		
		List<CallEvent> callLog = new ArrayList<CallEvent>();
		FakeCallStart callStart = new FakeCallStart("111111111111","222222222222",peakDateTime1.getMillis());
		callLog.add(callStart);
		FakeCallEnd callEnd = new FakeCallEnd("111111111111","222222222222",peakDateTime1.minusMinutes(2).getMillis());
		callLog.add(callEnd);
		billingSystem.setCallLog(callLog);
		
		//peakSeconds=0, offPeakSeconds=0
		long peakSeconds=0;
		long offPeakSeconds=0;
		final String expectedCost = calculateExpectedCost(tariff1,peakSeconds,offPeakSeconds);
		
		context.checking(new Expectations(){{
			oneOf (customerDatabase).getCustomers(); will(returnValue(customerList));
			oneOf (tariffLibrary).tarriffFor(customer1); will(returnValue(tariff1));
			oneOf (billGenerator).send(with(customer1), with(any(List.class)), with(expectedCost), with(HtmlPrinter.getInstance()));
		}});
		billingSystem.createCustomerBills();
	}
	
	//Tests what happens if a CallStart event and the next CallEnd event have different Callee numbers
	//The result is that the cost is calculated which is wrong
	//TODO
	@Test
	public void checkCallStartWithCallEndToAnotherNumber(){
		customerList.add(customer1);
		
		List<CallEvent> callLog = new ArrayList<CallEvent>();
		FakeCallStart callStart = new FakeCallStart("111111111111","222222222222",peakDateTime1.getMillis());
		callLog.add(callStart);
		FakeCallEnd callEnd = new FakeCallEnd("111111111111","333333333333",peakDateTime1.plusHours(1).getMillis());
		callLog.add(callEnd);
		billingSystem.setCallLog(callLog);
		
		//peakSeconds=1hour, offPeakSeconds=0
		long peakSeconds= 1 * 60 * 60;
		long offPeakSeconds=0;
		final String expectedCost = calculateExpectedCost(tariff1,peakSeconds,offPeakSeconds);
		
		context.checking(new Expectations(){{
			oneOf (customerDatabase).getCustomers(); will(returnValue(customerList));
			oneOf (tariffLibrary).tarriffFor(customer1); will(returnValue(tariff1));
			oneOf (billGenerator).send(with(customer1), with(any(List.class)), with(expectedCost), with(HtmlPrinter.getInstance()));
		}});
		billingSystem.createCustomerBills();
	}
	
	//Tests what happens if the Tariff of a customer changes after he has made a call
	//The cost of all previous calls is calculated with the new Tariff which is wrong
	//TODO
	@Test
	public void checkWhatHappensIfTariffChanges(){
		customerList.add(customer1);

		List<CallEvent> callLog = new ArrayList<CallEvent>();
		FakeCallStart callStart = new FakeCallStart("111111111111","222222222222",peakDateTime1.getMillis());
		callLog.add(callStart);
		FakeCallEnd callEnd = new FakeCallEnd("111111111111","222222222222",peakDateTime1.plusHours(1).getMillis());
		callLog.add(callEnd);
		billingSystem.setCallLog(callLog);
		
		//Tariff changes after the call
		tariff1 = Tariff.Business;
		
		//peakSeconds=1hour, offPeakSeconds=0
		long peakSeconds= 1 * 60 * 60;
		long offPeakSeconds=0;
		//The cost is calculated with the new Tariff
		final String expectedCost = calculateExpectedCost(tariff1,peakSeconds,offPeakSeconds);
		
		context.checking(new Expectations(){{
			oneOf (customerDatabase).getCustomers(); will(returnValue(customerList));
			oneOf (tariffLibrary).tarriffFor(customer1); will(returnValue(tariff1));
			oneOf (billGenerator).send(with(customer1), with(any(List.class)), with(expectedCost), with(HtmlPrinter.getInstance()));
		}});
		billingSystem.createCustomerBills();
	}
	
	//Tests what happens if the Tariff of a customer changes after he has made a call
	//The cost of all previous calls is calculated with the new Tariff which is wrong
	//TODO
	@Test
	public void checkWhatHappensIfPeakTimesChanges(){
		customerList.add(customer1);

		//The call starts at 6am and ends at 7am
		List<CallEvent> callLog = new ArrayList<CallEvent>();
		FakeCallStart callStart = new FakeCallStart("111111111111","222222222222",offPeakDateTime2.getMillis());
		callLog.add(callStart);
		FakeCallEnd callEnd = new FakeCallEnd("111111111111","222222222222",offPeakDateTime2.plusHours(1).getMillis());
		callLog.add(callEnd);
		billingSystem.setCallLog(callLog);
		
		//Peak & Off Peak periods for the tests change
		DaytimePeakPeriod.PEAK_RATE_START_TIME = 6;
		DaytimePeakPeriod.OFF_PEAK_RATE_START_TIME = 18;
		
		//peakSeconds=1hour, offPeakSeconds=0
		long peakSeconds = 1 * 60 * 60;
		long offPeakSeconds = 0;
		final String expectedCost = calculateExpectedCost(tariff1,peakSeconds,offPeakSeconds);
		
		context.checking(new Expectations(){{
			oneOf (customerDatabase).getCustomers(); will(returnValue(customerList));
			oneOf (tariffLibrary).tarriffFor(customer1); will(returnValue(tariff1));
			oneOf (billGenerator).send(with(customer1), with(any(List.class)), with(expectedCost), with(HtmlPrinter.getInstance()));
		}});
		billingSystem.createCustomerBills();
	}
	
	//Tests what happens if a CallStart event and the next CallEnd event have different Caller numbers and the same Callee
	@Test
	public void checkCallStartWithADifferentNumberAndCallEndWithTheSame(){
		customerList.add(customer1);
		customerList.add(customer2);
		
		List<CallEvent> callLog = new ArrayList<CallEvent>();
		FakeCallStart callStart = new FakeCallStart("111111111111","333333333333",peakDateTime1.getMillis());
		callLog.add(callStart);
		FakeCallEnd callEnd = new FakeCallEnd("222222222222","333333333333",peakDateTime1.plusHours(1).getMillis());
		callLog.add(callEnd);
		billingSystem.setCallLog(callLog);
		
		//peakSeconds=0, offPeakSeconds=0
		long peakSeconds=0;
		long offPeakSeconds=0;
		final String expectedCost1 = calculateExpectedCost(tariff1,peakSeconds,offPeakSeconds);
		final String expectedCost2 = calculateExpectedCost(tariff2,peakSeconds,offPeakSeconds);
		
		context.checking(new Expectations(){{
			oneOf (customerDatabase).getCustomers(); will(returnValue(customerList));
			oneOf (tariffLibrary).tarriffFor(customer1); will(returnValue(tariff1));
			oneOf (tariffLibrary).tarriffFor(customer2); will(returnValue(tariff2));
			oneOf (billGenerator).send(with(customer1), with(any(List.class)), with(expectedCost1), with(HtmlPrinter.getInstance()));
			oneOf (billGenerator).send(with(customer2), with(any(List.class)), with(expectedCost2), with(HtmlPrinter.getInstance()));
		}});
		billingSystem.createCustomerBills();
	}
	
	//Tests the cost for a call that lasts only one seconds. Call cost should be 0.0
	@Test
	public void checkCostOfOffPeakCallThatLastsOneSecond(){
		customerList.add(customer1);
		
		List<CallEvent> callLog = new ArrayList<CallEvent>();
		FakeCallStart callStart = new FakeCallStart("111111111111","222222222222",offPeakDateTime1.getMillis());
		callLog.add(callStart);
		FakeCallEnd callEnd = new FakeCallEnd("111111111111","222222222222",offPeakDateTime1.plusSeconds(1).getMillis());
		callLog.add(callEnd);
		billingSystem.setCallLog(callLog);
		
		//peakSeconds=0, offPeakSeconds=1
		long peakSeconds=0;
		long offPeakSeconds=1;
		final String expectedCost1 = calculateExpectedCost(tariff1,peakSeconds,offPeakSeconds);
		context.checking(new Expectations(){{
			oneOf (customerDatabase).getCustomers(); will(returnValue(customerList));
			oneOf (tariffLibrary).tarriffFor(customer1); will(returnValue(tariff1));
			oneOf (billGenerator).send(with(customer1), with(any(List.class)), with(expectedCost1), with(HtmlPrinter.getInstance()));
		}});
		billingSystem.createCustomerBills();
	}
	
	public String calculateExpectedCost(Tariff tariff, long peakSeconds, long offPeakSeconds){
		BigDecimal peakCost = tariff.peakRate().multiply(new BigDecimal(peakSeconds));
		BigDecimal offPeakCost = tariff.offPeakRate().multiply(new BigDecimal(offPeakSeconds));
		
		return String.format("%.2f", peakCost.add(offPeakCost).setScale(0, RoundingMode.HALF_UP).divide(new BigDecimal(100)).doubleValue());
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////// Initializing Calls ////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////
	private void initializeAllCallsForPeak() {
		List<CallEvent> callLog = new ArrayList<CallEvent>();
		FakeCallStart callStart = new FakeCallStart("111111111111","222222222222",peakDateTime1.getMillis());
		callLog.add(callStart);
		FakeCallEnd callEnd = new FakeCallEnd("111111111111","222222222222",peakDateTime1.plusMinutes(1).getMillis());
		callLog.add(callEnd);
		
		callStart = new FakeCallStart("222222222222","333333333333",peakDateTime2.getMillis());
		callLog.add(callStart);
		callEnd = new FakeCallEnd("222222222222","333333333333",peakDateTime2.plusMinutes(1).getMillis());
		callLog.add(callEnd);
		
		callStart = new FakeCallStart("333333333333","222222222222",peakDateTime1.getMillis());
		callLog.add(callStart);
		callEnd = new FakeCallEnd("333333333333","222222222222",peakDateTime1.plusMinutes(1).getMillis());
		callLog.add(callEnd);
		billingSystem.setCallLog(callLog);
	}
	
	private void initializeAllCallsForOffPeak() {
		List<CallEvent> callLog = new ArrayList<CallEvent>();
		FakeCallStart callStart = new FakeCallStart("111111111111","222222222222",offPeakDateTime1.getMillis());
		callLog.add(callStart);
		FakeCallEnd callEnd = new FakeCallEnd("111111111111","222222222222",offPeakDateTime1.plusHours(1).getMillis());
		callLog.add(callEnd);
		
		callStart = new FakeCallStart("222222222222","333333333333",offPeakDateTime1.getMillis());
		callLog.add(callStart);
		callEnd = new FakeCallEnd("222222222222","333333333333",offPeakDateTime1.plusHours(1).getMillis());
		callLog.add(callEnd);
		
		callStart = new FakeCallStart("333333333333","222222222222",offPeakDateTime1.getMillis());
		callLog.add(callStart);
		callEnd = new FakeCallEnd("333333333333","222222222222",offPeakDateTime1.plusHours(1).getMillis());
		callLog.add(callEnd);
		billingSystem.setCallLog(callLog);
	}
	
	private void initializeAllCallsForPeakToOffPeak() {
		List<CallEvent> callLog = new ArrayList<CallEvent>();
		FakeCallStart callStart = new FakeCallStart("111111111111","222222222222",peakDateTime2.getMillis());
		callLog.add(callStart);
		FakeCallEnd callEnd = new FakeCallEnd("111111111111","222222222222",offPeakDateTime3.getMillis());
		callLog.add(callEnd);
		
		callStart = new FakeCallStart("222222222222","333333333333",peakDateTime2.getMillis());
		callLog.add(callStart);
		callEnd = new FakeCallEnd("222222222222","333333333333",offPeakDateTime3.getMillis());
		callLog.add(callEnd);
		
		callStart = new FakeCallStart("333333333333","222222222222",peakDateTime2.getMillis());
		callLog.add(callStart);
		callEnd = new FakeCallEnd("333333333333","222222222222",offPeakDateTime3.getMillis());
		callLog.add(callEnd);
		billingSystem.setCallLog(callLog);
	}
	
	private void initializeAllCallsForOffPeakToPeak() {
		List<CallEvent> callLog = new ArrayList<CallEvent>();
		FakeCallStart callStart = new FakeCallStart("111111111111","222222222222",offPeakDateTime1.getMillis());
		callLog.add(callStart);
		FakeCallEnd callEnd = new FakeCallEnd("111111111111","222222222222",peakDateTime1.getMillis());
		callLog.add(callEnd);
		
		callStart = new FakeCallStart("222222222222","333333333333",offPeakDateTime1.getMillis());
		callLog.add(callStart);
		callEnd = new FakeCallEnd("222222222222","333333333333",peakDateTime1.getMillis());
		callLog.add(callEnd);
		
		callStart = new FakeCallStart("333333333333","222222222222",offPeakDateTime1.getMillis());
		callLog.add(callStart);
		callEnd = new FakeCallEnd("333333333333","222222222222",peakDateTime1.getMillis());
		callLog.add(callEnd);
		billingSystem.setCallLog(callLog);
	}
	
	private void initializeAllCallsForOffPeakToOffPeak() {
		List<CallEvent> callLog = new ArrayList<CallEvent>();
		FakeCallStart callStart = new FakeCallStart("111111111111","222222222222",offPeakDateTime1.getMillis());
		callLog.add(callStart);
		FakeCallEnd callEnd = new FakeCallEnd("111111111111","222222222222",offPeakDateTime1.plusDays(1).plusHours(23).getMillis());
		callLog.add(callEnd);
		
		callStart = new FakeCallStart("222222222222","333333333333",offPeakDateTime1.getMillis());
		callLog.add(callStart);
		callEnd = new FakeCallEnd("222222222222","333333333333",offPeakDateTime1.plusDays(1).plusHours(23).getMillis());
		callLog.add(callEnd);
		
		callStart = new FakeCallStart("333333333333","222222222222",offPeakDateTime1.getMillis());
		callLog.add(callStart);
		callEnd = new FakeCallEnd("333333333333","222222222222",offPeakDateTime1.plusDays(1).plusHours(23).getMillis());
		callLog.add(callEnd);
		billingSystem.setCallLog(callLog);
	}
}
