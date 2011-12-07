package com.acmetelecom;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.acmetelecom.BillGeneratorInterface;
import com.acmetelecom.BillingSystem;
import com.acmetelecom.CallInterface;
import com.acmetelecom.DaytimePeakPeriod;
import com.acmetelecom.HtmlPrinter;
import com.acmetelecom.customer.Customer;
import com.acmetelecom.customer.CustomerDatabase;
import com.acmetelecom.customer.Tariff;
import com.acmetelecom.customer.TariffLibrary;
import com.acmetelecom.helpers.ICallDuration;
import com.acmetelecom.helpers.ICallee;
import com.acmetelecom.helpers.ICaller;
import com.acmetelecom.helpers.ICustomerName;
import com.acmetelecom.helpers.ICustomerNameOrCall;
import com.acmetelecom.helpers.IOffPeakSeconds;
import com.acmetelecom.helpers.IPeakSeconds;
import com.acmetelecom.helpers.IPhoneNumber;
import com.acmetelecom.helpers.IPricePlan;
import com.acmetelecom.helpers.IPricePlanCharge;
import com.acmetelecom.helpers.ITestOrCall;
import com.acmetelecom.helpers.ITestOrCreateBill;

@SuppressWarnings("unchecked")
public class BillingSystemContext implements ICustomerName, IPhoneNumber, IPricePlan, ICustomerNameOrCall, ICaller, ICallee, ICallDuration, ITestOrCall, IPricePlanCharge, IPeakSeconds, IOffPeakSeconds, ITestOrCreateBill {
	Mockery context = new Mockery();
	CustomerDatabase customerDatabase;
	TariffLibrary tariffLibrary;
	BillGeneratorInterface billGenerator;
	CallInterface calls;
	BillingSystem billingSystem;
	DateTimeFormatter dateTimeFormatter;
	
	HashMap<String,Customer> customerHashMap = new HashMap<String,Customer>();
	List<Customer> customerList = new ArrayList<Customer>();
	List<Test> testList = new ArrayList<Test>();
	
	String name;
	String phone;
	String pricePlan;
	
	String date;
	String caller;
	String callee;
	int durationSeconds;
	
	String nameAndPhone;
	String newPricePlan;
	int peakSeconds;
	int offPeakSeconds;
	
	public BillingSystemContext(){
		customerDatabase = context.mock(CustomerDatabase.class);
		tariffLibrary = context.mock(TariffLibrary.class);
		calls = context.mock(CallInterface.class);
		billGenerator = context.mock(BillGeneratorInterface.class);
		billingSystem = new BillingSystem(customerDatabase, tariffLibrary, billGenerator);
		
		DaytimePeakPeriod.PEAK_RATE_START_TIME = 7;
		DaytimePeakPeriod.OFF_PEAK_RATE_START_TIME = 19;
		
		dateTimeFormatter = DateTimeFormat.forPattern("dd/MM/yy HH:mm:ss");
	}
	
	public IPhoneNumber withACustomerNamed(String name){
		this.name = name;
		return this;
	}
	public IPricePlan withPhoneNumber(String phone){
		this.phone = phone;
		return this;
	}
	public ICustomerNameOrCall andPricePlan(String pricePlan){
		Customer c = new Customer(name,phone,pricePlan);
		customerHashMap.put(name+"/"+phone, c);
		customerList.add(c);
		name="";
		phone="";
		pricePlan="";
		return this;
	}
	
	public ICaller withACallAt(String date){
		this.date = date;
		return this;
	}
	
	public ICallee fromCaller(String phoneNumber){
		this.caller = phoneNumber;
		return this;
	}
	public ICallDuration toCallee(String phoneNumber){
		this.callee = phoneNumber;
		return this;
	}
	public ITestOrCall thatLastsForSeconds(int durationSeconds){
		this.durationSeconds = durationSeconds;

		DateTime dateTime = DateTime.parse(date, dateTimeFormatter);
		
		billingSystem.startCall().atTime(dateTime.getMillis()).from(caller).to(callee);
		billingSystem.endCall().atTime(dateTime.plusSeconds(durationSeconds).getMillis()).from(caller).to(callee);
		
		date="";
		caller="";
		callee="";
		durationSeconds=0;
		return this;
	}
	
	public ITestOrCall withoutEnd() {
		DateTime dateTime = DateTime.parse(date, dateTimeFormatter);
		
		billingSystem.startCall().atTime(dateTime.getMillis()).from(caller).to(callee);
		
		date="";
		caller="";
		callee="";
		durationSeconds=0;
		return this;
	}
	
	public ITestOrCall withoutStart() {
		DateTime dateTime = DateTime.parse(date, dateTimeFormatter);
		
		billingSystem.endCall().atTime(dateTime.getMillis()).from(caller).to(callee);
		
		date="";
		caller="";
		callee="";
		durationSeconds=0;
		return this;
	}
	
	public IPricePlanCharge testBillsOfCustomer(String nameAndPhone){
		this.nameAndPhone = nameAndPhone;
		return this;
	}
	public IPeakSeconds chargedWithPricePlan(String pricePlan){
		this.pricePlan = pricePlan;
		return this;
	}
	public IOffPeakSeconds forPeakSeconds(int peakSeconds){
		this.peakSeconds = peakSeconds;
		return this;
	}
	public ITestOrCreateBill andOffPeakSeconds(int offPeakSeconds){
		this.offPeakSeconds = offPeakSeconds;
		Customer customer = customerHashMap.get(nameAndPhone);
		Tariff tariff = Tariff.valueOf(customer.getPricePlan());
		
		String expectedCost = calculateExpectedCost(tariff,peakSeconds,offPeakSeconds);
		
		testList.add(new Test(customer,expectedCost));
		
		return this;
	}
	
	public void createBills(){
		context.checking(new Expectations(){{
			oneOf (customerDatabase).getCustomers(); will(returnValue(customerList));
			
			for(int i=0; i<customerList.size(); i++){
				oneOf (tariffLibrary).tarriffFor(customerList.get(i)); will(returnValue(Tariff.valueOf(customerList.get(i).getPricePlan())));
			}
			
			for(int i=0; i<testList.size(); i++){
				oneOf (billGenerator).send(with(testList.get(i).customer), with(any(List.class)), with(testList.get(i).cost), with(HtmlPrinter.getInstance()));
			}
		}});
		
		billingSystem.createCustomerBills();
	}
	
	public String calculateExpectedCost(Tariff tariff, long peakSeconds, long offPeakSeconds){
		BigDecimal peakCost = tariff.peakRate().multiply(new BigDecimal(peakSeconds));
		BigDecimal offPeakCost = tariff.offPeakRate().multiply(new BigDecimal(offPeakSeconds));
		
		return String.format("%.2f", peakCost.add(offPeakCost).setScale(0, RoundingMode.HALF_UP).divide(new BigDecimal(100)).doubleValue());
	}

	
	private class Test{
		Customer customer;
		String cost;
		
		public Test(Customer customer,String cost){
			this.customer = customer;
			this.cost = cost;
		}
	}
}
