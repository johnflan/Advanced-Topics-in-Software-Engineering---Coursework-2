package com.acmetelecom.specs.billingsystem;

import java.math.BigDecimal;

import org.concordion.integration.junit3.ConcordionTestCase;

import com.acmetelecom.BillingSystem;
import com.acmetelecom.customer.Customer;

public class BillingSystemExistingData extends ConcordionTestCase {

	BigDecimal peakRate;
	BigDecimal offPeakRate;
	String peakTime;
	String offPeakTime;
	BillingSystem billingSystem;
	
	
	public void setPeakRate(String rate){
		peakRate = new BigDecimal( Float.parseFloat(rate) );
	}
	
	public void setOffPeakRate(String rate){
		offPeakRate = new BigDecimal( Float.parseFloat(rate) );
	}

	public void setPeakTime(String time){
		peakTime = time;
	}
	
	public void setOffPeakTime(String time){
		offPeakTime = time;
	}
	
	public void createCustomer(String name, String number, String plan){
		System.out.println("Name:" + name + " number " + number + " plan " + plan);
		Customer test = new Customer(name, number, plan);
		//TODO
	}
	
	public void createCallEntry(String fromNo, String toNo, String startTime, String duration, String description, String cost){
		System.out.println("From: " + fromNo + " to: " + toNo + ", startTime: " + startTime + ", duration: " + duration + ", cost: " + cost);
		//TODO
	}
	
	public String getTotalCostsFor(String telNo){
		return "";
	}
}
