package com.paymentnetwork;

import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_512;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URLEncodedUtils;

public class Gateway {
	private String merchantID;
	private String merchantSecret;
	private String merchantPwd;
	private String directUrl;
	private String hostedUrl;
	private String proxyUrl;

	public final int RC_SUCCESS = 0; // Transaction successful
	public final int RC_DO_NOT_HONOR = 5; // Transaction declined
	public final int RC_NO_REASON_TO_DECLINE = 85; // Verification successful

	public final int RC_3DS_AUTHENTICATION_REQUIRED = 0x1010A;

	// Non 3DS merchantId = 10001, with secret Circle4Take40Idea
	// 3DS merchantId = 100856, with secret Circle4Take40Idea

	public Gateway(String merchantID, String merchantSecret, String directUrl, String hostedUrl, String proxyUrl) {
		this.merchantID = merchantID == null ? "100856" : merchantID;
		this.merchantSecret = merchantSecret == null ? "Circle4Take40Idea" : merchantSecret;
		this.directUrl = directUrl == null ? "https://gateway.example.com/direct/" : directUrl;
		this.hostedUrl = hostedUrl == null ? "https://gateway.example.com/paymentform/" : hostedUrl;
		this.proxyUrl = proxyUrl;
	}

	/// <summary>
	/// Send request to Gateway using HTTP Direct API.
	///
	/// The method will send a request to the Gateway using the HTTP Direct API.
	///
	/// The request will use the following Gateway properties unless alternative
	/// values are provided in the request dictionary.
	/// hostedUrl - Gateway Hosted API Endpoint
	/// merchantID - Merchant Account Id
	/// merchantPwd - Merchant Account Password
	/// merchantSecret - Merchant Account Secret
	///
	/// The method will sign the request and also call verifySignature to
	/// check any response.
	///
	/// The method will throw an exception if it is unable to send the request
	/// or receive the response.
	///
	/// The method does not attempt to validate any request fields.
	///
	/// The prepareRequest method called within will throw an exception if there
	/// key fields are missing, the method does not attempt to validate any request
	/// fields.
	///
	/// </summary>
	/// <param name="request"> Request data </params>
	/// <param name="options"> Not currently used </params>
	public Map<String, String> directRequest(Map<String, String> request, Map<String, String> options)
			throws IOException, URISyntaxException {

		// requestSettings contains directUrl, hostedUrl and merchant secret.
		// this allows those values to be set by the prepareRequest method.
		Map<String, String> requestSettings = new HashMap<String, String>();

		this.prepareRequest(request, options, requestSettings);

		try (CloseableHttpClient client = HttpClients.createDefault()) {

			var httpPost = new HttpPost(this.directUrl);

			// HttpPost class requires an ArrayList<NameValuePair> rather than a Map<String,
			// String>
			ArrayList<NameValuePair> requestAsList = new ArrayList<NameValuePair>();

			for (Map.Entry<String, String> entry : request.entrySet()) {
				requestAsList.add(new BasicNameValuePair(entry.getKey(), (String) entry.getValue()));
			}

			if (requestSettings.containsKey("secret")) {
				requestAsList.add(new BasicNameValuePair("signature",
						sign(requestAsList, requestSettings.get("secret"))));
			}

			httpPost.setEntity(new UrlEncodedFormEntity(requestAsList));

			var response = client.execute(httpPost);

			var inputStream = response.getEntity().getContent();
			String gatewayResponse = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());

			List<NameValuePair> resultFieldsList = URLEncodedUtils.parse(gatewayResponse, Charset.forName("UTF-8"));

			var rtn = new HashMap<String, String>();

			resultFieldsList.forEach((f) -> {
				rtn.put(f.getName(), f.getValue());
			});

			VerifyResponse(rtn, merchantSecret);

			return rtn;
		}
	}

	/// <summary>
	/// Create a form that can then be used to send the request to the gateway
	/// using the HTTP Hosted API.
	///
	/// The request will use the following Gateway properties unless alternative
	/// values are provided in the request;
	/// hostedUrl - Gateway Hosted API Endpoint
	/// merchantID - Merchant Account Id
	/// merchantPwd - Merchant Account Password
	/// merchantSecret' - Merchant Account Secret

	/// The method accepts the following options in the options dictionary
	/// formAttrs - HTML form attributes string
	/// submitAttrs - HTML submit button attributes string
	/// submitImage - URL of image to use as the Submit button
	/// submitHtml - HTML to show on the Submit button
	/// submitText - Text to show on the Submit button

	/// 'submitImage', 'submitHtml' and 'submitText' are mutually exclusive
	/// options and will be checked for in that order. If none are provided
	/// the submitText='Pay Now' is assumed.

	/// The prepareRequest method called within will throw an exception if there
	/// key fields are missing, the method does not attempt to validate any request
	/// fields.

	/// </summary>
	/// <param name="request"> Dictionary<string, string> Request data </params>
	/// <param name="options"> Not currently used </params>
	public String HostedRequest(Map<String, String> request, Map<String, String> options) {

		Map<String, String> requestSettings = new HashMap<String, String>();

		this.prepareRequest(request, options, requestSettings);

		if (!request.containsKey("redirectURL")) {
			// RedirectURL is used to send the user back to your site following
			// a transaction. It must be set according to your environment.
			throw new IllegalArgumentException("redirectURL is required and must be set according to your environment");
		}

		if (requestSettings.containsKey("merchantSecret")) {
			request.put("signature", requestSettings.get("signature"));
		}

		StringBuilder ret = new StringBuilder();

		String formAttrs = options.containsKey("formAttrs") ? options.get("formAttrs") : "";
		String action = StringEscapeUtils.escapeHtml4(this.hostedUrl);

		ret.append(String.format("<form method=\"post\" %s action=\"%s\" /> \n", formAttrs, action));

		for (Map.Entry<String, String> entry : request.entrySet()) {
			ret.append(fieldToHtml(entry.getKey(), (String) entry.getValue()));
		}

		String submitAttrs = options.getOrDefault("submitAttrs", "");

		ret.append(submitAttrs);

		String submitElement;
		if (options.containsKey("submitImage")) {
			submitElement = String.format("<input %s  type=\"image\" src=\"", submitAttrs)
					+ StringEscapeUtils.escapeHtml4(options.get("submitImage")) + "\">\n";

		} else if (options.containsKey("submitHtml")) {
			submitElement = String.format("<button type=\"submit\" %s >", submitAttrs)
					+ options.get("submitHtml") + "</button>\n";
		} else {
			submitElement = String.format("<input %s type=\"submit\" value=\"", submitAttrs)
					+ StringEscapeUtils.escapeHtml4(options.getOrDefault("submitText", "Pay Now"))
					+ "\">\n";
		}

		ret.append(submitElement + "</form>\n");

		return ret.toString();
	}

	/// <summary>
	/// Prepare a request for sending to the Gateway.
	///
	/// The method will extract the following configuration properties from the
	/// request if they are present;
	/// merchantSecret
	/// directUrl
	/// hostedUrl
	///
	/// The method will throw an exception is the request doesn't contain an
	/// 'action' element or a 'merchantID' element (and none could be inserted).
	/// </summary>
	/// <param name="request"> Dictionary<string, string> Request data </params>
	/// <param name="options"> Not currently used </params>
	/// <param name="secret"> The Merchant Secret </params>
	/// <param name="directUrl"> The URL for direct integrations </params>
	/// <param name="hostedUrl"> The URL for hosted integrations </params>
	private void prepareRequest(Map<String, String> request, Map<String, String> options, Map<String, String> requestSettings) {

		if (request == null) {
			throw new NullPointerException("Request must be provided.");
		}
		if (request.size() == 0) {
			throw new IllegalArgumentException("Request must be provided.");
		}

		if (!request.containsKey("action")) {
			throw new IllegalArgumentException("Request must contain an 'action'");
		}

		// Insert 'merchantID' if doesn't exist and default is available
		if (!request.containsKey("merchantID")) {
			if (this.merchantID == null) {
				// MerchantID must be set
				throw new IllegalArgumentException("MerchantID not set in either request or the class");
			}
			request.put("merchantID", this.merchantID);
		}

		// Insert 'merchantPwd' if doesn't exist and default is available
		if (!request.containsKey("merchantPwd") && merchantPwd != null) {
			request.put("merchantPwd", merchantPwd);
		}

		if (request.containsKey("merchantSecret")) {
			requestSettings.put("secret", (String) request.get("merchantSecret"));
			request.remove("merchantSecret");
		} else if (this.merchantSecret != null) {
			requestSettings.put("secret", this.merchantSecret);
		}

		requestSettings.put("hostedUrl", (String) request.getOrDefault("hostedUrl", this.hostedUrl));
		request.remove("hostedUrl");

		requestSettings.put("directUrl", (String) request.getOrDefault("directUrl", this.directUrl));
		request.remove("directUrl");

		// Remove items we don't want to send in the request
		// (they may be there if a previous response is sent)
		String[] removeKeys = new String[] { 
			"responseCode",
			"responseMessage",
			"responseStatus",
			"state",
			"signature",
			"merchantAlias",
			"merchantID2"
		};

		for (String key : removeKeys) {
			request.remove(key); // Doesn't error if key not present.
		}
	}

	protected String sign(List<NameValuePair> fields, String secret) {
		return sign(fields, secret, null);
	}

	protected String sign(List<NameValuePair> fields, String secret, List<String> partial) {
		TreeMap<String, String> fieldsSorted = new TreeMap<String, String>(new FieldCompare());
		String partialStr = "";

		if (partial != null && partial.size() != 0) {
			fields.forEach((f) -> {
				if (partial.contains((f.getName()))) {
					fieldsSorted.put(f.getName(), f.getValue());
				}
			});
			partialStr = "|" + String.join(",", partial);
		}

		fields.forEach((f) -> {
			fieldsSorted.put(f.getName(), f.getValue());
		});

		var fieldsFinal = new ArrayList<NameValuePair>();
		fieldsSorted.forEach((k, v) -> {
			fieldsFinal.add(new BasicNameValuePair(k, v));
		});

		var body = URLEncodedUtils.format(fieldsFinal, StandardCharsets.UTF_8);
		body = body.replaceAll("\\*", "%2A");

		// System.out.println(body);

		String signature = new DigestUtils(SHA_512).digestAsHex(body + merchantSecret);

		return signature + partialStr;
	}

	public boolean VerifyResponse(Map<String, String> response, String secret) {
		if (response == null) {
			throw new NullPointerException("Invalid response from Gateway");
		}

		if (response.size() == 0) {
			throw new IllegalArgumentException("Invalid response from Gateway");
		}

		String signature = response.get("signature");
		response.remove("signature");

		List<String> fields = null;

		if (!secret.isEmpty() && !signature.isEmpty() && signature.contains("|")) {
			String[] split = signature.split("|");
			signature = split[0];
			fields = Arrays.asList(split[1].split(","));
		}

		List<NameValuePair> fieldsAsList = new ArrayList<NameValuePair>();

		response.forEach((k, v) -> {
			fieldsAsList.add(new BasicNameValuePair(k, v));
		});

		// System.out.println(fieldsAsList);

		// We display three suitable different exception messages to help show
		// secret mismatches between ourselves and the Gateway without giving
		// too much away if the messages are displayed to the Cardholder.
		if (secret.isEmpty() && !signature.isEmpty()) {
			// Signature present when not expected (Gateway has a secret but we don't)
			throw new RuntimeException("Incorrectly signed response from Payment Gateway (1)");
		} else if (!secret.isEmpty() && signature.isEmpty()) {
			// Signature missing when one expected (We have a secret but the Gateway doesn't)
			throw new RuntimeException("Incorrectly signed response from Payment Gateway (2)");
		}

		return true;
	}

	/// <summary>
	/// Creates a string of hidden HTML form fields. All the keys are prefixed
	/// with the name. This is a overload that accepts (string, object), then
	/// calls the correct method for the type of object.
	/// </summary>
	public static String fieldToHtml(String name, Object value) {
		if (value instanceof String) {
			return fieldToHtml(name, (String) value);
		} else if (value instanceof Map) {
			return fieldToHtml(name, (Map) value);
		} else {
			throw new UnsupportedOperationException("Invalid type passed to FieldToHtml");
		}
	}

	/// <summary>
	/// Creates a string of hidden HTML form fields. All the keys are prefixed
	/// with the name.
	/// </summary>
	/// <returns>
	/// String with the format `<input type="hidden" name="{name}[{dictionaryKey}]"
	/// value="{dictionaryValue}" />`
	/// </returns>
	public static String fieldToHtml(String name, Map<String, String> value) {
		StringBuilder sb = new StringBuilder();

		for (Map.Entry<String, String> entry : value.entrySet()) {

			sb.append(fieldToHtml(name + "[" + entry.getKey() + "]", entry.getValue()));
		}

		return sb.toString();
	}

	/// <summary>
	/// Name and value as a hidden form field.
	/// </summary>
	/// <returns>
	/// String with the format `<input type="hidden" name="{name}" value="{value}"/>`
	/// </returns>
	public static String fieldToHtml(String name, String value) {
		value = StringEscapeUtils.escapeHtml4(value);
		return String.format("<input type=\"hidden\" name=\"%s\" value=\"%s\" />\n", name, value);
	}
}
