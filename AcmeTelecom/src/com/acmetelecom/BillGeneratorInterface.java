package com.acmetelecom;

import java.util.List;

import com.acmetelecom.customer.Customer;

public interface BillGeneratorInterface {
	 public void send(Customer customer, List<LineItem> calls, String totalBill, Printer printer);
}
