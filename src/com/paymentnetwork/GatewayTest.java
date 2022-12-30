package com.paymentnetwork;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

public class GatewayTest extends TestCase {
	private Gateway g;

	@Before
	protected void setUp() {
		g = new Gateway("100856", "Circle4Take40Idea", null, null, null);
	}

	private boolean paramSignatureStartsWith(List<NameValuePair> params, String prefix) {
		var result = g.sign(params, "Circle4Take40Idea").startsWith(prefix);
		return result;
	}

	@Test
	public void testSign() {
		String result = "";

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("a", "one"));
		params.add(new BasicNameValuePair("b", "two"));

		result = g.sign(params, "Circle4Take40Idea");
		assertTrue(result.equals(
				"1eb24e703e2e1b01a4e7f9a64051e9df3151dff950bcc9e2be0bdde34e5207ec9e5e7f9a8021a781f7cc3160b7625fc6452976d574425ba36b2e71e880f8afbd"));

		params.clear();
		params.add(new BasicNameValuePair("a", "one"));
		params.add(new BasicNameValuePair("b", "New lines! %0D %0D%0A"));

		result = g.sign(params, "Circle4Take40Idea");
		assertTrue(result.equals(
				"16fe6952cfdbf4ef0fe23b1d795c31800757c6476ff94e5d4cdc8817c3bed2a896ec30b39d65fc20aa7b95b226235c2bc8c73f0fda27958863d928ee8be1a27b"));

		params.clear();
		params.add(new BasicNameValuePair("a", "one"));
		params.add(new BasicNameValuePair("b", "strange \"'?& symbols "));

		result = g.sign(params, "Circle4Take40Idea");
		assertTrue(result.equals(
				"561c514800bedb7217accce8f7e6d49d1fb4dc48c19fd51ab296f4ebcc81519da6e1da6072dd7d5c762073317cc31e6282164818fef8978f9af5984dd350659f"));

		params.clear();
		params.add(new BasicNameValuePair("a", "one"));
		params.add(new BasicNameValuePair("b", "a £ sign"));

		result = g.sign(params, "Circle4Take40Idea");
		assertTrue(result.equals(
				"4b4a7636384ff9eea61952ed784907dec816f049092ac029e2a75cf78b50b78a96f5f7388d7b6c38ac686b66769463dbc954ce4aef33f1a1c66b2329f03b37cd"));

		params.clear();
		params.add(new BasicNameValuePair("a", "one"));
		params.add(new BasicNameValuePair("b", "aa ** stars"));

		result = g.sign(params, "Circle4Take40Idea");
		assertTrue(result.equals(
				"dac8f232e53d16161fdf95c06ebb26b4fe15792475ff9f81bed81694607ae03d273044ef5004717586770e7675ef7c499bbff286cb76275bf4eca9ec4225fb40"));

		params.clear();
		params.add(new BasicNameValuePair("a", "one"));
		params.add(new BasicNameValuePair("b", "newline \n characater"));

		result = g.sign(params, "Circle4Take40Idea");
		assertTrue(result.equals(
				"0fc99e4d47b13fd2f69b773aedcb84ea9e0da5e9dba20786ae23cb6c03f42e48b4ce55570b506f68930c50ccb25f6488e82c190e6938cbace87f86c0df2fc66b"));

		params.clear();
		params.add(new BasicNameValuePair("a[aa]", "12"));
		params.add(new BasicNameValuePair("a[bb]", "13"));
		params.add(new BasicNameValuePair("a1", "0"));
		params.add(new BasicNameValuePair("aa", "1"));
		params.add(new BasicNameValuePair("aZ", "2"));

		result = g.sign(params, "Circle4Take40Idea");
		assertTrue(result.equals(
				"bda26c3f3a75d196e18eddfb7150ee055118679016048337af2716bdafae3815e851a8b10562d5f91e6c4631a07a931dfc2ebe9e7e793b8c5edd62f72307861b"));
	}

	// Test that we can actually carry out a direct request.
	@Test
	public void testDirectRequest() {
		HashMap<String, String> params = GatewayUtils.getInitialForm();
		params.putAll(GatewayUtils.getDebuggingBrowserData());

		var gTest = new Gateway("https://gateway.example.com/direct/", "Circle4Take40Idea", null, null, null);

		try {
			// Ensure this completes without exception is a test in itself.
			// If the server response doesn't pass a signature verification,
			// it will throw.
			var options = new HashMap<String, String>();
			var gatewayResponse = gTest.directRequest(params, options);

			// We're sending a valid transaction request which expects a
			// '3DS Authentication Required' response, which is 65802
			System.out.println(gatewayResponse);
			assertEquals("65802", gatewayResponse.get("responseCode"));
			assertEquals("3DS AUTHENTICATION REQUIRED", gatewayResponse.get("responseMessage"));
			assertEquals("2.1.0", gatewayResponse.get("threeDSDetails[version]"));
			assertEquals("Y", gatewayResponse.get("threeDSEnrolled"));
		} catch (IOException e) {
			Assert.fail();
		} catch (URISyntaxException e) {
			Assert.fail();
		} catch (RuntimeException e) {
			Assert.fail();
		}
	}
}

class GatewayUtils {

	protected static HashMap<String, String> getInitialForm() {
		var uniqid = RandomStringUtils.random(15, true, true);

		HashMap<String, String> params = new HashMap<String, String>();

		params.put("merchantID", "100856");

		params.put("action", "SALE");
		params.put("type", "1");
		params.put("transactionUnique", uniqid);
		params.put("countryCode", "826");
		params.put("currencyCode", "826");
		params.put("amount", "1001");
		params.put("cardNumber", "4012001037141112");
		params.put("cardExpiryMonth", "12");
		params.put("cardExpiryYear", "21");
		params.put("cardCVV", "083");
		params.put("customerName", "Test Customer");
		params.put("customerEmail", "test@testcustomer.com");
		params.put("customerAddress", "16 Test Street");
		params.put("customerPostCode", "TE15 5ST");
		params.put("orderRef", "Test purchase");

		// The following fields are mandatory for 3DS v2
		params.put("remoteAddress", "10.10.10.10");
		params.put("merchantCategoryCode", "5411");
		params.put("threeDSVersion", "2");
		params.put("threeDSRedirectURL", "https://example.net/returnUrl?acs=1"); // PLACEHOLDER

		return params;
	}

	protected static HashMap<String, String> getDebuggingBrowserData() {
		HashMap<String, String> params = new HashMap<String, String>();

		params.put("merchantID", "100856");
		params.put("deviceChannel", "browser");
		params.put("deviceIdentity",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:81.0) Gecko/20100101 Firefox/81.0");
		params.put("deviceTimeZone", "-60");
		params.put("deviceCapabilities", "javascript");
		params.put("deviceScreenResolution", "1920x1080x24");
		params.put("deviceAcceptContent",
				"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
		params.put("deviceAcceptEncoding", "gzip, deflate");
		params.put("deviceAcceptLanguage", "en-GB");

		return params;
	}
}
