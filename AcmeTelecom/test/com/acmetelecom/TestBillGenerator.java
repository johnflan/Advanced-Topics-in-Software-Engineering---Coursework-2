package com.acmetelecom;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.acmetelecom.BillingSystem.LineItem;
import com.acmetelecom.customer.Customer;

public class TestBillGenerator implements iBillGenerator {
	
	private Map<Customer, String> totalForCustomer;

	@Override
	public void send(Customer customer, List<LineItem> calls, String totalBill) {
		
		if (totalForCustomer == null)
			totalForCustomer = new HashMap<Customer, String>();
		
		if (totalForCustomer.containsKey(customer))
			try {
				throw new Exception("Bill alread calculated for Customer");
			} catch (Exception e) {
				e.printStackTrace();
			}
		
		totalForCustomer.put(customer, totalBill);
	}
	
	public Map<Customer, String> getTotals(){
		return totalForCustomer;
	}

}
