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
	
	Mockery context = new Mockery();
	CustomerDatabase customerDatabase;
	TariffLibrary tariffLibrary;
	BillGeneratorInterface billGenerator;
	CallInterface calls;
	BillingSystem billingSystem;
	
	//Customers to be added in the fake database
	Customer customer1 = new Customer("C1_name", "111111111111", "Standard");
	Customer customer2 = new Customer("C2_name", "222222222222", "Business");
	Customer customer3 = new Customer("C3_name", "333333333333", "Leisure");
	Customer customer4 = new Customer("C4_name", "111111111111", "Business");
	
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
	
	//PhoneNumbers of the customers
	String c1;
	String c2;
	String c3;
	String c4;
	
	@Before
	public void setUp(){
		customerDatabase = context.mock(CustomerDatabase.class);
		tariffLibrary = context.mock(TariffLibrary.class);
		calls = context.mock(CallInterface.class);
		billGenerator = context.mock(BillGeneratorInterface.class);
		billingSystem = new BillingSystem(customerDatabase, tariffLibrary, billGenerator);
		
		//Peak & Off Peak periods for the tests
		DaytimePeakPeriod.PEAK_RATE_START_TIME = 7;
		DaytimePeakPeriod.OFF_PEAK_RATE_START_TIME = 19;
		
		peakDateTime1 = new DateTime(2011,1,1,9,0,0);
		peakDateTime2 = new DateTime(2011,1,1,15,0,0);
		offPeakDateTime1 = new DateTime(2011,1,1,5,30,0);
		offPeakDateTime2 = new DateTime(2011,1,1,6,0,0);
		offPeakDateTime3 = new DateTime(2011,1,1,20,0,0);
		
		c1=customer1.getPhoneNumber();
		c4=customer4.getPhoneNumber();
		c2=customer2.getPhoneNumber();
		c3=customer3.getPhoneNumber();
	}
		
	//This test is wrong because the cost should have been 0.00
	@Test
	public void checkWhatHappensIfSomeoneCallsHimself(){
		customerList.add(customer1);
		List<CallEvent> callLog = new ArrayList<CallEvent>();
		
		billingSystem.startCall().atTime(offPeakDateTime1.getMillis()).from(c1).to(c1);
		billingSystem.endCall().atTime(offPeakDateTime1.plusSeconds(250).getMillis()).from(c1).to(c1);
		
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

		billingSystem.startCall().atTime(peakDateTime1.getMillis()).from(c1).to(c2);
		billingSystem.startCall().atTime(peakDateTime2.getMillis()).from(c1).to(c2);
		billingSystem.endCall().atTime(peakDateTime2.plusSeconds(60).getMillis()).from(c1).to(c2);
		
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
		
		billingSystem.startCall().atTime(peakDateTime1.getMillis()).from(c1).to(c2);
		billingSystem.endCall().atTime(peakDateTime1.plusSeconds(25).getMillis()).from(c1).to(c2);
		billingSystem.endCall().atTime(peakDateTime1.plusSeconds(35).getMillis()).from(c1).to(c2);	
		
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
	@Test
	public void checkWhatHappensIfTwoCustomersHaveTheSamePhoneNumber(){
		customerList.add(customer1);
		customerList.add(customer4);
		
		billingSystem.startCall().atTime(offPeakDateTime1.getMillis()).from(c1).to(c2);
		billingSystem.endCall().atTime(offPeakDateTime1.plusSeconds(60).getMillis()).from(c1).to(c2);
		
		final String expectedCost1 = calculateExpectedCost(tariff1,0,60);
		final String expectedCost1b = calculateExpectedCost(tariff1b,0,60);
		
		context.checking(new Expectations(){{
			oneOf (customerDatabase).getCustomers(); will(returnValue(customerList));
			oneOf (tariffLibrary).tarriffFor(customer1); will(returnValue(tariff1));
			oneOf (tariffLibrary).tarriffFor(customer4); will(returnValue(tariff1b));
			oneOf (billGenerator).send(with(customer1), with(any(List.class)), with(expectedCost1), with(HtmlPrinter.getInstance()));
			oneOf (billGenerator).send(with(customer4), with(any(List.class)), with(expectedCost1b), with(HtmlPrinter.getInstance()));
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

		//First Call
		billingSystem.startCall().atTime(peakDateTime1.getMillis()).from(c1).to(c2);
		billingSystem.endCall().atTime(peakDateTime1.plusMinutes(1).getMillis()).from(c1).to(c2);
		//Second Call
		billingSystem.startCall().atTime(peakDateTime2.getMillis()).from(c2).to(c3);
		billingSystem.endCall().atTime(peakDateTime2.plusMinutes(1).getMillis()).from(c2).to(c3);
		//Third Call
		billingSystem.startCall().atTime(peakDateTime1.getMillis()).from(c3).to(c2);
		billingSystem.endCall().atTime(peakDateTime1.plusMinutes(1).getMillis()).from(c3).to(c2);
		
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
		//initializeAllCallsForOffPeak();
		
		//First Call
		billingSystem.startCall().atTime(offPeakDateTime1.getMillis()).from(c1).to(c2);
		billingSystem.endCall().atTime(offPeakDateTime1.plusHours(1).getMillis()).from(c1).to(c2);
		//Second Call
		billingSystem.startCall().atTime(offPeakDateTime1.getMillis()).from(c2).to(c3);
		billingSystem.endCall().atTime(offPeakDateTime1.plusHours(1).getMillis()).from(c2).to(c3);
		//Third Call
		billingSystem.startCall().atTime(offPeakDateTime1.getMillis()).from(c3).to(c2);
		billingSystem.endCall().atTime(offPeakDateTime1.plusHours(1).getMillis()).from(c3).to(c2);
		
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
		//initializeAllCallsForOffPeakToPeak();
		
		//First Call
		billingSystem.startCall().atTime(offPeakDateTime1.getMillis()).from(c1).to(c2);
		billingSystem.endCall().atTime(peakDateTime1.getMillis()).from(c1).to(c2);
		//Second Call
		billingSystem.startCall().atTime(offPeakDateTime1.getMillis()).from(c2).to(c3);
		billingSystem.endCall().atTime(peakDateTime1.getMillis()).from(c2).to(c3);
		//Third Call
		billingSystem.startCall().atTime(offPeakDateTime1.getMillis()).from(c3).to(c2);
		billingSystem.endCall().atTime(peakDateTime1.getMillis()).from(c3).to(c2);
		
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
		
		//First Call
		billingSystem.startCall().atTime(peakDateTime2.getMillis()).from(c1).to(c2);
		billingSystem.endCall().atTime(offPeakDateTime3.getMillis()).from(c1).to(c2);
		//Second Call
		billingSystem.startCall().atTime(peakDateTime2.getMillis()).from(c2).to(c3);
		billingSystem.endCall().atTime(offPeakDateTime3.getMillis()).from(c2).to(c3);
		//Third Call
		billingSystem.startCall().atTime(peakDateTime2.getMillis()).from(c3).to(c2);
		billingSystem.endCall().atTime(offPeakDateTime3.getMillis()).from(c3).to(c2);
		
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
		
		//First Call
		billingSystem.startCall().atTime(offPeakDateTime1.getMillis()).from(c1).to(c2);
		billingSystem.endCall().atTime(offPeakDateTime1.plusDays(1).plusHours(23).getMillis()).from(c1).to(c2);
		//Second Call
		billingSystem.startCall().atTime(offPeakDateTime1.getMillis()).from(c2).to(c3);
		billingSystem.endCall().atTime(offPeakDateTime1.plusDays(1).plusHours(23).getMillis()).from(c2).to(c3);
		//Third Call
		billingSystem.startCall().atTime(offPeakDateTime1.getMillis()).from(c3).to(c2);
		billingSystem.endCall().atTime(offPeakDateTime1.plusDays(1).plusHours(23).getMillis()).from(c3).to(c2);
		
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
		
		billingSystem.startCall().atTime(dateBefore.getMillis()).from(c1).to(c2);
		billingSystem.endCall().atTime(dateAfter.getMillis()).from(c1).to(c2);
		
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
		
		billingSystem.startCall().atTime(peakDateTime1.getMillis()).from(c1).to(c2);
		billingSystem.endCall().atTime(peakDateTime1.minusMinutes(2).getMillis()).from(c1).to(c2);
		
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
	@Test
	public void checkCallStartWithCallEndToAnotherNumber(){
		customerList.add(customer1);
		
		billingSystem.startCall().atTime(peakDateTime1.getMillis()).from(c1).to(c2);
		billingSystem.endCall().atTime(peakDateTime1.plusHours(1).getMillis()).from(c1).to(c3);
		
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
	@Test
	public void checkWhatHappensIfTariffChanges(){
		customerList.add(customer1);
		
		billingSystem.startCall().atTime(peakDateTime1.getMillis()).from(c1).to(c2);
		billingSystem.endCall().atTime(peakDateTime1.plusHours(1).getMillis()).from(c1).to(c2);
		
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
	//The cost of all previous calls is calculated with the new peak times which is wrong
	@Test
	public void checkWhatHappensIfPeakTimesChanges(){
		customerList.add(customer1);

		//The call starts at 6am and ends at 7am
		billingSystem.startCall().atTime(offPeakDateTime2.getMillis()).from(c1).to(c2);
		billingSystem.endCall().atTime(offPeakDateTime2.plusHours(1).getMillis()).from(c1).to(c2);
		
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
		
		billingSystem.startCall().atTime(peakDateTime1.getMillis()).from(c1).to(c3);
		billingSystem.endCall().atTime(peakDateTime1.plusHours(1).getMillis()).from(c2).to(c3);
		
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
		
		billingSystem.startCall().atTime(offPeakDateTime1.getMillis()).from("111111111111").to("222222222222");
		billingSystem.endCall().atTime(offPeakDateTime1.plusSeconds(1).getMillis()).from("111111111111").to("222222222222");		
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
	
	
	//Classes needed to add fake calls in the log
	private class FakeCallStart extends CallEvent implements CallStartInterface{
		public FakeCallStart(String caller, String callee, long fakeMillis) {
			super(caller, callee, fakeMillis);
		}
	}
	private class FakeCallEnd extends CallEvent implements CallEndInterface{
		public FakeCallEnd(String caller, String callee, long fakeMillis) {
			super(caller, callee, fakeMillis);
		}
	}

}
