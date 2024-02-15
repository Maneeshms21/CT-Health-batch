package com.notification;

import java.awt.PageAttributes.MediaType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.sun.crypto.provider.SunJCE;
import java.io.PrintStream;
import java.security.Security;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

//import org.json.JSONObject;
//import org.apache.http.HttpEntity;
//import org.apache.http.client.methods.CloseableHttpResponse;
//import org.apache.http.client.methods.HttpGet;
//import org.apache.http.client.methods.HttpPost;
//import org.apache.http.entity.StringEntity;
//import org.apache.http.impl.client.CloseableHttpClient;
//import org.apache.http.impl.client.HttpClients;
//import org.apache.http.util.EntityUtils;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.notification.request.BitlyResponse;
import com.notification.request.NotificationMemberDetails;
import com.notification.whatsapprequest.Media;
import com.notification.whatsapprequest.Notification;
import com.notification.whatsapprequest.NotificationSendRequest;
import com.notification.whatsapprequest.NotificationSendResponse;
import com.notification.whatsapprequest.Params;
import com.notification.whatsapprequest.UserDetails;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "SendWhatsAppServlet", urlPatterns = "/api/sendWhatsApp")
public class SendWhatsAppServlet extends HttpServlet {

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		System.out.println("request " + request);

		StringBuilder requestBody = new StringBuilder();
		try (BufferedReader reader = request.getReader()) {
			String line;
			while ((line = reader.readLine()) != null) {
				requestBody.append(line);
			}
		} catch (IOException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().write("Error reading WhatsApp request body: " + e.getMessage());
			return;
		}
		String requestBodyString = requestBody.toString();
		System.out.println("requestBodyStringof WhatsApp " + requestBodyString);
		Gson gson = new Gson();
		NotificationMemberDetails message = gson.fromJson(requestBodyString, NotificationMemberDetails.class);
		String alertId = message.getAlertID();
		String crn = message.getPolicyNo();
		String DocId = fetchdocId(alertId);
		System.out.println("alertId " + alertId);
		System.out.println("crn " + crn);
		System.out.println("DocId " + DocId);
		Boolean mediaType = true;
		
		//Boolean mediaType = findMediaType(DocId, crn);

		String encryptedCrn = createEncrytedCrn(crn);
		System.out.println("encryptedCrn " + encryptedCrn);
		//String pdfDomainLink = "https://cpsspre.adityabirlahealth.com/";
		//String pdfCrnLink = "/resources/EncryptWS/getPDF/?crn=";

		Media media = new Media();
		if (mediaType == true) {
			String mediaLink = createmediaLink(DocId,encryptedCrn);
			System.out.println("mediaLink " + mediaLink);
			media.setMediaLink(mediaLink);
			media.setName(crn);
		}
		String mediaString = media.toString();
		System.out.println("media structure: "+media);
		System.out.println("mediaString structure: "+mediaString);
		NotificationSendRequest nSRequest = new NotificationSendRequest();
		UserDetails uDetail = new UserDetails();
		Notification notification = new Notification();
		Params param = new Params();

		System.out.println("message " + message.getSource());
		if (message.getSource() != null) {
			boolean rs = callGetSMSServiceDetailsBySource(message.getSource());
			System.out.println("rs " + rs);
			if (rs == true) {
			
				try {

					int currentSize = message.getParam().size();

					for (Map.Entry<String, String> entry : message.getParam().entrySet()) {
						String key = entry.getKey();
						String value = entry.getValue();
						int lastIndex= key.length()-1;
						char a= (key.charAt(lastIndex));
						String index = "" + a;
						
						//int index= a-'0';
						notification.getParams().put(index, value);//      insert(index, value);
					}
					// System.out.println(WhatsAppContent);
					notification.getParams().put("media", mediaString);
					uDetail.setNumber(message.getMobileNo());
					// param.setParams(null);
					notification.setSender("918976972883");
					notification.setType("whatsapp");
					notification.setTemplateId(callGetTemplateIdbyAlertId(message.getAlertID()));
					//
					//param.setMedia(media);
					
					nSRequest.setUserDetails(uDetail);
					nSRequest.setNotification(notification);
					
					
					System.out.println("request body: "+ nSRequest);
					NotificationSendResponse responseApi = SendWhatsAppToExternalService(nSRequest);
					if (responseApi == null) {
						response.setContentType("application/json");
						PrintWriter out = response.getWriter();
						out.print("Request Processed error: " );
						return;
					}
					String responseBody= responseApi.toString();
					if (responseApi != null) {
						response.setContentType("application/json");
						PrintWriter out = response.getWriter();
						out.print("Request Processed Successfully: " + responseBody);
						return;
					} else {
						System.out.println("Error: 'param' is missing required keys.");
						response.setContentType("application/json");
						PrintWriter out = response.getWriter();
						out.print("Error: 'param' is missing required keys.");
						return;
					}

				} catch (Exception e) {
					e.printStackTrace();
				}

			} else {
				response.setContentType("application/json");
				PrintWriter out = response.getWriter();
				out.print("Source is not valid. Please check the Source and try again ");
				out.flush();
				return;
			}

		}

		response.setContentType("application/json");

		// Send the response
		PrintWriter out = response.getWriter();
		out.print("Request Processing Failed: ");
		out.flush();
	}

	private String createmediaLink(String docId, String encryptedCrn) {
		String jdbcUrl = "jdbc:sqlserver://10.91.102.20:1433;databaseName=CommServices";
		String username = "ABHI_CC";
		String password = "Birla@1234";
		try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
			if (connection != null) {
				System.out.println("Connected to the SQL Server database!");

				String templateQuery = "Select Media_URL from CommServices.dbo.DocumentLink "
						+ "where DOC_ID = " + "'" + docId
					    + "';" + "";
				PreparedStatement preparedStatement = connection.prepareStatement(templateQuery);
					
				ResultSet resultSet = preparedStatement.executeQuery();

					// Check if there are results
					if (resultSet.next()) {
						// Data fetched successfully
						System.out.println("Data fetched successfully for source: " + docId);
						System.out.println(resultSet.getString("Media_URL")+ encryptedCrn);
						return resultSet.getString("Media_URL")+ encryptedCrn;
					} else {
						// No data found
						System.out.println("No data found for source: " + docId);
						return null;
					}
				}
			
		} catch (SQLException e) {
			System.out.println("Connection failed or error executing Media_URL query: " + e.getMessage());
		}
		return null;
	}

	public static String byte2hex(byte[] b) {
		String hs = "";
		for (byte element : b) {
			String s2 = Integer.toHexString(element & 0xFF);
			if (s2.length() == 1) {
				hs = hs + "0" + s2;
			} else {
				hs = hs + s2;
			}
		}
		return hs.toUpperCase();
	}

	public static byte[] getKey() throws Exception {
		// encryption key
		return "__f5ads_".getBytes();
	}

	private String createEncrytedCrn(String crn) {
		// TODO Auto-generated method stub
		SecretKey deskey;
		try {
			deskey = new SecretKeySpec(getKey(), "DES");
			Cipher c1 = Cipher.getInstance("DES");
			c1.init(1, deskey);
			byte[] cipherByte = c1.doFinal(crn.getBytes());
			return byte2hex(cipherByte);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}

	private Boolean findMediaType(String docId, String crn) {
		// TODO Auto-generated method stub
		try {
			try {
				Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Connection connection = DriverManager.getConnection("jdbc:oracle:thin:@10.92.2.76:1521:MTPREPRD",
					"MTPREPROD", "MTPREPROD");
			Statement statement = connection.createStatement();
			String FindDocQuery = "Select Medialinkpresence from table_name where crn= " + crn + " and IXPRDT = "
					+ docId;
			ResultSet rs1 = statement.executeQuery(FindDocQuery);
			if (rs1 != null) {
				return true;
			}
			return false;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	private String fetchdocId(String alertId) {
		String jdbcUrl = "jdbc:sqlserver://10.91.102.20:1433;databaseName=CommServices";
		String username = "ABHI_CC";
		String password = "Birla@1234";
		try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
			if (connection != null) {
				System.out.println("Connected to the SQL Server database!");

				String templateQuery = "Select Doc_ID from CommServices.dbo.Alert_to_Template "
						+ "where Alert_ID = " + "'" + alertId
					    + "';" + "";
				System.out.println("templateQuery: " + templateQuery);
				PreparedStatement preparedStatement = connection.prepareStatement(templateQuery);
				
				ResultSet resultSet = preparedStatement.executeQuery();

					// Check if there are results
					if (resultSet.next()) {
						// Data fetched successfully
						System.out.println("Data fetched successfully for source: " + alertId);
						return resultSet.getString("Doc_ID");
					} else {
						// No data found
						System.out.println("No data found for source: " + alertId);
						return null;
					}
				}
			
		} catch (SQLException e) {
			System.out.println("Connection failed or error executing fetchDocId: " + e.getMessage());
		}
		return null;
	}

	private static boolean recordAvailabilityCheckInDB(String source, String alertID) {
		String jdbcUrl = "jdbc:sqlserver://10.91.102.20:1433;databaseName=CommServices";

		String username = "ABHI_CC";
		String password = "Birla@1234";

		Connection connection = null;

		try {
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
			connection = DriverManager.getConnection(jdbcUrl, username, password);
			if (connection != null) {
				System.out.println("Connected to the database!");
				String selectQuery = "SELECT * FROM CommServices.dbo.CommSource WHERE \"Source\"= '" + source
						+ "' AND TemplateID = '" + alertID + "'";
				try (Statement statement = connection.createStatement();
						ResultSet rs = statement.executeQuery(selectQuery)) {

					if (rs.next()) {
						System.out.println("Record is already present in Database. " + rs.getString("TemplateID"));
						return true;
					}
				}

				// Prepare and execute the stored procedure call /*
				try (CallableStatement statement = connection.prepareCall("{call GetNotificationLogs}")) {
					// Execute the stored procedure
					ResultSet resultSet = statement.executeQuery();

					// Process the retrieved data while (resultSet.next()) {
					long logsId = resultSet.getLong("LOGS_ID");
					String urlRequestResponse = resultSet.getString("URL_Request_Response");
					String transactionDate = resultSet.getString("TRANSACTON_DATE");
					String alertMode = resultSet.getString("AlertMode");

					// Perform operations with the retrieved data (example: print the results)
					System.out.println("LOGS_ID: " + logsId + ",  URL_Request_Response: " + urlRequestResponse
							+ ", TRANSACTON_DATE: " + transactionDate + ", AlertMode: " + alertMode);
					return true;
				} catch (SQLException e) {
					System.out.println("Error executing stored procedure: " + e.getMessage());
				}
			}
		} catch (Exception e) {
			System.out.println("Connection failed: " + e.getMessage());
		}
		return false;
	}

	public static boolean recordInsertionInLogsTable(String urlRequestResponse, String transactionDate,
			String alertMode) {
		String jdbcUrl = "jdbc:sqlserver://10.91.102.20:1433;databaseName=CommServices";

		String username = "ABHI_CC";
		String password = "Birla@1234";

		try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
			if (connection != null) {
				System.out.println("Connected to the database!");

				String insertQuery = "INSERT INTO CommServices.dbo.SENDNOTIFICATION_LOGS"
						+ "( URL_Request_Response, TRANSACTON_DATE, AlertMode)" + "VALUES( '" + urlRequestResponse
						+ "', '" + transactionDate + "', '" + alertMode + "');" + "";
				try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
					preparedStatement.setString(1, urlRequestResponse);
					preparedStatement.setString(2, transactionDate);
					preparedStatement.setString(3, alertMode);

					int result = preparedStatement.executeUpdate();
					if (result > 0) {
						System.out.println("Successfully inserted");
						return true;
					} else {
						System.out.println("Unsuccessful insertion");
					}
				}

				try (CallableStatement statement = connection.prepareCall("{CALL InsertNotificationLog( ?, ?, ?)}")) {
					statement.setString(2, urlRequestResponse);
					statement.setString(3, transactionDate);
					statement.setString(4, alertMode);

					// Execute the stored procedure statement.execute();

					System.out.println("Stored procedure executed successfully for inserting records.");
					return true;
				} catch (SQLException e) {
					System.out.println("Error executing stored procedure: " + e.getMessage());
				}

			}
		} catch (SQLException e) {
			System.out.println("Connection failed: " + e.getMessage());
		}
		return false;
	}

	public static boolean callGetSMSServiceDetailsBySource(String sourceFilter) {
		String jdbcUrl = "jdbc:sqlserver://10.91.102.20:1433;databaseName=CommServices";
		String username = "ABHI_CC";
		String password = "Birla@1234";
		try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
			if (connection != null) {
				System.out.println("Connected to the SQL Server database!");

				// Call the stored procedure
				try (CallableStatement callableStatement = connection
						.prepareCall("{CALL CommServices.dbo.GetSMSServiceDetailsBySource(?)}")) {
					callableStatement.setString(1, sourceFilter);

					// Execute the stored procedure
					ResultSet resultSet = callableStatement.executeQuery();

					// Check if there are results
					if (resultSet.next()) {
						// Data fetched successfully
						System.out.println("Data fetched successfully for source: " + sourceFilter);
						return true;
					} else {
						// No data found
						System.out.println("No data found for source: " + sourceFilter);
						return false;
					}
				}
			}
		} catch (SQLException e) {
			System.out.println("Connection failed or error executing stored procedure: " + e.getMessage());
		}
		return false;
	}

	public static String callGetTemplateIdbyAlertId(String AlertId) {
		String jdbcUrl = "jdbc:sqlserver://10.91.102.20:1433;databaseName=CommServices";
		String username = "ABHI_CC";
		String password = "Birla@1234";
		try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
			if (connection != null) {
				System.out.println("Connected to the SQL Server database!");

				String templateQuery = "Select Template_ID from CommServices.dbo.Alert_to_Template "
						+ "where Alert_ID = " + "'" + AlertId
					    + "';" + "";
				PreparedStatement preparedStatement = connection.prepareStatement(templateQuery);
					
				ResultSet resultSet = preparedStatement.executeQuery();

					// Check if there are results
					if (resultSet.next()) {
						// Data fetched successfully
						System.out.println("Data fetched successfully for source: " + AlertId);
						return resultSet.getString("Template_ID");
					} else {
						// No data found
						System.out.println("No data found for source: " + AlertId);
						return null;
					}
				}
			
		} catch (SQLException e) {
			System.out.println("Connection failed or error executing Template_ID query: " + e.getMessage());
		}
		return null;
	}
//	public static String processDatabaseToFetchSMSContent(String alertId) {
//		String JDBC_URL = "jdbc:oracle:thin:@10.92.2.76:1521:MTPREPRD";
//		String USERNAME = "MTPREPROD";
//		String PASSWORD = "MTPREPROD";
//		String smsContent = null; // Variable to store the result
//
//		try (Connection con = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
//				PreparedStatement stmt = con
//						.prepareStatement("SELECT MSGXALRT FROM PXALRTCRFGXM WHERE NMXALRTXCFG = ?")) {
//			// PXALRTCRFGXM
//			stmt.setString(1, alertId);
//
//			try (ResultSet rs = stmt.executeQuery()) {
//				if (rs.next()) {
//					smsContent = rs.getString("MSGXALRT");
//				}
//			}
//
//		} catch (SQLException e) {
//			// Handle specific SQLExceptions separately if needed
//			e.printStackTrace();
//			return "Error: Unable to retrieve SMS content from the database. " + e.getMessage();
//		} catch (Exception e) {
//			// Handle other exceptions
//			e.printStackTrace();
//			return "Error: " + e.getMessage();
//		}
//
//		return smsContent;
//	}

	private NotificationSendResponse SendWhatsAppToExternalService(NotificationSendRequest request) throws Exception {
		// Implement code to send a POST request to the external service
		// Use a library like Apache HttpClient or java.net.HttpURLConnection
		CloseableHttpClient httpClient = HttpClients.createDefault();
		String EndpointUrl = "https://app.yellowmessenger.com/api/engagements/notifications/v2/push?bot=x1580738723878&=";
		HttpPost httprequest = new HttpPost(EndpointUrl);
		JSONObject jsonObject = new JSONObject(new Gson().toJson(request));
		String body = jsonObject.toString();
		
		System.out.println("final request body: " + body);
		//if (body != null) {
			
			httprequest.addHeader("Content-Type", "application/json");
			httprequest.addHeader("x-api-key", "SMdbAbb8My6PvmzoZMJFoLbyRy6J_TCwFvW4lPUa");
						
			StringEntity stringEntity = new StringEntity(body);
			httprequest.setEntity(stringEntity);
			
			JSONObject jsObject = new JSONObject(new Gson().toJson(httprequest));
			String body1 = jsonObject.toString();
			System.out.println("final request body to hit: " + body1);
//			

			try (CloseableHttpResponse response = httpClient.execute(httprequest)) {
				int statusCode = 200;//response.getStatusLine().getStatusCode();
				System.out.println("Response Status Code: " + statusCode);

				if (statusCode >= 200 && statusCode < 300) {
					HttpEntity entity = response.getEntity();
					if (entity != null) {
						String result = EntityUtils.toString(entity);
						String urlRequestResponse = "preurl " + EndpointUrl + " result " + result;
						Date currentDate = new Date();
						String transactionDate = convertToString(currentDate, "yyyy-MM-dd HH:mm:ss");
						boolean logInsert = recordInsertionInLogsTable(urlRequestResponse, transactionDate, "WhatsApp");
						if (logInsert == true) {
							System.out.println("Successfull Insertion in Logs table");
						}
						System.out.println("Response Body: " + result);
						return new Gson().fromJson(result, NotificationSendResponse.class);
					}
				} else {
					// Handle non-successful status codes here
					// Log the response and status code for analysis
					HttpEntity entity = response.getEntity();
					if (entity != null) {
					String result = EntityUtils.toString(entity);
					String urlRequestResponse = "preurl " + EndpointUrl + " result " + result;
					Date currentDate = new Date();
					String transactionDate = convertToString(currentDate, "yyyy-MM-dd HH:mm:ss");
					boolean logInsert = recordInsertionInLogsTable(urlRequestResponse, transactionDate, "WhatsApp");
					if (logInsert == true) {
						System.out.println("Bitly service responded with non-successful status: " + statusCode);
					}
					System.out.println("Bitly service responded with non-successful status: " + statusCode);
				}
				}
			} catch (IOException e) {
				// Handle specific exceptions related to HTTP client calls
				System.out.println("Exception occurred while calling WhatsApp service: " + e.getMessage());
			} finally {
				try {
					httpClient.close(); // Close the HttpClient in the finally block
				} catch (IOException e) {
					// Handle exception related to closing HttpClient
					System.out.println("Exception occurred while closing HttpClient: " + e.getMessage());
				}
			}
		//}
		return null;
	}

	
//	public static boolean recordInsertionInSqlServerLogsTable(String urlRequestResponse, String transactionDate,
//			String alertMode) {
//		String jdbcUrl = "jdbc:sqlserver://10.91.102.20:1433;databaseName=CommServices";
//		String username = "ABHI_CC";
//		String password = "Birla@1234";
//
//		try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
//			if (connection != null) {
//				System.out.println("Connected to the SQL Server database!");
//
//				String insertQuery = "EXEC CommServices.dbo.InsertNotificationLog ?, ?, ?";
//
//				try (CallableStatement callableStatement = connection.prepareCall(insertQuery)) {
//					callableStatement.setString(1, urlRequestResponse);
//					callableStatement.setString(2, transactionDate);
//					callableStatement.setString(3, alertMode);
//
//					boolean hasResults = callableStatement.execute();
//					if (!hasResults) {
//						System.out.println("Successfully inserted");
//						return true;
//					} else {
//						System.out.println("Unsuccessful insertion");
//					}
//				}
//			}
//		} catch (SQLException e) {
//			System.out.println("Connection failed: " + e.getMessage());
//		}
//		return false;
//	}

	public static String convertToString(Date inputDate, String format) {
		DateFormat dateFormat = new SimpleDateFormat(format);
		return dateFormat.format(inputDate);
	}

}
