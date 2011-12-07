package com.acmetelecom;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class HtmlPrinterTest {
	
	Printer printer;
	
	@Before
	public void setUp(){
		printer = HtmlPrinter.getInstance();
	}

	@Test
	public void testPrintHeading() {
		printer.printHeading("", "", "");
	}
	
	@Test
	public void testPrintItem() {
		printer.printItem("", "", "", "");
	}
	
	@Test
	public void testPrintTotal() {
		printer.printTotal("");
	}
	

}
