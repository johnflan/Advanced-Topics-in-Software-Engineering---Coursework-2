package com.acmetelecom;


import static org.junit.Assert.*;
import java.math.BigDecimal;
import org.junit.Test;

public class MoneyFormatterTest {
	@Test
	public void testMoneyConversion() {
		BigDecimal pence = new BigDecimal(0);
		assertEquals("0.00",MoneyFormatter.penceToPounds(pence));
		
		pence = new BigDecimal(40);
		assertEquals("0.40",MoneyFormatter.penceToPounds(pence));
		
		pence = new BigDecimal(100);
		assertEquals("1.00",MoneyFormatter.penceToPounds(pence));
		
		pence = new BigDecimal(111);
		assertEquals("1.11",MoneyFormatter.penceToPounds(pence));
		
		pence = new BigDecimal(222);
		assertFalse(MoneyFormatter.penceToPounds(pence).equals("02.22"));
		assertFalse(MoneyFormatter.penceToPounds(pence).equals("2.220"));
	}

}
