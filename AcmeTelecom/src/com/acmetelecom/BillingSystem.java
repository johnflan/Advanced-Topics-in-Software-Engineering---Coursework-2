package com.acmetelecom;

import java.io.FileInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.acmetelecom.customer.CentralCustomerDatabase;
import com.acmetelecom.customer.CentralTariffDatabase;
import com.acmetelecom.customer.Customer;
import com.acmetelecom.customer.CustomerDatabase;
import com.acmetelecom.customer.Tariff;
import com.acmetelecom.customer.TariffLibrary;
import com.acmetelecom.logcall.CallFrom;
import com.acmetelecom.logcall.CallLogInterface;

public class BillingSystem implements CallLogInterface{

    private List<CallEvent> callLog = new ArrayList<CallEvent>();
    private final CustomerDatabase centralCustomerDatabase;
    private final TariffLibrary centralTariffDatabase;
    private final BillGeneratorInterface billGenerator;
    
    static final String PEAK_RATE_START_TIME = "peak_rate_start";
	static final String OFF_PEAK_RATE_START_TIME = "off-peak_rate_start";
    
	@Deprecated
	/*
	 * This constructor has been replaced by BillingSystem(CustomerDatabase, TariffLibrary, BillGeneratorInterface)
	 * The old constructor is needed only to maintain backwards compatibility
	 */
	public BillingSystem(){
		this.centralCustomerDatabase = CentralCustomerDatabase.getInstance();
    	this.centralTariffDatabase = CentralTariffDatabase.getInstance();
    	this.billGenerator = new BillGenerator();
    	loadConfigurationProperties();
	}
	
	public BillingSystem(CustomerDatabase customerDB, TariffLibrary tariffDB, BillGeneratorInterface billGen){
    	this.centralCustomerDatabase = customerDB;
    	this.centralTariffDatabase = tariffDB;
    	this.billGenerator = billGen;
    	loadConfigurationProperties();
    }
    
    private void loadConfigurationProperties() {
    	Properties props = new Properties();
    	String peak_rate_start = "";
    	String off_peak_rate_start = "";
        //Initialise the system parameters
      	try {
  				props.load(new FileInputStream("billing_system.properties"));
  			} catch (Exception e) {
  				// TODO Auto-generated catch block
  				System.out.println("Unable to load configuration file \"billing_system.properties\"");
  			} 
     		
		try {
			if(props.containsKey(PEAK_RATE_START_TIME))
				peak_rate_start = props.getProperty(PEAK_RATE_START_TIME);

			
			if(props.containsKey(OFF_PEAK_RATE_START_TIME))
     			off_peak_rate_start = props.getProperty(OFF_PEAK_RATE_START_TIME);

			
			if (off_peak_rate_start == null || peak_rate_start == null)
				throw new Exception("Configuration error!");	
 				
		} catch (Exception e) {
			e.getMessage();
 		}
		
		DaytimePeakPeriod.OFF_PEAK_RATE_START_TIME = Integer.parseInt(off_peak_rate_start);
		DaytimePeakPeriod.PEAK_RATE_START_TIME = Integer.parseInt(peak_rate_start);
	}
    
    //DSL Start call methods
    public CallFrom startCall(){
    	return new LogCallStart(this);
    }
    
    public CallFrom endCall(){
    	return new LogCallEnd(this);
    }
    
    @Deprecated
    /*
     * This method has been replaced by the new DSL startCall() method
     */
    public void callInitiated(String caller, String callee) {
        callLog.add(new CallStart(caller, callee));
    }
    
    @Deprecated
    /*
     * This method has been replaced by the new DSL startCall() method
     */
    public void callInitiated(String caller, String callee, long timeStamp) {
        callLog.add(new CallStart(caller, callee, timeStamp));
    }

    @Deprecated 
    /*
     * This method has been replaced by the new DSL endCall() method
     */
    public void callCompleted(String caller, String callee) {
        callLog.add(new CallEnd(caller, callee));
    }
    
    @Deprecated 
    /*
     * This method has been replaced by the new DSL endCall() method
     */
    public void callCompleted(String caller, String callee, long timeStamp) {
        callLog.add(new CallEnd(caller, callee, timeStamp));
    }

    public void createCustomerBills() {
        List<Customer> customers = centralCustomerDatabase.getCustomers();
        for (Customer customer : customers) {
            createBillFor(customer);
        }
        callLog.clear();
    }

    private void createBillFor(Customer customer) {
        List<CallEvent> customerEvents = new ArrayList<CallEvent>();
        for (CallEvent callEvent : callLog) {
            if (callEvent.getCaller().equals(customer.getPhoneNumber())) {
                customerEvents.add(callEvent);
            }
        }

        List<Call> calls = new ArrayList<Call>();

        CallEvent start = null;
        for (CallEvent event : customerEvents) {
            if (event instanceof CallStartInterface) {
                start = event;
            }
            if (event instanceof CallEndInterface && start != null) {
                calls.add(new Call((CallStartInterface) start,(CallEndInterface) event));
                start = null;
            }
        }

        BigDecimal totalBill = new BigDecimal(0);
        List<LineItem> items = new ArrayList<LineItem>();

        for (Call call : calls) {
        	
            Tariff tariff = centralTariffDatabase.tarriffFor(customer);
            
            BigDecimal cost = BigDecimal.ZERO;
           
            BigDecimal costPeak = new BigDecimal(call.durationPeakSeconds()).multiply(tariff.peakRate());
            BigDecimal costOffPeak = new BigDecimal(call.durationOffPeakSeconds()).multiply(tariff.offPeakRate());
            
            cost=costPeak.add(costOffPeak);
            
            cost = cost.setScale(0, RoundingMode.HALF_UP);
            
            BigDecimal callCost = cost;
            
            totalBill = totalBill.add(callCost);
            
            items.add(new LineItem(call, callCost));
        }
        billGenerator.send(customer, items, MoneyFormatter.penceToPounds(totalBill), HtmlPrinter.getInstance());
    }
}
