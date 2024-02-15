package com.notification.whatsapprequest;

//import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Params {
	private Map<String, String> Param = new HashMap<>();
   
    
	private Media media;

    // Getter and setter for Param
    
	public Map<String, String> getParam() {
		return Param;
	}

	public void setParam(Map<String, String> param) {
		Param = param;
	}


	// Getter and setter for media
    public Media getMedia() {
        return media;
    }

	public void setMedia(Media media) {
        this.media = media;
    }
}

