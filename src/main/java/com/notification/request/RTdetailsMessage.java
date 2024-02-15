package com.notification.request;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "RTdetails")
public class RTdetailsMessage {

	private String policyID;
    private String source;
    private String appNo;
    private String alertID;
    private String channelID;
    private String reqId;
    private String field1;
    private String field2;
    private String field3;
    private String alertMode;
    private AlertData alertData;
    
    public RTdetailsMessage() {
        alertData = new AlertData();
    }

    // Getters and Setters
    public String getPolicyID() {
        return policyID;
    }

    public void setPolicyID(String policyID) {
        this.policyID = policyID;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getAppNo() {
        return appNo;
    }

    public void setAppNo(String appNo) {
        this.appNo = appNo;
    }

    public String getAlertID() {
        return alertID;
    }

    public void setAlertID(String alertID) {
        this.alertID = alertID;
    }

    public String getChannelID() {
        return channelID;
    }

    public void setChannelID(String channelID) {
        this.channelID = channelID;
    }

    public String getReqId() {
        return reqId;
    }

    public void setReqId(String reqId) {
        this.reqId = reqId;
    }

    public String getField1() {
        return field1;
    }

    public void setField1(String field1) {
        this.field1 = field1;
    }

    public String getField2() {
        return field2;
    }

    public void setField2(String field2) {
        this.field2 = field2;
    }

    public String getField3() {
        return field3;
    }

    public void setField3(String field3) {
        this.field3 = field3;
    }

    public String getAlertMode() {
        return alertMode;
    }

    public void setAlertMode(String alertMode) {
        this.alertMode = alertMode;
    }

    public AlertData getAlertData() {
        return alertData;
    }

    public void setAlertData(AlertData alertData) {
        this.alertData = alertData;
    }
}
