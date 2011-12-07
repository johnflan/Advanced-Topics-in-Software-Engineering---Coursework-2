package com.acmetelecom;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ BillingSystemTest.class, CallTest.class,
		DatetimePeakPeriodTest.class, MoneyFormatterTest.class, LogCallTest.class,
		BillGeneratorTest.class })
public class AllTests {

}
