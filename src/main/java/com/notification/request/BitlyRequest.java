package com.notification.request;

public class BitlyRequest {

	  private String Source;
	  private String AlertMode;
	    private ObjRequest objRequest;

	    // Getters and Setters

	    
	    


	    public ObjRequest getObjRequest() {
	        return objRequest;
	    }
	    public String getSource() {
			return Source;
		}
		public void setSource(String source) {
			Source = source;
		}
		public String getAlertMode() {
			return AlertMode;
		}
		public void setAlertMode(String alertMode) {
			AlertMode = alertMode;
		}
		public void setObjRequest(ObjRequest objRequest) {
	        this.objRequest = objRequest;
	    }
}
