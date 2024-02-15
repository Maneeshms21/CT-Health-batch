package com.notification;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.notification.request.BitlyRequest;
import com.notification.request.BitlyResponse;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "testbitly", urlPatterns = "/api/testbitly")
public class testbitly extends HttpServlet {
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		System.out.println("request " + request);
		//String result = "kdfndkl/abc.com";
		//System.out.println("Response Body: " + result);
		response.setContentType("application/json");
		response.setStatus(200);
		PrintWriter out = response.getWriter();
		out.print("Request Processed Successfully: " + "www.src.com");
		out.flush();
		//new Gson().fromJson(result, HttpServletResponse.class);
		return;
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		System.out.println("request " + request);
		//String result = "kdfndkl/abc.com";
		//System.out.println("Response Body: " + result);
		response.setContentType("application/json");
		response.setStatus(200);
		PrintWriter out = response.getWriter();
		out.print("Request Processed Successfully of Kalerya API");
		out.flush();
		//new Gson().fromJson(result, HttpServletResponse.class);
		return;

	}
}	

	