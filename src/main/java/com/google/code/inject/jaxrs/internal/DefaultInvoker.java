package com.google.code.inject.jaxrs.internal;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.invoker.Invoker;

/**
 * Default invoker marker class. Do not use for actual operation.
 */
public final class DefaultInvoker implements Invoker {

	@Override
	public Object invoke(Exchange arg0, Object arg1) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Is an invoker a default marker.
	 * 
	 * @param invoker
	 *            instance to check
	 * @return true if invoker is an instance of DefaultInvoker
	 */
	public static boolean isDefault(Invoker invoker) {
		return invoker instanceof DefaultInvoker;
	}

}