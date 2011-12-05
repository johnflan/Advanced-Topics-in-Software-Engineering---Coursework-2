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
	Date startDateOffPeak1 = new Date(111,0,0,0,0,0);
	Date startDateOffPeak2 = new Date(111,0,5,0,0,0);
	Date startDateOffPeak3 = new Date(111,0,10,0,0,0);
	Date endDateOffPeak1 = new Date(111,0,0,0,1,0);
	Date endDateOffPeak2 = new Date(111,0,5,0,1,0);
	Date endDateOffPeak3 = new Date(111,0,10,0,1,0);
	
	@Before
	public void setUp(){
		customerDatabase = context.mock(CustomerDatabase.class);
		tariffLibrary = context.mock(TariffLibrary.class);
		calls = context.mock(iCall.class);
		billGenerator = context.mock(iBillGenerator.class);
		billingSystem = new BillingSystem(customerDatabase,tariffLibrary,billGenerator);
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
		FakeCallStart callStart = new FakeCallStart("111111111111","111111111111",startDateOffPeak1.getTime());
		callLog.add(callStart);
		FakeCallEnd callEnd=new FakeCallEnd("111111111111","111111111111",endDateOffPeak1.getTime());
		callLog.add(callEnd);
		billingSystem.setCallLog(callLog);
		
		final String expectedCost = calculateExpectedCost(tariff1,0,60);
		
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
		FakeCallStart callStart = new FakeCallStart("111111111111","222222222222",startDateOffPeak1.getTime());
		callLog.add(callStart);
		callStart = new FakeCallStart("111111111111","222222222222",startDateOffPeak2.getTime());
		callLog.add(callStart);
		FakeCallEnd callEnd=new FakeCallEnd("111111111111","222222222222",endDateOffPeak2.getTime());
		callLog.add(callEnd);
		billingSystem.setCallLog(callLog);
		
		final String expectedCost = calculateExpectedCost(tariff1,0,60);
		
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
		FakeCallStart callStart = new FakeCallStart("111111111111","222222222222",startDateOffPeak1.getTime());
		callLog.add(callStart);
		FakeCallEnd callEnd = new FakeCallEnd("111111111111","222222222222",endDateOffPeak1.getTime());
		callLog.add(callEnd);
		callEnd=new FakeCallEnd("111111111111","222222222222",endDateOffPeak2.getTime());
		callLog.add(callEnd);
		billingSystem.setCallLog(callLog);
		
		final String expectedCost = calculateExpectedCost(tariff1,0,60);
		
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
		FakeCallStart callStart = new FakeCallStart("111111111111","222222222222",startDateOffPeak1.getTime());
		callLog.add(callStart);
		FakeCallEnd callEnd = new FakeCallEnd("111111111111","222222222222",endDateOffPeak1.getTime());
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
		
		billingSystem.createCustomerBills();
	}
	
	//Tests that costs can be calculated for different price plans in the off peak time period
	@Test
	public void checkCallCostWhenCallIsInOffPeakTime(){
		customerList.add(customer1);
		customerList.add(customer2);
		customerList.add(customer3);
		initializeAllCallsForOffPeak();
		
		final String expectedCost1 = calculateExpectedCost(tariff1,0,60);
		final String expectedCost2 = calculateExpectedCost(tariff2,0,60);
		final String expectedCost3 = calculateExpectedCost(tariff3,0,60);
		
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
		
		final String expectedCost1 = calculateExpectedCost(tariff1,50,0);
		final String expectedCost2 = calculateExpectedCost(tariff2,50,0);
		final String expectedCost3 = calculateExpectedCost(tariff3,50,0);
		
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
	public void checkCallCostWhenCallStartsInPeakTimeAndEndsInOffPeakTime(){
		customerList.add(customer1);
		customerList.add(customer2);
		customerList.add(customer3);
		initializeAllCallsForOffPeakToPeak();
		
		final String expectedCost1 = calculateExpectedCost(tariff1,50,0);
		final String expectedCost2 = calculateExpectedCost(tariff2,50,0);
		final String expectedCost3 = calculateExpectedCost(tariff3,50,0);
		
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
		final String expectedCost1 = calculateExpectedCost(tariff1,47*60*60,0);
		final String expectedCost2 = calculateExpectedCost(tariff2,47*60*60,0);
		final String expectedCost3 = calculateExpectedCost(tariff3,47*60*60,0);
		
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

	public String calculateExpectedCost(Tariff tariff, long peakSeconds, long offPeakSeconds){
		BigDecimal peakCost = tariff.peakRate().multiply(new BigDecimal(peakSeconds));
		BigDecimal offPeakCost = tariff.offPeakRate().multiply(new BigDecimal(offPeakSeconds));
		
		return String.format("%.2f", peakCost.add(offPeakCost).setScale(0, RoundingMode.HALF_UP).divide(new BigDecimal(100)).doubleValue());
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////// Initializing Calls ////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////
	private void initializeAllCallsForPeak() {
		Date startDatePeak1 = new Date(111,0,0,10,0,0);
		Date startDatePeak2 = new Date(111,0,5,10,0,0);
		Date startDatePeak3 = new Date(111,0,10,10,0,0);
		Date endDatePeak1 = new Date(111,0,0,10,1,0);
		Date endDatePeak2 = new Date(111,0,5,10,1,0);
		Date endDatePeak3 = new Date(111,0,10,10,1,0);
		
		List<CallEvent> callLog = new ArrayList<CallEvent>();
		FakeCallStart callStart = new FakeCallStart("111111111111","222222222222",startDatePeak1.getTime());
		callLog.add(callStart);
		FakeCallEnd callEnd = new FakeCallEnd("111111111111","222222222222",endDatePeak1.getTime());
		callLog.add(callEnd);
		
		callStart = new FakeCallStart("222222222222","333333333333",startDatePeak2.getTime());
		callLog.add(callStart);
		callEnd = new FakeCallEnd("222222222222","333333333333",endDatePeak2.getTime());
		callLog.add(callEnd);
		
		callStart = new FakeCallStart("333333333333","222222222222",startDatePeak3.getTime());
		callLog.add(callStart);
		callEnd = new FakeCallEnd("333333333333","222222222222",endDatePeak3.getTime());
		callLog.add(callEnd);
		billingSystem.setCallLog(callLog);
	}
	
	private void initializeAllCallsForOffPeak() {
		List<CallEvent> callLog = new ArrayList<CallEvent>();
		FakeCallStart callStart = new FakeCallStart("111111111111","222222222222",startDateOffPeak1.getTime());
		callLog.add(callStart);
		FakeCallEnd callEnd = new FakeCallEnd("111111111111","222222222222",endDateOffPeak1.getTime());
		callLog.add(callEnd);
		
		callStart = new FakeCallStart("222222222222","333333333333",startDateOffPeak2.getTime());
		callLog.add(callStart);
		callEnd = new FakeCallEnd("222222222222","333333333333",endDateOffPeak2.getTime());
		callLog.add(callEnd);
		
		callStart = new FakeCallStart("333333333333","222222222222",startDateOffPeak3.getTime());
		callLog.add(callStart);
		callEnd = new FakeCallEnd("333333333333","222222222222",endDateOffPeak3.getTime());
		callLog.add(callEnd);
		billingSystem.setCallLog(callLog);
	}
	
	private void initializeAllCallsForPeakToOffPeak() {
		Date startDatePeak1 = new Date(111,0,0,18,59,40);
		Date startDatePeak2 = new Date(111,0,5,18,59,40);
		Date startDatePeak3 = new Date(111,0,10,18,59,40);
		Date endDateOffPeak1 = new Date(111,0,0,19,0,30);
		Date endDateOffPeak2 = new Date(111,0,5,19,0,30);
		Date endDateOffPeak3 = new Date(111,0,10,19,0,30);
		
		List<CallEvent> callLog = new ArrayList<CallEvent>();
		FakeCallStart callStart = new FakeCallStart("111111111111","222222222222",startDatePeak1.getTime());
		callLog.add(callStart);
		FakeCallEnd callEnd = new FakeCallEnd("111111111111","222222222222",endDateOffPeak1.getTime());
		callLog.add(callEnd);
		
		callStart = new FakeCallStart("222222222222","333333333333",startDatePeak2.getTime());
		callLog.add(callStart);
		callEnd = new FakeCallEnd("222222222222","333333333333",endDateOffPeak2.getTime());
		callLog.add(callEnd);
		
		callStart = new FakeCallStart("333333333333","222222222222",startDatePeak3.getTime());
		callLog.add(callStart);
		callEnd = new FakeCallEnd("333333333333","222222222222",endDateOffPeak3.getTime());
		callLog.add(callEnd);
		billingSystem.setCallLog(callLog);
	}
	
	private void initializeAllCallsForOffPeakToPeak() {
		Date startDateOffPeak1 = new Date(111,0,0,6,59,40);
		Date startDateOffPeak2 = new Date(111,0,5,6,59,40);
		Date startDateOffPeak3 = new Date(111,0,10,6,59,40);
		Date endDatePeak1 = new Date(111,0,0,7,0,30);
		Date endDatePeak2 = new Date(111,0,5,7,0,30);
		Date endDatePeak3 = new Date(111,0,10,7,0,30);
		
		List<CallEvent> callLog = new ArrayList<CallEvent>();
		FakeCallStart callStart = new FakeCallStart("111111111111","222222222222",startDateOffPeak1.getTime());
		callLog.add(callStart);
		FakeCallEnd callEnd = new FakeCallEnd("111111111111","222222222222",endDatePeak1.getTime());
		callLog.add(callEnd);
		
		callStart = new FakeCallStart("222222222222","333333333333",startDateOffPeak2.getTime());
		callLog.add(callStart);
		callEnd = new FakeCallEnd("222222222222","333333333333",endDatePeak2.getTime());
		callLog.add(callEnd);
		
		callStart = new FakeCallStart("333333333333","222222222222",startDateOffPeak3.getTime());
		callLog.add(callStart);
		callEnd = new FakeCallEnd("333333333333","222222222222",endDatePeak3.getTime());
		callLog.add(callEnd);
		billingSystem.setCallLog(callLog);
	}
	
	private void initializeAllCallsForOffPeakToOffPeak() {
		Date startDateOffPeak1 = new Date(111,0,0,6,0,0);
		Date startDateOffPeak2 = new Date(111,0,5,6,0,0);
		Date startDateOffPeak3 = new Date(111,0,10,6,0,0);
		Date endDateOffPeak1 = new Date(111,0,2,5,0,0);
		Date endDateOffPeak2 = new Date(111,0,7,5,0,0);
		Date endDateOffPeak3 = new Date(111,0,12,5,0,0);
		
		List<CallEvent> callLog = new ArrayList<CallEvent>();
		FakeCallStart callStart = new FakeCallStart("111111111111","222222222222",startDateOffPeak1.getTime());
		callLog.add(callStart);
		FakeCallEnd callEnd = new FakeCallEnd("111111111111","222222222222",endDateOffPeak1.getTime());
		callLog.add(callEnd);
		
		callStart = new FakeCallStart("222222222222","333333333333",startDateOffPeak2.getTime());
		callLog.add(callStart);
		callEnd = new FakeCallEnd("222222222222","333333333333",endDateOffPeak2.getTime());
		callLog.add(callEnd);
		
		callStart = new FakeCallStart("333333333333","222222222222",startDateOffPeak3.getTime());
		callLog.add(callStart);
		callEnd = new FakeCallEnd("333333333333","222222222222",endDateOffPeak3.getTime());
		callLog.add(callEnd);
		billingSystem.setCallLog(callLog);
	}
}
