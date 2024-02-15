package com.notification.whatsapprequest;

import java.util.HashMap;
import java.util.Map;

public class Notification {
	
	private String type;
    private String sender;
    private String templateId;
   // private Params params;
    private Map<String, String> params = new HashMap<>();

    
    // Getter and setter for type
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    // Getter and setter for sender
    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    // Getter and setter for templateId
    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

	public Map<String, String> getParams() {
		return params;
	}

	public void setParams(Map<String, String> params) {
		//Media media;
		this.params = params;
	}

    // Getter and setter for params
//    public Params getParams() {
//        return params;
//    }
//
//    public void setParams(Params params) {
//        this.params = params;
//    }
}
