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
/*	DateTime startDateOffPeak1 = new DateTime(2011,1,1,0,0,0);
	DateTime startDateOffPeak2 = new DateTime(2011,1,5,0,0,0);
	DateTime startDateOffPeak3 = new DateTime(2011,1,10,0,0,0);
	DateTime endDateOffPeak1 = new DateTime(2011,1,1,0,1,0);
	DateTime endDateOffPeak2 = new DateTime(2011,1,5,0,1,0);
	DateTime endDateOffPeak3 = new DateTime(2011,1,10,0,1,0);*/
	
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
		peakDateTime1 = new DateTime(2011,1,1,9,0,0);
		peakDateTime2 = new DateTime(2011,1,1,15,0,0);
		offPeakDateTime1 = new DateTime(2011,1,1,5,30,0);
		offPeakDateTime2 = new DateTime(2011,1,1,6,0,0);
		offPeakDateTime3 = new DateTime(2011,1,1,20,0,0);
		//DateTime peakDateTime = new DateTime(2011,1,1,5,0,0);
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
	//Everything is calculated as peak time which will need to be changed
	//TODO
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
		System.out.println("Test expected cost: " + expectedCost1 + " --- for customer " + customer1.getFullName());
		billingSystem.createCustomerBills();
	}
		
	//Tests that costs can be calculated for different price plans
	//when the call is in the peak and off peak time periods
	//Everything is calculated as peak time which will need to be changed
	//TODO
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
	//Everything is calculated as peak time which will need to be changed
	//TODO
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
		//System.out.println("Expected Cost: "+expectedCost1);
		billingSystem.createCustomerBills();
	}
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
		final String expectedCost1 = calculateExpectedCost(tariff1,peakSeconds,offPeakSeconds);
		
		context.checking(new Expectations(){{
			oneOf (customerDatabase).getCustomers(); will(returnValue(customerList));
			oneOf (tariffLibrary).tarriffFor(customer1); will(returnValue(tariff1));
			oneOf (billGenerator).send(with(customer1), with(any(List.class)), with(expectedCost1), with(HtmlPrinter.getInstance()));
		}});
		billingSystem.createCustomerBills();
	}
	
	//Test what happens if a CallEnd event is earlier than a CallStart event. Call cost should be 0.0
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
		final String expectedCost1 = calculateExpectedCost(tariff1,peakSeconds,offPeakSeconds);
		
		context.checking(new Expectations(){{
			oneOf (customerDatabase).getCustomers(); will(returnValue(customerList));
			oneOf (tariffLibrary).tarriffFor(customer1); will(returnValue(tariff1));
			oneOf (billGenerator).send(with(customer1), with(any(List.class)), with(expectedCost1), with(HtmlPrinter.getInstance()));
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
		
		//peakSeconds=0, offPeakSeconds=0
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
/*		DateTime startDatePeak1 = new DateTime(2011,1,1,10,0,0);
		DateTime startDatePeak2 = new DateTime(2011,1,5,10,0,0);
		DateTime startDatePeak3 = new DateTime(2011,1,10,10,0,0);
		DateTime endDatePeak1 = new DateTime(2011,1,1,10,1,0);
		DateTime endDatePeak2 = new DateTime(2011,1,5,10,1,0);
		DateTime endDatePeak3 = new DateTime(2011,1,10,10,1,0);*/
		
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
/*		DateTime startDatePeak1 = new DateTime(2011,1,1,18,59,40);
		DateTime startDatePeak2 = new DateTime(2011,1,5,18,59,40);
		DateTime startDatePeak3 = new DateTime(2011,1,10,18,59,40);
		DateTime endDateOffPeak1 = new DateTime(2011,1,1,19,0,30);
		DateTime endDateOffPeak2 = new DateTime(2011,1,5,19,0,30);
		DateTime endDateOffPeak3 = new DateTime(2011,1,10,19,0,30);*/
		
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
/*		DateTime startDateOffPeak1 = new DateTime(2011,1,1,6,59,40);
		DateTime startDateOffPeak2 = new DateTime(2011,1,5,6,59,40);
		DateTime startDateOffPeak3 = new DateTime(2011,1,10,6,59,40);
		DateTime endDatePeak1 = new DateTime(2011,1,1,7,0,30);
		DateTime endDatePeak2 = new DateTime(2011,1,5,7,0,30);
		DateTime endDatePeak3 = new DateTime(2011,1,10,7,0,30);*/
		
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
/*		DateTime startDateOffPeak1 = new DateTime(2011,1,1,6,0,0);
		DateTime startDateOffPeak2 = new DateTime(2011,1,5,6,0,0);
		DateTime startDateOffPeak3 = new DateTime(2011,1,10,6,0,0);
		DateTime endDateOffPeak1 = new DateTime(2011,1,3,5,0,0);
		DateTime endDateOffPeak2 = new DateTime(2011,1,7,5,0,0);
		DateTime endDateOffPeak3 = new DateTime(2011,1,12,5,0,0);*/
		
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
