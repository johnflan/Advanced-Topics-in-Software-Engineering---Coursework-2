package com.acmetelecom.helpers;

public interface ITestOrCreateBill {
	public IPricePlanCharge testBillsOfCustomer(String nameAndPhone);
	public void createBills();
}
