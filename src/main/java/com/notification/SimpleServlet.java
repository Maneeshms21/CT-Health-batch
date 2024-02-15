package com.notification;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
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

@WebServlet(name = "SimpleServlet", urlPatterns = "/api/bitly")
public class SimpleServlet extends HttpServlet {
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
				response.getWriter().write("Error reading request body: " + e.getMessage());
				return;
			}
			String requestBodyString = requestBody.toString();
			System.out.println("requestBodyString " + requestBodyString);
			Gson gson = new Gson();
			BitlyRequest message = gson.fromJson(requestBodyString, BitlyRequest.class);
			System.out.println("message " + message.getObjRequest().getLongURL() + " " + message.getSource());
			// BitlyResponse britlyResponse = new BitlyResponse();
			String britlyResponse = null;
			String modifiedJsonRequest = modifyAndSerializeRequest(message);
			Date currentDate = new Date();
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(currentDate);
			calendar.add(Calendar.DATE, message.getObjRequest().getExpiryDay());
			Timestamp expiryTimeStamp = new Timestamp(calendar.getTimeInMillis());
			String transactionDate = convertToString(currentDate, "yyyy-MM-dd HH:mm:ss");
			// String reqUrl = "https://webapi.abcap.co.in/ABCCap.svc/InsertShorturl";
			String reqUrl = "http://10.91.2.55:8666/SimpleServletProject/api/testbitly";
			if (message.getSource() == null) {
				boolean insertInLog = recordInsertionInSqlServerLogsTable(
						reqUrl + " Request: " + requestBodyString + " Response: " + "Source not found in Request ",
						transactionDate, message.getAlertMode());
				System.out.println("Source not found in Request: " + message);
				response.setContentType("application/json");
				PrintWriter out = response.getWriter();
				out.print("Source not found in Request ");
				out.flush();
				return;
			}
			boolean bitlySourceCheck = fetchBitlySourceData(message.getSource());
			if (bitlySourceCheck == true) {
				if (message.getObjRequest().getExpiryDay() == 0) {

					boolean insertInLog = recordInsertionInSqlServerLogsTable(reqUrl + " Request: " + requestBodyString
							+ " Response: " + "Expiry day missing in the request: ", transactionDate,
							message.getAlertMode());
					System.out.println("Expiry day missing in the request: " + message);
					response.setContentType("application/json");
					PrintWriter out = response.getWriter();
					out.print("Expiry day missing in the request");
					out.flush();
					return;
				}
				if (message.getObjRequest().getLongURL() == null) {
					boolean insertInLog = recordInsertionInSqlServerLogsTable(reqUrl + " Request: " + requestBodyString
							+ " Response: " + "Long URL missing in the request: ", transactionDate,
							message.getAlertMode());
					System.out.println("Long URL missing in the request: " + message);
					response.setContentType("application/json");
					PrintWriter out = response.getWriter();
					out.print("Long URL missing in the request");
					out.flush();
					return;
				}

				int availabilityCheck = recordAvailabilityCheckInSqlServerDB(message.getObjRequest().getLongURL(),
						message.getObjRequest().getExpiryDay(), modifiedJsonRequest, reqUrl);
				System.out.println("availabilityCheck " + availabilityCheck);

				if (availabilityCheck == 0) {

					try {

						System.out.println("modifiedJsonRequest " + modifiedJsonRequest);
						Map<String, String> headers = new HashMap<String, String>();
						headers.put("Content-Type", "application/json");
						britlyResponse = callBitlyService(reqUrl, modifiedJsonRequest, headers);

						if (britlyResponse != null) {
							boolean insertInBit = recordInsertionInSqlServerBitlyTable(
									message.getObjRequest().getLongURL(), message.getObjRequest().getExpiryDay(),
									britlyResponse, message.getSource());
							/*
							 * boolean insertInBit =
							 * recordInsertionInSqlServerBitlyTable(message.getObjRequest().getLongURL(),
							 * message.getObjRequest().getExpiryDay(), britlyResponse.getURLReturned(),
							 * message.getSource());
							 */

							boolean insertInLog = recordInsertionInSqlServerLogsTable(
									reqUrl + " Request: " + modifiedJsonRequest + " Response: " + britlyResponse,
									transactionDate, message.getAlertMode());

							if (insertInBit && insertInLog) {
								System.out.println("Request Processed Successfully: " + britlyResponse);
								response.setContentType("application/json");
								PrintWriter out = response.getWriter();
								out.print("Request Processed Successfully: " + britlyResponse);
								out.flush();
								return;
							}
						} else {
							System.out.println("Response is null: " + britlyResponse);
						}
					} catch (Exception e) {
						System.out.println("Exception occurred while processing request " + e);
						System.out.println("Exception occurred while processing request: " + e.getMessage());
					}
				} else if (availabilityCheck == -2) {
					String shortUrl = callFetchSqlServerBitlyDetailsSP(message.getObjRequest().getLongURL(),
							message.getObjRequest().getExpiryDay());
					boolean insertInLog = recordInsertionInSqlServerLogsTable(
							reqUrl + " Request: " + modifiedJsonRequest + " Response: " + shortUrl, transactionDate,
							message.getAlertMode());
					try {
						System.out.println(" Already existed with the current bitly " + shortUrl);
						response.setContentType("application/json");
						PrintWriter out = response.getWriter();
						out.print(" Already existed with the current bitly " + shortUrl);
						out.flush();
						return;
					} catch (Exception e) {
						System.out.println("Exception occurred while extracting data from table " + e);
						e.printStackTrace();
					}

				} else if (availabilityCheck == 1) {
					try {
						Timestamp timestamp = Timestamp.valueOf(transactionDate);
						System.out.println("modifiedJsonRequest " + modifiedJsonRequest);
						Map<String, String> headers = new HashMap<String, String>();
						headers.put("Content-Type", "application/json");
						britlyResponse = callBitlyService(reqUrl, modifiedJsonRequest, headers);

						if (britlyResponse != null) {
							int result = callUpdateSqlServerBitlySP(message.getObjRequest().getLongURL(),
									britlyResponse, message.getSource(), expiryTimeStamp, timestamp,
									message.getObjRequest().getExpiryDay());

							boolean insertInLog = recordInsertionInSqlServerLogsTable(
									reqUrl + " Request: " + modifiedJsonRequest + " Response: " + britlyResponse,
									transactionDate, message.getAlertMode());

							if (result == 1 && insertInLog) {
								System.out.println("Request Processed Successfully: " + britlyResponse);
								response.setContentType("application/json");
								PrintWriter out = response.getWriter();
								out.print("Request Processed Successfully: " + britlyResponse);
								out.flush();
								return;
							}
						}
					} catch (Exception e) {
						System.out.println("Exception occurred: " + e.getMessage());
					}

				}
			}
			String shortUrl = callFetchSqlServerBitlyDetailsSP(message.getObjRequest().getLongURL(),
					message.getObjRequest().getExpiryDay());
			boolean insertInLog = recordInsertionInSqlServerLogsTable(
					reqUrl + " Request: " + requestBodyString + " Response: " + shortUrl, transactionDate,
					message.getAlertMode());
			response.setContentType("application/json");
			PrintWriter out = response.getWriter();
			out.print("Source is Invalid ");
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

	public static void removeKeyIgnoreCase(JSONObject jsonObject, String key) {
		for (String currentKey : jsonObject.keySet()) {
			if (currentKey.equalsIgnoreCase(key)) {
				jsonObject.remove(currentKey);
				break; // Assuming you want to remove only the first matching key
			}
		}
	}

	private static String modifyAndSerializeRequest(BitlyRequest message) {
		System.out.println("message " + message);
		try {
			JSONObject jsonObject = new JSONObject(new Gson().toJson(message));
			removeKeyIgnoreCase(jsonObject, "source");
			removeKeyIgnoreCase(jsonObject, "alertMode");

			if (jsonObject.has("objRequest")) {
				JSONObject objRequest = jsonObject.getJSONObject("objRequest");
				objRequest.put("UserId", "ClickPassProd");
				objRequest.put("Password", "B8iaP9sbZLX09KKbijoqkg==");
			}

			return jsonObject.toString();
		} catch (Exception e) {
			System.out.println("Exception occurred: " + e.getMessage());
			return null;
		}
	}
	/*
	 * public static boolean fetchBitlySourceData(String sourceFilter) { String
	 * jdbcUrl = "jdbc:sqlserver://10.91.102.20:1433;databaseName=CommServices";
	 * String username = "ABHI_CC"; String password = "Birla@1234"; try (Connection
	 * connection = DriverManager.getConnection(jdbcUrl, username, password)) { if
	 * (connection != null) {
	 * System.out.println("Connected to the SQL Server database!");
	 * 
	 * // Call the stored procedure try (CallableStatement callableStatement =
	 * connection.prepareCall("{CALL CommServices.dbo.GetBitlySourceData(?)}")) {
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

	public static boolean fetchBitlySourceData(String sourceFilter) {
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

	public static String callFetchSqlServerBitlyDetailsSP(String longURL, int expiryDay) {
		String jdbcUrl = "jdbc:sqlserver://10.91.102.20:1433;databaseName=CommServices";
		String username = "ABHI_CC";
		String password = "Birla@1234";
		String resultString = null;

		try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
				CallableStatement statement = connection
						.prepareCall("{CALL CommServices.dbo.GetBitlyDataByLongUrl(?)}")) {

			statement.setString(1, longURL);

			// Execute the stored procedure
			try (ResultSet resultSet = statement.executeQuery()) {
				// Process the ResultSet if needed
				while (resultSet.next()) {
					resultString = resultSet.getString("Short_url");
				}
			}

		} catch (SQLException e) {
			System.out.println("Connection failed or error executing stored procedure: " + e.getMessage());
		}

		return resultString;
	}

	/*
	 * public static BitlyResponse callBitlyService(String reqUrl, String reqBody,
	 * Map<String, String> headers) throws Exception { CloseableHttpClient
	 * httpClient = HttpClients.createDefault(); HttpPost request = new
	 * HttpPost(reqUrl); System.out.println("reqUrl " + reqUrl + " reqBody " +
	 * reqBody);
	 * 
	 * for (Entry<String, String> entry : headers.entrySet()) {
	 * request.addHeader(entry.getKey(), entry.getValue()); }
	 * 
	 * StringEntity stringEntity = new StringEntity(reqBody);
	 * request.setEntity(stringEntity);
	 * 
	 * try (CloseableHttpResponse response = httpClient.execute(request)) { int
	 * statusCode = response.getStatusLine().getStatusCode();
	 * System.out.println("Response Status Code: " + statusCode);
	 * 
	 * if (statusCode >= 200 && statusCode < 300) { HttpEntity entity =
	 * response.getEntity(); if (entity != null) { String result =
	 * EntityUtils.toString(entity); System.out.println("Response Body: " + result);
	 * return new Gson().fromJson(result, BitlyResponse.class); } } else { // Handle
	 * non-successful status codes here // Log the response and status code for
	 * analysis
	 * System.out.println("Bitly service responded with non-successful status: " +
	 * statusCode); } } catch (IOException e) { // Handle specific exceptions
	 * related to HTTP client calls
	 * System.out.println("Exception occurred while calling Bitly service: " +
	 * e.getMessage()); } finally { try { httpClient.close(); // Close the
	 * HttpClient in the finally block } catch (IOException e) { // Handle exception
	 * related to closing HttpClient
	 * System.out.println("Exception occurred while closing HttpClient: " +
	 * e.getMessage()); } }
	 * 
	 * return null; // Return null if there's no valid response or an error occurred
	 * }
	 */
	public static String callBitlyService(String reqUrl, String reqBody, Map<String, String> headers) throws Exception {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpPost request = new HttpPost(reqUrl);
		System.out.println("reqUrl " + reqUrl + " reqBody " + reqBody);

		for (Entry<String, String> entry : headers.entrySet()) {
			request.addHeader(entry.getKey(), entry.getValue());
		}

		StringEntity stringEntity = new StringEntity(reqBody);
		request.setEntity(stringEntity);

		try (CloseableHttpResponse response = httpClient.execute(request)) {
			int statusCode = response.getStatusLine().getStatusCode();
			System.out.println("Response Status Code: " + statusCode);

			if (statusCode >= 200 && statusCode < 300) {
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					String result = EntityUtils.toString(entity);
					System.out.println("Response Body: " + result);
					return result;
				}
			} else {
				// Handle non-successful status codes here
				// Log the response and status code for analysis
				System.out.println("Bitly service responded with non-successful status: " + statusCode);
			}
		} catch (IOException e) {
			// Handle specific exceptions related to HTTP client calls
			System.out.println("Exception occurred while calling Bitly service: " + e.getMessage());
		} finally {
			try {
				httpClient.close(); // Close the HttpClient in the finally block
			} catch (IOException e) {
				// Handle exception related to closing HttpClient
				System.out.println("Exception occurred while closing HttpClient: " + e.getMessage());
			}
		}

		return null; // Return null if there's no valid response or an error occurred
	}

	public static int recordAvailabilityCheckInSqlServerDB(String longURL, int expiryDay, String modifiedJsonRequest,
			String reqUrl) {
		String jdbcUrl = "jdbc:sqlserver://10.91.102.20:1433;databaseName=CommServices";
		String username = "ABHI_CC";
		String password = "Birla@1234";

		try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
			if (connection != null) {
				System.out.println("Connected to the SQL Server database!");

				// Call the stored procedure
				try (CallableStatement statement = connection
						.prepareCall("{CALL CommServices.dbo.GetBitlyDataByLongUrl(?)}")) {
					statement.setString(1, longURL);

					// Execute the stored procedure
					ResultSet resultSet = statement.executeQuery();

					// Check if there are results
					if (resultSet.next()) {
						// Data fetched successfully
						Date currentDate = new Date();
						Calendar calendar = Calendar.getInstance();
						calendar.setTime(currentDate);
						calendar.add(Calendar.DATE, expiryDay);
						calendar.set(Calendar.HOUR_OF_DAY, 0);
						calendar.set(Calendar.MINUTE, 0);
						calendar.set(Calendar.SECOND, 0);
						calendar.set(Calendar.MILLISECOND, 0);
						// Timestamp expiryDate = resultSet.getTimestamp("ExpiryTime");
						Date expiryDate = resultSet.getDate("ExpiryTime");
						int comparisonResult = compareDates(expiryDate, currentDate);

						if (comparisonResult == 0) {
							System.out.println("Timestamps are equal.");
							return -2;
						} else if (comparisonResult < 0) {
							System.out.println("Timestamp1 is less or before Timestamp2.");
							return 1;
						} else if (comparisonResult > 0) {
							System.out.println("Timestamp1 is after Timestamp2.");
							return -2;
						}
					} else {
						// No data found for the specified longURL
						System.out.println("No data found for Long_url: " + longURL);
						return 0;
					}
				}
			}
		} catch (SQLException e) {
			System.out.println("Connection failed or error executing stored procedure: " + e.getMessage());
		}
		return 0;
	}

	private static int compareDates(Date date, Date expirytime) {
		if (date.before(expirytime)) {
			return -1; // ''expirytime'' is before date
		} else if (date.after(expirytime)) {
			return 1; // 'expirytime' is after 'date'
		} else {
			return 0; // 'expirytime' is equal to 'date'
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

	public static boolean recordInsertionInSqlServerBitlyTable(String longURL, int expiryDay, String shortURL,
			String source) {
		String jdbcUrl = "jdbc:sqlserver://10.91.102.20:1433;databaseName=CommServices";
		String username = "ABHI_CC";
		String password = "Birla@1234";

		try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
			if (connection != null) {
				System.out.println("Connected to the SQL Server database!");

				Timestamp currentTimeStamp = new Timestamp(System.currentTimeMillis());
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(currentTimeStamp);
				calendar.add(Calendar.DATE, expiryDay);
				Timestamp expiryTimeStamp = new Timestamp(calendar.getTimeInMillis());

				String storedProcedureCall = "{call CommServices.dbo.InsertIntoCreateBitly(?, ?, ?, ?, ?, ?)}";

				try (CallableStatement callableStatement = connection.prepareCall(storedProcedureCall)) {
					callableStatement.setString(1, longURL);
					callableStatement.setString(2, shortURL);
					callableStatement.setString(3, source);
					callableStatement.setTimestamp(4, currentTimeStamp);
					callableStatement.setInt(5, expiryDay);
					callableStatement.setTimestamp(6, expiryTimeStamp);

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

	public static int callUpdateSqlServerBitlySP(String longURL, String shortURL, String source, Timestamp expiryTime,
			Timestamp sentTime, int expiryDay) {
		String jdbcUrl = "jdbc:sqlserver://10.91.102.20:1433;databaseName=CommServices";
		String username = "ABHI_CC";
		String password = "Birla@1234";

		try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
			if (connection != null) {
				System.out.println("Connected to the SQL Server database!");
				String callSP = "{CALL CommServices.dbo.UpdateCreateBitly(?, ?, ?, ?, ?, ?)}";

				try (CallableStatement statement = connection.prepareCall(callSP)) {
					statement.setString(1, longURL);
					statement.setString(2, shortURL);
					statement.setString(3, source);
					statement.setTimestamp(4, sentTime);
					statement.setInt(5, expiryDay);
					statement.setTimestamp(6, expiryTime);

					// Execute the stored procedure
					statement.executeUpdate();
					System.out.println("Stored procedure executed successfully for updating records.");
					return 1;
				}
			}
		} catch (SQLException e) {
			System.out.println("Connection failed or error executing stored procedure: " + e.getMessage());
		}
		return 0;
	}

	public static String convertToString(Date inputDate, String format) {
		DateFormat dateFormat = new SimpleDateFormat(format);
		return dateFormat.format(inputDate);
	}

	/*
	 * public static int recordAvailabilityCheckInSqlServerDB(String longURL, int
	 * expiryDay, String modifiedJsonRequest, String reqUrl) { String jdbcUrl =
	 * "jdbc:sqlserver://10.91.102.20:1433;databaseName=CommServices"; String
	 * username = "ABHI_CC"; String password = "Birla@1234";
	 * 
	 * try (Connection connection = DriverManager.getConnection(jdbcUrl, username,
	 * password)) { if (connection != null) {
	 * System.out.println("Connected to the SQL Server database!");
	 * 
	 * String callSP = "{CALL CommServices.dbo.FetchBitlyData(?, ?)}";
	 * 
	 * try (CallableStatement statement = connection.prepareCall(callSP)) {
	 * statement.setString(1, longURL); statement.setInt(2, expiryDay);
	 * 
	 * try (ResultSet resultSet = statement.executeQuery()) { if (resultSet.next())
	 * { //System.out.println("Inside resultSet loop");
	 * 
	 * Date currentDate = new Date(); Calendar calendar = Calendar.getInstance();
	 * calendar.setTime(currentDate); calendar.add(Calendar.DATE, expiryDay); //
	 * Timestamp expiryDate = resultSet.getTimestamp("ExpiryTime"); Date expiryDate
	 * = resultSet.getDate("ExpiryTime"); int comparisonResult =
	 * compareDates(calendar.getTime(), expiryDate);
	 * 
	 * if (comparisonResult == 0) { System.out.println("Timestamps are equal.");
	 * return -1; } else if (comparisonResult < 0) {
	 * System.out.println("Timestamp1 is before Timestamp2."); return 1; } else
	 * if(comparisonResult > 0) {
	 * System.out.println("Timestamp1 is after Timestamp2.");
	 * 
	 * 
	 * return -2; } } } } } } catch (SQLException e) {
	 * System.out.println("Connection failed or error executing stored procedure: "
	 * + e.getMessage()); } return 0; } public static ResultSet
	 * callFetchSqlServerBitlyDetailsSP(String longURL, int expiryDay) { String
	 * jdbcUrl = "jdbc:sqlserver://10.91.102.20:1433;databaseName=CommServices";
	 * String username = "ABHI_CC"; String password = "Birla@1234"; ResultSet
	 * resultSet = null; Connection connection = null; CallableStatement statement =
	 * null;
	 * 
	 * try { connection = DriverManager.getConnection(jdbcUrl, username, password);
	 * if (connection != null) {
	 * System.out.println("Connected to the SQL Server database!"); String callSP =
	 * "{CALL CommServices.dbo.FetchBitlyData(?, ?)}";
	 * 
	 * statement = connection.prepareCall(callSP); statement.setString(1, longURL);
	 * statement.setInt(2, expiryDay);
	 * 
	 * // Execute the stored procedure resultSet = statement.executeQuery(); } }
	 * catch (SQLException e) {
	 * System.out.println("Connection failed or error executing stored procedure: "
	 * + e.getMessage()); } finally { try { if (resultSet != null) {
	 * resultSet.close(); } if (statement != null) { statement.close(); } if
	 * (connection != null) { connection.close(); } } catch (SQLException e) {
	 * System.out.println("Error closing resources: " + e.getMessage()); } } return
	 * resultSet; }
	 */
}

/*
 * public static void main(String[] args) { String jdbcUrl =
 * "jdbc:mysql://localhost:3306/CommServices"; String username = "root"; String
 * password = "12345"; ResultSet resultSet = null; Connection connection = null;
 * CallableStatement statement = null;
 * 
 * try { connection = DriverManager.getConnection(jdbcUrl, username, password);
 * if (connection != null) {
 * System.out.println("Connected to the MySQL database!"); String callSP =
 * "{CALL FetchBitlyDetails(?, ?, ?)}";
 * 
 * statement = connection.prepareCall(callSP); statement.setString(1,
 * "Hello I am maneesh"); statement.setString(2, "ODS"); statement.setInt(3, 3);
 * 
 * // Execute the stored procedure resultSet = statement.executeQuery(); } }
 * catch (SQLException e) {
 * System.out.println("Connection failed or error executing stored procedure: "
 * + e.getMessage()); } finally { try { if (resultSet != null) {
 * resultSet.close(); } if (statement != null) { statement.close(); } if
 * (connection != null) { connection.close(); } } catch (SQLException e) {
 * System.out.println("Error closing resources: " + e.getMessage()); } }
 * System.out.println("All fetching done and resultset"+resultSet); }
 */
