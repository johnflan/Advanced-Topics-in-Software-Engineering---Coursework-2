package com.acmetelecom;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.acmetelecom.LineItem;
import com.acmetelecom.customer.Customer;

public class TestBillGenerator implements iBillGenerator {
	
	private Map<Customer, String> totalForCustomer;

	@Override
	public void send(Customer customer, List<LineItem> calls, String totalBill, Printer printer) {
		
		if (totalForCustomer == null)
			totalForCustomer = new HashMap<Customer, String>();
		
		if (totalForCustomer.containsKey(customer))
			try {
				throw new Exception("Bill already calculated for Customer");
			} catch (Exception e) {
				e.printStackTrace();
			}
		
		totalForCustomer.put(customer, totalBill);
	}
	
	public Map<Customer, String> getTotals(){
		return totalForCustomer;
	}

}
