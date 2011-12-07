package com.acmetelecom.helpers;

public interface ICustomerNameOrCall {
	public IPhoneNumber withACustomerNamed(String name);
	public ICaller withACallAt(String date);
}
