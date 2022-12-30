package com.paymentnetwork;

import java.util.Comparator;

class FieldCompare implements Comparator<String> {
	@Override
	public int compare(String o1, String o2) {
		if (o1.contains("[")) {
			o1 = o1.substring(0, o1.indexOf("["));
		}

		if (o2.contains("[")) {
			o2 = o2.substring(0, o2.indexOf("["));
		}
		return o1.compareTo(o2);
	}
}
