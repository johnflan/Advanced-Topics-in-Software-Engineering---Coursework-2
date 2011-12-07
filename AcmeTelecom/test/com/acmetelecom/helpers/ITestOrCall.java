package com.acmetelecom.helpers;

public interface ITestOrCall {
	public ICaller withACallAt(String date);
	public IPricePlanCharge testBillsOfCustomer(String nameAndPhone);
}
