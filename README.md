Disclaimer: Please note that we no longer support older versions of SDKs and Modules. We recommend that the latest versions are used.

# README

# Contents
- Introduction
- Prerequisites
- Using the Gateway SDK
- License

# Introduction
This Java SDK provides an easy method to integrate with the payment gateway.
 - The Gateway.java file contains the main body of the SDK.

# Prerequisites
- The SDK requires the following prerequisites to be met in order to function correctly:
    - For convenience the following Java libraries have been included for easier set up:
	- commons-cli-1,4
	- commons-codec-1.13
	- commons-io-2.8.0
	- commons-lang3-3.11
	- commons-text-1.9
	- hamcrest-2.2
	- httpclient5-5.0.2
	- httpclient5-cache-5.0.2
	- httpclient5-fluent-5.0.2
	- httpclient5-testing-5.0.2
	- httpclient5-win-5.0.2
	- httpcore5-5.0.2
	- httpcore5-h2-5.0.2
	- httpcore5-reactive-5.0.2
	- httpcore5-testing-5.0.2
	- jna-5.2.0
	- jna-platform-5.2.0
	- junit-4.13
	- reactive-streams-1.0.2
	- rxjava-2.2.8
	- slf4j-api-1.7.25

> <span style="color: red">Please note that we can only offer support for the SDK itself. While every effort has been made to ensure the sample code is complete and bug free, it is only a guide and should not be used in a production environment.</span>

# Using the Gateway SDK

Require the gateway SDK into your project

```
    import java.util.HashMap;
    import com.paymentnetwork.Gateway;
```

Instantiate the Gateway object

```
        var gateway = new Gateway("merchantid", "secretkey", "https://gateway.example.com/direct/", "https://gateway.example.com/paymentform/", null);

```

Once your SDK has been required. You create your request array, for example:
```
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
	params.put("cardExpiryYear", "20");
	params.put("cardCVV", "083");
	params.put("customerName", "Test Customer");
	params.put("customerEmail", "test@testcustomer.com");
	params.put("customerAddress", "16 Test Street");
	params.put("customerPostCode", "TE15 5ST");
	params.put("orderRef", "Test purchase");

	// The following fields are mandatory for 3DS v2 direct integrations
	params.put("remoteAddress", "10.10.10.10");
	params.put("merchantCategoryCode", "5411");
	params.put("threeDSVersion", "2");
	params.put("threeDSRedirectURL", "https://example.net/returnUrl?acs=1"); // PLACEHOLDER

```
> NB: This is a sample request. The gateway features many more options. Please see our integration guides for more details.

Then, depending on your integration method, you'd either call:

```
	var options = new HashMap<String, String>();
	var gatewayResponse = gTest.directRequest(params, options);
```

OR

```
	var options = new HashMap<String, String>();
	var gatewayResponse = gateway.hostedRequest(params, options);
```

And then handle the response received from the gateway as per our integration guides.

License
----
MIT

**Disclaimer**

Sample code, SDKs and modules have been created as reference material only. Modules are developed and tested against vanilla base platform installs only. Any further module compatibility would need to be tested by the user/merchant/developer. Version support is as shown within the associated VERSION section. All sample code, SDKs and modules offer foundation transaction functionality for merchant and developers to use as a guide and/or to adapt, enhance or otherwise build upon. For the avoidance of doubt, this means that some desired functionality may not be useable or exist. All sample code, SDKs or modules that are used will require complete full end to end testing by the user/merchant/developer. Further to this, use of any sample code, SDKs or modules, Cardstream bears no responsibility for; nor extends any warranty in regard to; nor accepts any liability arising due to any changes or errors in functionality that may result. Developers, merchants or other users of any sample code, SDKs or modules accept these conditions de facto upon usage.
