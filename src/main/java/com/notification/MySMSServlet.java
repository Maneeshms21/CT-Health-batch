package com.notification;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import com.google.gson.Gson;
import com.notification.request.NotificationMemberDetails;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "MySMSServlet", urlPatterns = "/api/smspost")
public class MySMSServlet extends HttpServlet {

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		System.out.println("request " + request);
		String accessId = request.getHeader("accessid");
		if ("150903b4-bb24-4725-848f-e4d39a612251".equals(accessId)) {
			StringBuilder requestBody = new StringBuilder();
			try (BufferedReader reader = request.getReader()) {
				String line;
				while ((line = reader.readLine()) != null) {
					requestBody.append(line);
				}
			} catch (IOException e) {
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				response.getWriter().write("Error reading SMS request body: " + e.getMessage());
				return;
			}
			String requestBodyString = requestBody.toString();
			System.out.println("requestBodyStringof SMS " + requestBodyString);
			Gson gson = new Gson();
			NotificationMemberDetails message = gson.fromJson(requestBodyString, NotificationMemberDetails.class);
			System.out.println("message " + message.getSource());
			Date currentDate = new Date();
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(currentDate);
			// Timestamp expiryTimeStamp = new Timestamp(calendar.getTimeInMillis());
			String transactionDate = convertToString(currentDate, "yyyy-MM-dd HH:mm:ss");
			String reqUrl = "http://10.158.1.60/WS_SMS/Service.asmx/SendSMS";
			String responseApi = null;
			if (message.getSource() != null) {
				boolean rs = callGetSMSServiceDetailsBySource(message.getSource());
				System.out.println("rs " + rs);
				if (rs == true) {
					if(message.getAlertID()!=null && message.getMobileNo()!=null  ) {				
							String strippedNumber = message.getMobileNo().replaceAll("\\+91", "");
							if (isTenDigitNumber(strippedNumber)) {
					//mobileno validation alertid
					String smsContent = processDatabaseToFetchSMSContent(message.getAlertID());
					System.out.println("smsContent " + smsContent);
					try {
						if (smsContent != null) {
							int minSize = 4;
							int currentSize = message.getParam().size();
							if (currentSize < minSize) {
								boolean insertInLog = recordInsertionInSqlServerLogsTable(
										reqUrl + " Request: " + requestBodyString + " Response: "
												+ "Error: Minimum size of 'param' should be 4",
										transactionDate, "SMS");
								System.out.println("Error: Minimum size of 'param' should be " + minSize);
								response.setContentType("application/json");
								PrintWriter out = response.getWriter();
								out.print("Error: Minimum size of 'param' should be " + minSize);
								return;
							} else {
								boolean hasRequiredKeys = true;
								for (int i = 1; i <= minSize; i++) {
									String requiredKey = "ALERTV" + i;
									if (!message.getParam().containsKey(requiredKey)) {
										hasRequiredKeys = false;
										break;
									}
								}

								if (hasRequiredKeys) {
									for (Map.Entry<String, String> entry : message.getParam().entrySet()) {
										String key = entry.getKey();
										String value = entry.getValue();
										smsContent = smsContent.replaceAll("\\{" + key + "\\}", value);
									}
									System.out.println(smsContent);
									responseApi = callKaleryaGetApi(strippedNumber, smsContent);
									if (responseApi != null) {
										boolean insertInSmsWhatsapp=executeInsertSendSMS_WhatsApp(
												requestBodyString,message.getSource(),message.getAlertID(),transactionDate,
												transactionDate,2);
										boolean insertInLog = recordInsertionInSqlServerLogsTable(
												reqUrl + " Request: " + requestBodyString + " Response: " + responseApi,
												transactionDate, "SMS");
										response.setContentType("application/json");
										PrintWriter out = response.getWriter();
										out.print("Request Processed Successfully: " + responseApi);
										return;
									}
								} else {
									boolean insertInLog = recordInsertionInSqlServerLogsTable(
											reqUrl + " Request: " + requestBodyString + " Response: "
													+ "Error: 'param' is missing required keys.",
											transactionDate, "SMS");
									System.out.println("Error: 'param' is missing required keys.");
									response.setContentType("application/json");
									PrintWriter out = response.getWriter();
									out.print("Error: 'param' is missing required keys." + minSize);
									return;
								}

							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
							else {
								boolean insertInLog = recordInsertionInSqlServerLogsTable(
										reqUrl + " Request: " + requestBodyString + " Response: "
												+ "Please check the MobileNumber and try again ",
										transactionDate, "SMS");
								response.setContentType("application/json");
								PrintWriter out = response.getWriter();
								out.print("Please check the MobileNumber and try again ");
								out.flush();
								return;
							}
							}
					else {
						boolean insertInLog = recordInsertionInSqlServerLogsTable(
								reqUrl + " Request: " + requestBodyString + " Response: "
										+ "Please check the AlertId/MobileNumber and try again ",
								transactionDate, "SMS");
						response.setContentType("application/json");
						PrintWriter out = response.getWriter();
						out.print("Please check the AlertId/MobileNumber and try again ");
						out.flush();
						return;
					}
					} else {
					boolean insertInLog = recordInsertionInSqlServerLogsTable(
							reqUrl + " Request: " + requestBodyString + " Response: "
									+ "Source is not valid. Please check the Source and try again ",
							transactionDate, "SMS");
					response.setContentType("application/json");
					PrintWriter out = response.getWriter();
					out.print("Source is not valid. Please check the Source and try again ");
					out.flush();
					return;
				}

			}
			boolean insertInLog = recordInsertionInSqlServerLogsTable(
					reqUrl + " Request: " + requestBodyString + " Response: " + "Source not found in Request ",
					transactionDate, "SMS");
			System.out.println("Source not found in Request: " + message);
			response.setContentType("application/json");
			PrintWriter out = response.getWriter();
			out.print("Source not found in Request ");
			out.flush();
			return;
		} else {
			System.out.println("Verify the accessid ");
			response.setContentType("application/json");
			PrintWriter out = response.getWriter();
			out.print("Verify the accessid ");
			out.flush();
			return;
		}
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
				String selectQuery = "SELECT * FROM CommServices.dbo.SendSMS_WhatsApp WHERE \"Source\"= '" + source
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

	/*
	 * public static boolean callGetSMSServiceDetailsBySource(String sourceFilter) {
	 * String jdbcUrl =
	 * "jdbc:sqlserver://10.91.102.20:1433;databaseName=CommServices"; String
	 * username = "ABHI_CC"; String password = "Birla@1234"; try (Connection
	 * connection = DriverManager.getConnection(jdbcUrl, username, password)) { if
	 * (connection != null) {
	 * System.out.println("Connected to the SQL Server database!");
	 * 
	 * // Call the stored procedure try (CallableStatement callableStatement =
	 * connection.
	 * prepareCall("{CALL CommServices.dbo.GetSMSServiceDetailsBySource(?)}")) {
	 * callableStatement.setString(1, sourceFilter);
	 * 
	 * // Execute the stored procedure ResultSet resultSet =
	 * callableStatement.executeQuery();
	 * 
	 * // Check if there are results if (resultSet.next()) { // Data fetched
	 * successfully System.out.println("Data fetched successfully for source: " +
	 * sourceFilter); return true; } else { // No data found
	 * System.out.println("No data found for source: " + sourceFilter); return
	 * false; } } } } catch (SQLException e) {
	 * System.out.println("Connection failed or error executing stored procedure: "
	 * + e.getMessage()); } return false; }
	 */
	public static boolean callGetSMSServiceDetailsBySource(String sourceFilter) {
		String jdbcUrl = "jdbc:sqlserver://10.91.102.20:1433;databaseName=CommServices";
		String username = "ABHI_CC";
		String password = "Birla@1234";
		try {
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

			Connection connection = DriverManager.getConnection(jdbcUrl, username, password);

			String sql = "SELECT * FROM CommServices.dbo.CommSource Where Source='" + sourceFilter + "'";
			PreparedStatement preparedStatement = connection.prepareStatement(sql);
			ResultSet resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				return true;
			}
		} catch (Exception e) {
			System.out.println("Exception occurred while fetching Source");
		}
		return false;
	}

	/*
	 * private static String processDatabaseToFetchSMSContent(String alertId) {
	 * String smsContent = null; // Variable to store the result
	 * 
	 * try (Connection con = DriverManager.getConnection(
	 * "jdbc:oracle:thin:@10.92.2.23:1521,DatabaseName=MTPREPRD", "MTPREPROD",
	 * "MTPREPROD"); PreparedStatement stmt = con.
	 * prepareStatement("SELECT MSGXALRT FROM PXALRTCRFGXM WHERE NMXALRTXCFG = ?");
	 * ) { stmt.setString(1, alertId); try (ResultSet rs = stmt.executeQuery()) { if
	 * (rs.next()) { smsContent = rs.getString("MSGXALRT"); } }
	 * 
	 * } catch (Exception e) { return "Error: " + e.getMessage(); } return
	 * smsContent; }
	 */
	public static String processDatabaseToFetchSMSContent(String alertId) {
		String JDBC_URL = "jdbc:oracle:thin:@10.92.2.76:1521:MTPREPRD";
		String USERNAME = "MTPREPROD";
		String PASSWORD = "MTPREPROD";
		String smsContent = null; // Variable to store the result

		try (Connection con = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
				PreparedStatement stmt = con
						.prepareStatement("SELECT MSGXALRT FROM PXALRTXCFGXM WHERE NMXALRTXCFG = ?")) {
			// PXALRTCRFGXM
			stmt.setString(1, alertId);

			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					smsContent = rs.getString("MSGXALRT");
				}
			}

		} catch (SQLException e) {
			// Handle specific SQLExceptions separately if needed
			e.printStackTrace();
			return "Error: Unable to retrieve SMS content from the database. " + e.getMessage();
		} catch (Exception e) {
			// Handle other exceptions
			e.printStackTrace();
			return "Error: " + e.getMessage();
		}

		return smsContent;
	}

	public static String callKaleryaGetApi(String mobileNumber, String smsContent) throws IOException {
		String userID = "339";
		String password = "jimeet";
		String apiUrl = "http://10.158.1.60/WS_SMS/Service.asmx/SendSMS";

		String encodedSmsContent = URLEncoder.encode(smsContent, StandardCharsets.UTF_8.toString());

		//String fullUrl = String.format("%s?UserID=%s&Password=%s&MobileNo=%s&SMSText=%s", apiUrl, userID, password,mobileNumber, encodedSmsContent);
		String fullUrl = "http://10.91.2.55:8666/SimpleServletProject/api/testbitly";

		System.out.println("Full URL: " + fullUrl);

		HttpURLConnection connection = null;

		try {
			URL url = new URL(fullUrl);
			connection = (HttpURLConnection) url.openConnection();

			connection.setRequestMethod("GET");

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
				if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
					// Reading the response from the API
					return reader.lines().reduce(String::concat).orElse("");
				} else {
					// Handle non-success response codes
					System.out.println("API request failed with response code: " + connection.getResponseCode());
					return null;
				}
			}
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	public static boolean recordInsertionInSqlServerLogsTable(String urlRequestResponse, String transactionDate,
			String alertMode) {
		String jdbcUrl = "jdbc:sqlserver://10.91.102.20:1433;databaseName=CommServices";
		String username = "ABHI_CC";
		String password = "Birla@1234";

		try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
			if (connection != null) {
				System.out.println("Connected to the SQL Server database!");

				String insertQuery = "EXEC CommServices.dbo.InsertNotificationLog ?, ?, ?";

				try (CallableStatement callableStatement = connection.prepareCall(insertQuery)) {
					callableStatement.setString(1, urlRequestResponse);
					callableStatement.setString(2, transactionDate);
					callableStatement.setString(3, alertMode);

					boolean hasResults = callableStatement.execute();
					if (!hasResults) {
						System.out.println("Successfully inserted");
						return true;
					} else {
						System.out.println("Unsuccessful insertion");
					}
				}
			}
		} catch (SQLException e) {
			System.out.println("Connection failed: " + e.getMessage());
		}
		return false;
	}
	public static boolean executeInsertSendSMS_WhatsApp(
	        String requestBody,
	        String source,
	        String templateID,
	        String sentTime,
	        String responseTime,
	        int alertMode) {
		String jdbcUrl = "jdbc:sqlserver://10.91.102.20:1433;databaseName=CommServices";
		String username = "ABHI_CC";
		String password = "Birla@1234";
	    boolean success = false;

	    try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
	        // Stored procedure call
	        String storedProcedureCall = "{call CommServices.dbo.InsertSendSMS_WhatsApp(?, ?, ?, ?, ?, ?)}";
	        
	        try (PreparedStatement preparedStatement = connection.prepareCall(storedProcedureCall)) {
	            // Set parameters
	            preparedStatement.setString(1, requestBody);
	            preparedStatement.setString(2, source);
	            preparedStatement.setString(3, templateID);
	            preparedStatement.setString(4, sentTime);
	            preparedStatement.setString(5, responseTime);
	            preparedStatement.setInt(6, alertMode);

	            // Execute the stored procedure
	            int rowsAffected = preparedStatement.executeUpdate();

	            // Check if the execution was successful
	            success = (rowsAffected > 0);
	        }
	    } catch (SQLException e) {
	        e.printStackTrace(); // Handle the exception according to your application's needs
	    }

	    return success;
	}


	public static String convertToString(Date inputDate, String format) {
		DateFormat dateFormat = new SimpleDateFormat(format);
		return dateFormat.format(inputDate);
	}
	private static boolean isTenDigitNumber(String number) {
	    String digitsOnly = number.replaceAll("\\D", "");
	    return digitsOnly.length() == 10;
	}

}