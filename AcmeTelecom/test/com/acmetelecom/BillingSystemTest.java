package com.acmetelecom;

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

import com.acmetelecom.BillingSystem.LineItem;
import com.acmetelecom.customer.Customer;
import com.acmetelecom.customer.CustomerDatabase;
import com.acmetelecom.customer.Tariff;
import com.acmetelecom.customer.TariffLibrary;

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
	Tariff tariff2 = Tariff.Business;
	Tariff tariff3 = Tariff.Leisure;
	
	//Dates for 3 calls
	Date startDate1 = new Date(111,0,0,0,0,0);
	Date startDate2 = new Date(111,0,5,0,0,0);
	Date startDate3 = new Date(111,0,10,0,0,0);
	Date endDate1 = new Date(111,0,0,0,1,0);
	Date endDate2 = new Date(111,0,5,0,1,0);
	Date endDate3 = new Date(111,0,10,0,1,0);
	
	@Before
	public void setUp(){
		customerDatabase = context.mock(CustomerDatabase.class);
		tariffLibrary = context.mock(TariffLibrary.class);
		calls = context.mock(iCall.class);
		billGenerator = context.mock(iBillGenerator.class);
		billingSystem = new BillingSystem(customerDatabase,tariffLibrary,billGenerator);
	}
	
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
	@Test
	public void checkWhatHappensIfSomeoneCallsHimself(){
		customerList.add(customer1);
		List<CallEvent> callLog = new ArrayList<CallEvent>();
		FakeCallStart callStart = new FakeCallStart("111111111111","111111111111",startDate1.getTime());
		callLog.add(callStart);
		FakeCallEnd callEnd=new FakeCallEnd("111111111111","111111111111",endDate1.getTime());
		callLog.add(callEnd);
		billingSystem.setCallLog(callLog);
		
		context.checking(new Expectations(){{
			allowing (customerDatabase).getCustomers(); will(returnValue(customerList));
			allowing (tariffLibrary).tarriffFor(customer1); will(returnValue(tariff1));
			allowing (billGenerator).send(with(customer1), with(any(List.class)), with("0.12"));
		}});
		
		billingSystem.createCustomerBills();
	}
	
	//The result should be the cost from startDate2 until endDate2 (60 seconds)
	@Test
	public void checkWhatHappensIfWeHaveTwoCallStartsAndOneCallEnd(){
		customerList.add(customer1);
		List<CallEvent> callLog = new ArrayList<CallEvent>();
		FakeCallStart callStart = new FakeCallStart("111111111111","222222222222",startDate1.getTime());
		callLog.add(callStart);
		callStart = new FakeCallStart("111111111111","222222222222",startDate2.getTime());
		callLog.add(callStart);
		FakeCallEnd callEnd=new FakeCallEnd("111111111111","222222222222",endDate2.getTime());
		callLog.add(callEnd);
		billingSystem.setCallLog(callLog);
		
		final String expectedCost = tariff1.offPeakRate().multiply(new BigDecimal(60)).setScale(0, RoundingMode.HALF_UP).divide(new BigDecimal(100)).toPlainString();
		
		context.checking(new Expectations(){{
			allowing (customerDatabase).getCustomers(); will(returnValue(customerList));
			allowing (tariffLibrary).tarriffFor(customer1); will(returnValue(tariff1));
			allowing (billGenerator).send(with(customer1), with(any(List.class)), with(expectedCost));
		}});
		
		billingSystem.createCustomerBills();
	}
	
	//The result should be the cost from startDate1 until endDate1 (60 seconds)
	@Test
	public void checkWhatHappensIfWeHaveOneCallStartAndTwoCallEnds(){
		customerList.add(customer1);
		
		List<CallEvent> callLog = new ArrayList<CallEvent>();
		FakeCallStart callStart = new FakeCallStart("111111111111","222222222222",startDate1.getTime());
		callLog.add(callStart);
		FakeCallEnd callEnd = new FakeCallEnd("111111111111","222222222222",endDate1.getTime());
		callLog.add(callEnd);
		callEnd=new FakeCallEnd("111111111111","222222222222",endDate2.getTime());
		callLog.add(callEnd);
		billingSystem.setCallLog(callLog);
		
		final String expectedCost = tariff1.offPeakRate().multiply(new BigDecimal(60)).setScale(0, RoundingMode.HALF_UP).divide(new BigDecimal(100)).toPlainString();
		
		context.checking(new Expectations(){{
			allowing (customerDatabase).getCustomers(); will(returnValue(customerList));
			allowing (tariffLibrary).tarriffFor(customer1); will(returnValue(tariff1));
			allowing (billGenerator).send(with(customer1), with(any(List.class)), with(expectedCost));
		}});
		
		billingSystem.createCustomerBills();
	}
	
	//
	@Test
	public void checkWhatHappensIfTwoCustomersHaveTheSamePhoneNumber(){
		customerList.add(customer1);
		customerList.add(customer1b);
		
		List<CallEvent> callLog = new ArrayList<CallEvent>();
		FakeCallStart callStart = new FakeCallStart("111111111111","222222222222",startDate1.getTime());
		callLog.add(callStart);
		FakeCallEnd callEnd = new FakeCallEnd("111111111111","222222222222",endDate1.getTime());
		callLog.add(callEnd);
		billingSystem.setCallLog(callLog);
		
		
	}
	
}
