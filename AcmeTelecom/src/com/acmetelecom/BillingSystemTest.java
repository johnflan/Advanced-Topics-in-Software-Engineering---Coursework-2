package com.acmetelecom;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.internal.State;
import org.junit.Before;
import org.junit.Test;

import com.acmetelecom.customer.Customer;
import com.acmetelecom.customer.CustomerDatabase;
import com.acmetelecom.customer.Tariff;
import com.acmetelecom.customer.TariffLibrary;

public class BillingSystemTest {
	Mockery context = new Mockery();
	
	CustomerDatabase customerDatabase;
	TariffLibrary tariffLibrary;
	iBillGenerator billGenerator;
	iCall calls;
	
	BillingSystem billingSystem;
	
	Customer customer1 = new Customer("C1_name","11111111111","Standard");
	Customer customer2 = new Customer("C2_name","22222222222","Business");
	Customer customer3 = new Customer("C3_name","33333333333","Leisure");
	
	List<Customer> customerList = new ArrayList<Customer>();
	
	Tariff tariff1 = Tariff.Standard;
	Tariff tariff2 = Tariff.Business;
	Tariff tariff3 = Tariff.Leisure;
	
	Date startDate1 = new Date(111,0,0,0,0,0);
	Date startDate2 = new Date(111,0,5,0,0,0);
	Date startDate3 = new Date(111,0,10,0,0,0);
	
	Date endDate1 = new Date(111,0,0,0,1,0);
	Date endDate2 = new Date(111,0,5,0,1,0);
	Date endDate3 = new Date(111,0,10,0,1,0);
	
	List<BillingSystem.LineItem> l1 = new ArrayList<BillingSystem.LineItem>();
	List<BillingSystem.LineItem> l2 = new ArrayList<BillingSystem.LineItem>();
	List<BillingSystem.LineItem> l3 = new ArrayList<BillingSystem.LineItem>();
	

	@Before
	public void setUp(){
		customerDatabase = context.mock(CustomerDatabase.class);
		tariffLibrary = context.mock(TariffLibrary.class);
		calls = context.mock(iCall.class);
		billGenerator = context.mock(iBillGenerator.class);
		billingSystem = new BillingSystem(customerDatabase,tariffLibrary,billGenerator);
		
		
		List<CallEvent> callLog = new ArrayList<CallEvent>();
		
		callLog.add(new CallStart("111111111111","111111111111"));
		callLog.add(new CallEnd("111111111111","111111111111"));
		
		billingSystem.setCallLog(callLog);
	}
	
	@Test
	public void checkCallLogSize() {
		final BillingSystem bst = new BillingSystem();
		
		assertEquals(0,bst.getCallLog().size());
		
		bst.callInitiated("caller", "callee");
		
		assertEquals(1,bst.getCallLog().size());
		
		bst.callCompleted("caller", "callee");
		
		assertEquals(2,bst.getCallLog().size());
		
		bst.createCustomerBills();
		
		assertEquals(0,bst.getCallLog().size());
	}
	
	//This test fails
	@Test
	public void checkWhatHappensIfSomeoneCallsHimself(){
		customerList.add(customer1);
		
		context.checking(new Expectations(){{
			allowing (customerDatabase).getCustomers(); will(returnValue(customerList));
			allowing (tariffLibrary).tarriffFor(customer1); will(returnValue(tariff1));
			
			allowing (billGenerator).send(customer1, l1, "0.00");
		}});
		
		billingSystem.callInitiated("11111111111", "11111111111");
		billingSystem.callCompleted("11111111111", "11111111111");
		
		billingSystem.createCustomerBills();
	}
	
}
