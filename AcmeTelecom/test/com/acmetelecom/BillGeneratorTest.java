package com.acmetelecom;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import com.acmetelecom.LineItem;
import com.acmetelecom.customer.Customer;
import com.acmetelecom.customer.Tariff;

public class BillGeneratorTest {
	
	Mockery context = new Mockery();
	Printer printer;
	BillGenerator billGenerator;
	Customer customer = new Customer("Cname", "111111111111", "Standard");
	Tariff tariff = Tariff.Standard;
	List<LineItem> calls;
	String totalBill;
	String custNo;
		
	@Before
	public void setUp(){
		printer = context.mock(Printer.class);
		calls = new ArrayList<LineItem>();
		billGenerator = new BillGenerator();
		custNo=customer.getPhoneNumber();
	}
	
	//Tests if the total cost remains zero and no lines are printed
	@Test
	public void noCallWasMade(){
		context.checking(new Expectations() {{
			oneOf (printer).printHeading(customer.getFullName(), customer.getPhoneNumber(), customer.getPricePlan());
			oneOf (printer).printTotal("0.00");
		}});
		
		billGenerator.send(customer, calls, "0.00", printer);
	}
	
	//Tests one call to see if the total cost can change
	@Test
	public void aCallWasMade(){
		DateTime startDate = new DateTime(2011,5,5,5,0,0);
		final Call call = new Call(new FakeCallStart("111111111111","222222222222",startDate.getMillis()),new FakeCallEnd("111111111111","222222222222",startDate.plusHours(1).getMillis()));
		
		final String expectedCost = calculateExpectedCost(tariff,0,60);
		calls.add(new LineItem(call, (new BigDecimal(expectedCost)).multiply(new BigDecimal(100))));
		
		context.checking(new Expectations() {{
			oneOf (printer).printHeading(customer.getFullName(), customer.getPhoneNumber(), customer.getPricePlan());
			oneOf (printer).printItem("05/05/11 05:00:00", "222222222222", "60:00", expectedCost);
			oneOf (printer).printTotal(expectedCost);
		}});
		
		billGenerator.send(customer, calls, expectedCost, printer);
	}
	
	//Tests more than 1 call to see if they are printed
	@Test
	public void twoCallsWereMade(){
		DateTime startDate = new DateTime(2011,5,5,5,0,0);
		Call call = new Call(new FakeCallStart("111111111111","222222222222",startDate.getMillis()),new FakeCallEnd("111111111111","222222222222",startDate.plusHours(1).getMillis()));
		final String expectedCost1 = calculateExpectedCost(tariff,0,60);
		calls.add(new LineItem(call, (new BigDecimal(expectedCost1)).multiply(new BigDecimal(100))));
		
		startDate = new DateTime(2011,6,6,5,0,0);
		call = new Call(new FakeCallStart("111111111111","222222222222",startDate.getMillis()),new FakeCallEnd("111111111111","222222222222",startDate.plusHours(1).getMillis()));
		final String expectedCost2 = calculateExpectedCost(tariff,0,60);
		calls.add(new LineItem(call, (new BigDecimal(expectedCost2)).multiply(new BigDecimal(100))));
		
		final String expectedTotalCost = calculateExpectedCost(tariff,0,120);
		
		context.checking(new Expectations() {{
			oneOf (printer).printHeading(customer.getFullName(), customer.getPhoneNumber(), customer.getPricePlan());
			oneOf (printer).printItem("05/05/11 05:00:00", "222222222222", "60:00", expectedCost1);
			oneOf (printer).printItem("06/06/11 05:00:00", "222222222222", "60:00", expectedCost2);
			oneOf (printer).printTotal(expectedTotalCost);
		}});
		
		billGenerator.send(customer, calls, expectedTotalCost, printer);
	}
	
	public String calculateExpectedCost(Tariff tariff, long peakSeconds, long offPeakSeconds){
		BigDecimal peakCost = tariff.peakRate().multiply(new BigDecimal(peakSeconds));
		BigDecimal offPeakCost = tariff.offPeakRate().multiply(new BigDecimal(offPeakSeconds));
		
		return String.format("%.2f", peakCost.add(offPeakCost).setScale(0, RoundingMode.HALF_UP).divide(new BigDecimal(100)).doubleValue());
	}
	
	//Classes needed to add fake calls in the log
	private class FakeCallStart extends CallEvent implements CallStartInterface{
		public FakeCallStart(String caller, String callee, long fakeMillis) {
			super(caller, callee, fakeMillis);
		}
	}
	private class FakeCallEnd extends CallEvent implements CallEndInterface{
		public FakeCallEnd(String caller, String callee, long fakeMillis) {
			super(caller, callee, fakeMillis);
		}
	}
}
