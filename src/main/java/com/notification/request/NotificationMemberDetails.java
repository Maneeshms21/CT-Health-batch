package com.notification.request;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class NotificationMemberDetails {

	private String customerId;
    private String alertID;
    private String source;
    private String policyNo;
    private String mobileNo;
    private Map<String, String> param = new HashMap<>();


    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getAlertID() {
        return alertID;
    }

    public void setAlertID(String alertID) {
        this.alertID = alertID;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getPolicyNo() {
        return policyNo;
    }

    public void setPolicyNo(String policyNo) {
        this.policyNo = policyNo;
    }

    public String getMobileNo() {
        return mobileNo;
    }

    public void setMobileNo(String mobileNo) {
        this.mobileNo = mobileNo;
    }


    public Map<String, String> getParam() {
		return param;
	}

	public void setParam(Map<String, String> param) {
		this.param = param;
	}
	


	
}
