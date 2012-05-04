package com.google.code.inject.jaxrs.internal;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import com.google.inject.Inject;
import com.google.inject.Injector;

public class SubresourceInterceptor implements MethodInterceptor {

	private Injector injector;

	@Override
	public Object invoke(MethodInvocation method) throws Throwable {
		final Class<?> rt = method.getMethod().getReturnType();
		return injector.getInstance(rt);
	}

	@Inject
	public void setInjector(Injector injector) {
		this.injector = injector;
	}

}
