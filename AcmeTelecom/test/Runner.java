package com.acmetelecom.tests;

import com.acmetelecom.BillingSystem;

public class Runner {
	public static void main(String[] args) throws Exception {
		System.out.println("Running...");
		BillingSystem billingSystem = new BillingSystem();
		billingSystem.callInitiated("447722113434", "447766814143");
		sleepSeconds(2);
		billingSystem.callCompleted("447722113434", "447766814143");
		billingSystem.callInitiated("447722113434", "447711111111");
		sleepSeconds(3);
		billingSystem.callCompleted("447722113434", "447711111111");
		billingSystem.callInitiated("447777765432", "447711111111");
		sleepSeconds(6);
		billingSystem.callCompleted("447777765432", "447711111111");
		billingSystem.createCustomerBills();
		}
		private static void sleepSeconds(int n) throws InterruptedException {
		Thread.sleep(n * 1000);
		}
}
