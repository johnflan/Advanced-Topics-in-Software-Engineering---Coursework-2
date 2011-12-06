package com.acmetelecom;

import com.acmetelecom.BillingSystem;

public class Runner {
	public static void main(String[] args) throws Exception {
		System.out.println("Running...");
		BillingSystem billingSystem = new BillingSystem();
		billingSystem.startCall().from("447722113434").to("447766814143");
		sleepSeconds(1);
		billingSystem.endCall().from("447722113434").to("447766814143");
		billingSystem.startCall().from("447722113434").to("447711111111");
		sleepSeconds(1);
		billingSystem.endCall().from("447722113434").to("447711111111");
		billingSystem.startCall().from("447777765432").to("447711111111");
		sleepSeconds(1);
		billingSystem.endCall().from("447777765432").to("447711111111");
		billingSystem.createCustomerBills();
	}
	private static void sleepSeconds(int n) throws InterruptedException {
		Thread.sleep(n * 100);
	}
}
