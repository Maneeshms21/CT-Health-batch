package com.notification.request;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Alertdata")
public class AlertData {

	
	private String mobileNo;
    private String emailId;
    private String bccEmailId;
    private List<String> alerts = new ArrayList<>();
    
	public String getMobileNo() {
		return mobileNo;
	}
	public void setMobileNo(String mobileNo) {
		this.mobileNo = mobileNo;
	}
	public String getEmailId() {
		return emailId;
	}
	public void setEmailId(String emailId) {
		this.emailId = emailId;
	}
	public String getBccEmailId() {
		return bccEmailId;
	}
	public void setBccEmailId(String bccEmailId) {
		this.bccEmailId = bccEmailId;
	}
	public List<String> getAlerts() {
		return alerts;
	}
	public void setAlerts(List<String> alerts) {
		this.alerts = alerts;
	}

    
    
}
