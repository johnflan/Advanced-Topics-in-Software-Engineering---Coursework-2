package com.acmetelecom;

import org.junit.Before;
import org.junit.Test;


public class BillingSystemTest {
	BillingSystemContext newTest;
	
	private final String CARLY_SIMON = "Carly Simon";
	private final String CARLY_SIMON_PHONE_NUM = "111111111";
	
	private final String TONI_BRAXTON = "Toni Braxton";
	private final String TONI_BRAXTON_PHONE_NUM = "222222222";
	
	private final String TOM_JONES = "Tom Jones";
	private final String TOM_JONES_PHONE_NUM = "333333333";
	
	private final String ELTON_JOHN = "Elton John";
	private final String ELTON_JOHN_PHONE_NUM = "111111111";
	
	private final String STANDARD_TARIFF = "Standard";
	private final String BUSINESS_TARIFF = "Business";
	private final String LEISURE_TARIFF = "Leisure";
	
	@Before
	public void setUp(){
		newTest = new BillingSystemContext();
	}
	
	@Test
	public void checkWhatHappensIfSomeoneCallsHimself(){
		newTest
			.withACustomerNamed(CARLY_SIMON).withPhoneNumber(CARLY_SIMON_PHONE_NUM).andPricePlan(STANDARD_TARIFF)
			.withACallAt("01/01/11 10:00:00").fromCaller(CARLY_SIMON_PHONE_NUM).toCallee(CARLY_SIMON_PHONE_NUM).thatLastsForSeconds(60)
			.testBillsOfCustomer(CARLY_SIMON+"/"+CARLY_SIMON_PHONE_NUM).chargedWithPricePlan(STANDARD_TARIFF).forPeakSeconds(60).andOffPeakSeconds(0)
			.createBills();
	}
	
	//The result should be the cost from startDate2 until endDate2 (60 seconds)
	@Test
	public void checkWhatHappensIfWeHaveTwoCallStartsAndOneCallEnd(){
		newTest
			.withACustomerNamed(CARLY_SIMON).withPhoneNumber(CARLY_SIMON_PHONE_NUM).andPricePlan(STANDARD_TARIFF)
			.withACallAt("01/01/11 10:00:00").fromCaller(CARLY_SIMON_PHONE_NUM).toCallee(TONI_BRAXTON_PHONE_NUM).withoutEnd()
			.withACallAt("01/01/11 11:00:00").fromCaller(CARLY_SIMON_PHONE_NUM).toCallee(TONI_BRAXTON_PHONE_NUM).thatLastsForSeconds(60)
			.testBillsOfCustomer(CARLY_SIMON+"/"+CARLY_SIMON_PHONE_NUM).chargedWithPricePlan(STANDARD_TARIFF).forPeakSeconds(60).andOffPeakSeconds(0)
			.createBills();
	}
	
	//The result should be the cost from startDate1 until endDate1 (60 seconds)
	@Test
	public void checkWhatHappensIfWeHaveOneCallStartAndTwoCallEnds(){
		newTest
			.withACustomerNamed(CARLY_SIMON).withPhoneNumber(CARLY_SIMON_PHONE_NUM).andPricePlan(STANDARD_TARIFF)
			.withACallAt("01/01/11 10:00:00").fromCaller(CARLY_SIMON_PHONE_NUM).toCallee(TONI_BRAXTON_PHONE_NUM).withoutStart()
			.withACallAt("01/01/11 11:00:00").fromCaller(CARLY_SIMON_PHONE_NUM).toCallee(TONI_BRAXTON_PHONE_NUM).thatLastsForSeconds(60)
			.testBillsOfCustomer(CARLY_SIMON+"/"+CARLY_SIMON_PHONE_NUM).chargedWithPricePlan(STANDARD_TARIFF).forPeakSeconds(60).andOffPeakSeconds(0)
			.createBills();
	}
	
	//This is wrong because both costumers are charged for the call
	//Both customers are charged according to their own price plan
	@Test
	public void checkWhatHappensIfTwoCustomersHaveTheSamePhoneNumber(){
		newTest
			.withACustomerNamed(CARLY_SIMON).withPhoneNumber(CARLY_SIMON_PHONE_NUM).andPricePlan(STANDARD_TARIFF)
			.withACustomerNamed(ELTON_JOHN).withPhoneNumber(ELTON_JOHN_PHONE_NUM).andPricePlan(BUSINESS_TARIFF)
			.withACallAt("01/01/11 10:00:00").fromCaller(CARLY_SIMON_PHONE_NUM).toCallee(TOM_JONES_PHONE_NUM).thatLastsForSeconds(60)
			.testBillsOfCustomer(CARLY_SIMON+"/"+CARLY_SIMON_PHONE_NUM).chargedWithPricePlan(STANDARD_TARIFF).forPeakSeconds(60).andOffPeakSeconds(0)
			.testBillsOfCustomer(ELTON_JOHN+"/"+ELTON_JOHN_PHONE_NUM).chargedWithPricePlan(BUSINESS_TARIFF).forPeakSeconds(60).andOffPeakSeconds(0)
			.createBills();
	}
	
	//Tests that costs can be calculated for different price plans in the peak time period
	@Test
	public void checkCallCostWhenCallIsInPeakTime(){
		newTest
			.withACustomerNamed(CARLY_SIMON).withPhoneNumber(CARLY_SIMON_PHONE_NUM).andPricePlan(STANDARD_TARIFF)
			.withACustomerNamed(TONI_BRAXTON).withPhoneNumber(TONI_BRAXTON_PHONE_NUM).andPricePlan(BUSINESS_TARIFF)
			.withACustomerNamed(TOM_JONES).withPhoneNumber(TOM_JONES_PHONE_NUM).andPricePlan(LEISURE_TARIFF)
			.withACallAt("01/01/11 14:00:00").fromCaller(CARLY_SIMON_PHONE_NUM).toCallee(ELTON_JOHN_PHONE_NUM).thatLastsForSeconds(60 * 60)
			.withACallAt("01/01/11 15:00:00").fromCaller(TONI_BRAXTON_PHONE_NUM).toCallee(ELTON_JOHN_PHONE_NUM).thatLastsForSeconds(60 * 60)
			.withACallAt("01/01/11 16:00:00").fromCaller(TOM_JONES_PHONE_NUM).toCallee(ELTON_JOHN_PHONE_NUM).thatLastsForSeconds(60 * 60)
			.testBillsOfCustomer(CARLY_SIMON+"/"+CARLY_SIMON_PHONE_NUM).chargedWithPricePlan(STANDARD_TARIFF).forPeakSeconds(60 * 60).andOffPeakSeconds(0)
			.testBillsOfCustomer(TONI_BRAXTON+"/"+TONI_BRAXTON_PHONE_NUM).chargedWithPricePlan(BUSINESS_TARIFF).forPeakSeconds(60 * 60).andOffPeakSeconds(0)
			.testBillsOfCustomer(TOM_JONES+"/"+TOM_JONES_PHONE_NUM).chargedWithPricePlan(LEISURE_TARIFF).forPeakSeconds(60 * 60).andOffPeakSeconds(0)
			.createBills();
	}
	
	//Tests that costs can be calculated for different price plans in the off peak time period
	@Test
	public void checkCallCostWhenCallIsInOffPeakTime(){
		newTest
			.withACustomerNamed(CARLY_SIMON).withPhoneNumber(CARLY_SIMON_PHONE_NUM).andPricePlan(STANDARD_TARIFF)
			.withACustomerNamed(TONI_BRAXTON).withPhoneNumber(TONI_BRAXTON_PHONE_NUM).andPricePlan(BUSINESS_TARIFF)
			.withACustomerNamed(TOM_JONES).withPhoneNumber(TOM_JONES_PHONE_NUM).andPricePlan(LEISURE_TARIFF)
			.withACallAt("01/01/11 02:00:00").fromCaller(CARLY_SIMON_PHONE_NUM).toCallee(ELTON_JOHN_PHONE_NUM).thatLastsForSeconds(60 * 60)
			.withACallAt("01/01/11 03:00:00").fromCaller(TONI_BRAXTON_PHONE_NUM).toCallee(ELTON_JOHN_PHONE_NUM).thatLastsForSeconds(60 * 60)
			.withACallAt("01/01/11 04:00:00").fromCaller(TOM_JONES_PHONE_NUM).toCallee(ELTON_JOHN_PHONE_NUM).thatLastsForSeconds(60 * 60)
			.testBillsOfCustomer(CARLY_SIMON+"/"+CARLY_SIMON_PHONE_NUM).chargedWithPricePlan(STANDARD_TARIFF).forPeakSeconds(0).andOffPeakSeconds(60 * 60)
			.testBillsOfCustomer(TONI_BRAXTON+"/"+TONI_BRAXTON_PHONE_NUM).chargedWithPricePlan(BUSINESS_TARIFF).forPeakSeconds(0).andOffPeakSeconds(60 * 60)
			.testBillsOfCustomer(TOM_JONES+"/"+TOM_JONES_PHONE_NUM).chargedWithPricePlan(LEISURE_TARIFF).forPeakSeconds(0).andOffPeakSeconds(60 * 60)
			.createBills();
	}
	
	//Tests that costs can be calculated for different price plans
	//when the call is in the peak and off peak time periods
	@Test
	public void checkCallCostWhenCallStartsInOffPeakTimeAndEndsInPeakTime(){
		newTest
			.withACustomerNamed(CARLY_SIMON).withPhoneNumber(CARLY_SIMON_PHONE_NUM).andPricePlan(STANDARD_TARIFF)
			.withACustomerNamed(TONI_BRAXTON).withPhoneNumber(TONI_BRAXTON_PHONE_NUM).andPricePlan(BUSINESS_TARIFF)
			.withACustomerNamed(TOM_JONES).withPhoneNumber(TOM_JONES_PHONE_NUM).andPricePlan(LEISURE_TARIFF)
			.withACallAt("01/01/11 06:40:00").fromCaller(CARLY_SIMON_PHONE_NUM).toCallee(ELTON_JOHN_PHONE_NUM).thatLastsForSeconds(60 * 60)
			.withACallAt("01/01/11 06:40:00").fromCaller(TONI_BRAXTON_PHONE_NUM).toCallee(ELTON_JOHN_PHONE_NUM).thatLastsForSeconds(60 * 60)
			.withACallAt("01/01/11 06:40:00").fromCaller(TOM_JONES_PHONE_NUM).toCallee(ELTON_JOHN_PHONE_NUM).thatLastsForSeconds(60 * 60)
			.testBillsOfCustomer(CARLY_SIMON+"/"+CARLY_SIMON_PHONE_NUM).chargedWithPricePlan(STANDARD_TARIFF).forPeakSeconds(40 * 60).andOffPeakSeconds(20 * 60)
			.testBillsOfCustomer(TONI_BRAXTON+"/"+TONI_BRAXTON_PHONE_NUM).chargedWithPricePlan(BUSINESS_TARIFF).forPeakSeconds(40 * 60).andOffPeakSeconds(20 * 60)
			.testBillsOfCustomer(TOM_JONES+"/"+TOM_JONES_PHONE_NUM).chargedWithPricePlan(LEISURE_TARIFF).forPeakSeconds(40 * 60).andOffPeakSeconds(20 * 60)
			.createBills();
	}
		
	//Tests that costs can be calculated for different price plans
	//when the call is in the peak and off peak time periods
	@Test
	public void checkCallCostWhenCallStartsInPeakTimeAndEndsInOffPeakTime(){
		newTest
			.withACustomerNamed(CARLY_SIMON).withPhoneNumber(CARLY_SIMON_PHONE_NUM).andPricePlan(STANDARD_TARIFF)
			.withACustomerNamed(TONI_BRAXTON).withPhoneNumber(TONI_BRAXTON_PHONE_NUM).andPricePlan(BUSINESS_TARIFF)
			.withACustomerNamed(TOM_JONES).withPhoneNumber(TOM_JONES_PHONE_NUM).andPricePlan(LEISURE_TARIFF)
			.withACallAt("01/01/11 18:40:00").fromCaller(CARLY_SIMON_PHONE_NUM).toCallee(ELTON_JOHN_PHONE_NUM).thatLastsForSeconds(60 * 60)
			.withACallAt("01/01/11 18:40:00").fromCaller(TONI_BRAXTON_PHONE_NUM).toCallee(ELTON_JOHN_PHONE_NUM).thatLastsForSeconds(60 * 60)
			.withACallAt("01/01/11 18:40:00").fromCaller(TOM_JONES_PHONE_NUM).toCallee(ELTON_JOHN_PHONE_NUM).thatLastsForSeconds(60 * 60)
			.testBillsOfCustomer(CARLY_SIMON+"/"+CARLY_SIMON_PHONE_NUM).chargedWithPricePlan(STANDARD_TARIFF).forPeakSeconds(20 * 60).andOffPeakSeconds(40 * 60)
			.testBillsOfCustomer(TONI_BRAXTON+"/"+TONI_BRAXTON_PHONE_NUM).chargedWithPricePlan(BUSINESS_TARIFF).forPeakSeconds(20 * 60).andOffPeakSeconds(40 * 60)
			.testBillsOfCustomer(TOM_JONES+"/"+TOM_JONES_PHONE_NUM).chargedWithPricePlan(LEISURE_TARIFF).forPeakSeconds(20 * 60).andOffPeakSeconds(40 * 60)
			.createBills();
	}
	
	//Tests that costs can be calculated for different price plans
	//when the call is in the peak and off peak time periods and lasts more than a day
	@Test
	public void checkCallCostWhenCallStartsInOffPeakTimeAndEndsInOffPeakTimeButLastsMoreThanADay(){
		newTest
			.withACustomerNamed(CARLY_SIMON).withPhoneNumber(CARLY_SIMON_PHONE_NUM).andPricePlan(STANDARD_TARIFF)
			.withACustomerNamed(TONI_BRAXTON).withPhoneNumber(TONI_BRAXTON_PHONE_NUM).andPricePlan(BUSINESS_TARIFF)
			.withACustomerNamed(TOM_JONES).withPhoneNumber(TOM_JONES_PHONE_NUM).andPricePlan(LEISURE_TARIFF)
			.withACallAt("01/01/11 04:00:00").fromCaller(CARLY_SIMON_PHONE_NUM).toCallee(ELTON_JOHN_PHONE_NUM).thatLastsForSeconds(49 * 60 * 60)
			.withACallAt("01/01/11 04:00:00").fromCaller(TONI_BRAXTON_PHONE_NUM).toCallee(ELTON_JOHN_PHONE_NUM).thatLastsForSeconds(49 * 60 * 60)
			.withACallAt("01/01/11 04:00:00").fromCaller(TOM_JONES_PHONE_NUM).toCallee(ELTON_JOHN_PHONE_NUM).thatLastsForSeconds(49 * 60 * 60)
			.testBillsOfCustomer(CARLY_SIMON+"/"+CARLY_SIMON_PHONE_NUM).chargedWithPricePlan(STANDARD_TARIFF).forPeakSeconds(24 * 60 * 60).andOffPeakSeconds(25 * 60 * 60)
			.testBillsOfCustomer(TONI_BRAXTON+"/"+TONI_BRAXTON_PHONE_NUM).chargedWithPricePlan(BUSINESS_TARIFF).forPeakSeconds(24 * 60 * 60).andOffPeakSeconds(25 * 60 * 60)
			.testBillsOfCustomer(TOM_JONES+"/"+TOM_JONES_PHONE_NUM).chargedWithPricePlan(LEISURE_TARIFF).forPeakSeconds(24 * 60 * 60).andOffPeakSeconds(25 * 60 * 60)
			.createBills();
	}
	
	//Tests the cost of a call when the year changes
	@Test
	public void checkCallCostOverChangeOfYear(){
		newTest
			.withACustomerNamed(CARLY_SIMON).withPhoneNumber(CARLY_SIMON_PHONE_NUM).andPricePlan(STANDARD_TARIFF)
			.withACallAt("31/12/11 04:00:00").fromCaller(CARLY_SIMON_PHONE_NUM).toCallee(ELTON_JOHN_PHONE_NUM).thatLastsForSeconds(24 * 60 * 60)
			.testBillsOfCustomer(CARLY_SIMON+"/"+CARLY_SIMON_PHONE_NUM).chargedWithPricePlan(STANDARD_TARIFF).forPeakSeconds(12 * 60 * 60).andOffPeakSeconds(12 * 60 * 60)
			.createBills();
	}
	
	//Tests what happens if a CallEnd event is earlier than a CallStart event. Call cost should be 0.0
	@Test
	public void checkCallEndBeforeCallStart(){
		newTest
			.withACustomerNamed(CARLY_SIMON).withPhoneNumber(CARLY_SIMON_PHONE_NUM).andPricePlan(STANDARD_TARIFF)
			.withACallAt("01/01/11 04:59:59").fromCaller(CARLY_SIMON_PHONE_NUM).toCallee(ELTON_JOHN_PHONE_NUM).withoutStart()
			.withACallAt("01/01/11 05:00:00").fromCaller(CARLY_SIMON_PHONE_NUM).toCallee(ELTON_JOHN_PHONE_NUM).withoutEnd()
			.testBillsOfCustomer(CARLY_SIMON+"/"+CARLY_SIMON_PHONE_NUM).chargedWithPricePlan(STANDARD_TARIFF).forPeakSeconds(0).andOffPeakSeconds(0)
			.createBills();
	}
	
	//Tests what happens if a CallStart event and the next CallEnd event have different Callee numbers
	//The result is that the cost is calculated which is wrong
	@Test
	public void checkCallStartWithCallEndToAnotherNumber(){
		newTest
			.withACustomerNamed(CARLY_SIMON).withPhoneNumber(CARLY_SIMON_PHONE_NUM).andPricePlan(STANDARD_TARIFF)
			.withACallAt("01/01/11 04:59:00").fromCaller(CARLY_SIMON_PHONE_NUM).toCallee(ELTON_JOHN_PHONE_NUM).withoutEnd()
			.withACallAt("01/01/11 05:00:00").fromCaller(CARLY_SIMON_PHONE_NUM).toCallee(TOM_JONES_PHONE_NUM).withoutStart()
			.testBillsOfCustomer(CARLY_SIMON+"/"+CARLY_SIMON_PHONE_NUM).chargedWithPricePlan(STANDARD_TARIFF).forPeakSeconds(0).andOffPeakSeconds(60)
			.createBills();
	}
	
	//Tests what happens if the Tariff of a customer changes after he has made a call
	//The cost of all previous calls is calculated with the new Tariff which is wrong
	@Test
	public void checkWhatHappensIfTariffChanges(){
		newTest
			.withACustomerNamed(CARLY_SIMON).withPhoneNumber(CARLY_SIMON_PHONE_NUM).andPricePlan(STANDARD_TARIFF)
			.withACallAt("01/01/11 05:00:00").fromCaller(CARLY_SIMON_PHONE_NUM).toCallee(ELTON_JOHN_PHONE_NUM).thatLastsForSeconds(60)
			.testBillsOfCustomer(CARLY_SIMON+"/"+CARLY_SIMON_PHONE_NUM).chargedWithPricePlan(BUSINESS_TARIFF).forPeakSeconds(0).andOffPeakSeconds(60)
			.createBills();
	}
	
	//Tests what happens if the Tariff of a customer changes after he has made a call
	//The cost of all previous calls is calculated with the new peak times which is wrong
	@Test
	public void checkWhatHappensIfPeakTimesChanges(){
		newTest
			.withACustomerNamed(CARLY_SIMON).withPhoneNumber(CARLY_SIMON_PHONE_NUM).andPricePlan(STANDARD_TARIFF)
			.withACallAt("01/01/11 08:00:00").fromCaller(CARLY_SIMON_PHONE_NUM).toCallee(ELTON_JOHN_PHONE_NUM).thatLastsForSeconds(60)
			.testBillsOfCustomer(CARLY_SIMON+"/"+CARLY_SIMON_PHONE_NUM).chargedWithPricePlan(STANDARD_TARIFF).forPeakSeconds(0).andOffPeakSeconds(60);
		
		DaytimePeakPeriod.PEAK_RATE_START_TIME=9;
		DaytimePeakPeriod.OFF_PEAK_RATE_START_TIME=21;
		
		newTest.createBills();
	}
	
	//Tests what happens if a CallStart event and the next CallEnd event have different Caller numbers and the same Callee
	@Test
	public void checkCallStartWithADifferentNumberAndCallEndWithTheSame(){
		newTest
			.withACustomerNamed(CARLY_SIMON).withPhoneNumber(CARLY_SIMON_PHONE_NUM).andPricePlan(STANDARD_TARIFF)
			.withACustomerNamed(TOM_JONES).withPhoneNumber(TOM_JONES_PHONE_NUM).andPricePlan(LEISURE_TARIFF)
			.withACallAt("01/01/11 04:59:00").fromCaller(CARLY_SIMON_PHONE_NUM).toCallee(ELTON_JOHN_PHONE_NUM).withoutEnd()
			.withACallAt("01/01/11 05:00:00").fromCaller(TOM_JONES_PHONE_NUM).toCallee(ELTON_JOHN_PHONE_NUM).withoutStart()
			.testBillsOfCustomer(CARLY_SIMON+"/"+CARLY_SIMON_PHONE_NUM).chargedWithPricePlan(STANDARD_TARIFF).forPeakSeconds(0).andOffPeakSeconds(0)
			.testBillsOfCustomer(TOM_JONES+"/"+TOM_JONES_PHONE_NUM).chargedWithPricePlan(LEISURE_TARIFF).forPeakSeconds(0).andOffPeakSeconds(0)
			.createBills();
	}
	
	//Tests the cost for a call that lasts only one seconds. Call cost should be 0.0
	@Test
	public void checkCostOfOffPeakCallThatLastsOneSecond(){
		newTest
			.withACustomerNamed(CARLY_SIMON).withPhoneNumber(CARLY_SIMON_PHONE_NUM).andPricePlan(STANDARD_TARIFF)
			.withACallAt("01/01/11 08:00:00").fromCaller(CARLY_SIMON_PHONE_NUM).toCallee(ELTON_JOHN_PHONE_NUM).thatLastsForSeconds(1)
			.testBillsOfCustomer(CARLY_SIMON+"/"+CARLY_SIMON_PHONE_NUM).chargedWithPricePlan(STANDARD_TARIFF).forPeakSeconds(0).andOffPeakSeconds(0);
	}
}
