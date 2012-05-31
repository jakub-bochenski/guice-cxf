package com.google.code.inject.jaxrs.scope;

import static org.apache.cxf.phase.Phase.INVOKE;

import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.Callable;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.interceptor.ServiceInvokerInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.PhaseInterceptor;

import com.google.inject.OutOfScopeException;
import com.google.inject.internal.util.$Preconditions;

public class GuiceInterceptorWrapper extends AbstractPhaseInterceptor<Message> {

	static class Context {
		final Message originalRequest;
		volatile Thread owner;
		final Message request;

		Context(Message originalRequest, Message request) {
			this.originalRequest = originalRequest;
			this.request = request;
		}

		<T> T call(Callable<T> callable) throws Exception {
			final Thread oldOwner = owner;
			final Thread newOwner = Thread.currentThread();
			$Preconditions
					.checkState(oldOwner == null || oldOwner == newOwner,
							"Trying to transfer request scope but original scope is still active");
			owner = newOwner;
			final Context previous = localContext.get();
			localContext.set(this);
			try {
				return callable.call();
			} finally {
				owner = oldOwner;
				localContext.set(previous);
			}
		}

		public Message getOriginalRequest() {
			return originalRequest;
		}

		public Message getRequest() {
			return request;
		}
	}

	static final ThreadLocal<Context> localContext = new ThreadLocal<Context>();

	private static Context getContext() {
		final Context context = localContext.get();
		if (context == null) {
			throw new OutOfScopeException(
					"Cannot access scoped object. Either we"
							+ " are not currently inside a request, or you may"
							+ " have forgotten to apply "
							+ GuiceInterceptorWrapper.class.getName()
							+ " as an interceptor for this request.");
		}
		return context;
	}

	static Message getOriginalRequest() {
		return getContext().getOriginalRequest();
	}

	private final PhaseInterceptor<Message> delegate;

	public GuiceInterceptorWrapper() {
		this(new ServiceInvokerInterceptor());
	}

	public GuiceInterceptorWrapper(PhaseInterceptor<Message> delegate) {
		super(INVOKE);
		setBefore(Collections.singleton(ServiceInvokerInterceptor.class
				.getName()));
		this.delegate = delegate;
	}

	@Override
	public void handleMessage(final Message request) throws Fault {
		final InterceptorChain chain = request.getInterceptorChain();
		final Iterator<Interceptor<? extends Message>> it = chain.iterator();
		while (it.hasNext()) {
			final Interceptor<? extends Message> next = it.next();
			if (next instanceof ServiceInvokerInterceptor) {
				chain.remove(next);
			}
		}

		final Context previous = localContext.get();
		final Message originalRequest = (previous != null) ? previous
				.getOriginalRequest() : request;

		try {
			new Context(originalRequest, request).call(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					delegate.handleMessage(request);
					return null;
				}
			});
		} catch (final Fault e) {
			throw e;
		} catch (final Exception e) {
			throw new Fault(e);
		}
	}
}