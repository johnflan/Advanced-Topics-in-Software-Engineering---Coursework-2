package com.acmetelecom;

import java.util.List;

import com.acmetelecom.customer.Customer;

public interface iBillGenerator {
	 public void send(Customer customer, List<BillingSystem.LineItem> calls, String totalBill);
}