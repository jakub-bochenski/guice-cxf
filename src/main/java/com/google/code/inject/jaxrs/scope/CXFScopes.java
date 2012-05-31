package com.google.code.inject.jaxrs.scope;

import static com.google.code.inject.jaxrs.scope.CXFScopes.Marker.NULL;
import static com.google.code.inject.jaxrs.scope.GuiceInterceptorWrapper.getOriginalMessage;
import static com.google.code.inject.jaxrs.util.ScopeUtils.isCircularProxy;
import static com.google.inject.internal.util.$Preconditions.checkArgument;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Collections.unmodifiableSet;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.MessageContextImpl;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.Service;

import com.google.code.inject.jaxrs.util.ScopeUtils;
import com.google.inject.AbstractModule;
import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Scope;
import com.google.inject.ScopeAnnotation;

public class CXFScopes {
	/**
	 * Binds the scope annotation and provides @Context instances
	 */
	public static class Module extends AbstractModule {

		@SuppressWarnings("unchecked")
		@Override
		protected void configure() {
			bindScope(RequestScope.class, REQUEST);

			for (final Key<?> key : MESSAGE_CONTEXT_KEYS) {
				bind(key).toProvider(DUMMY_PROVIDER).in(REQUEST);
			}

		}

		@Provides
		@RequestScope
		protected MessageContext provideMessageContext(Message m) {
			return new MessageContextImpl(m);
		}

		@Provides
		@RequestScope
		protected Exchange provideExchange(Message m) {
			return m.getExchange();
		}

		@Provides
		@RequestScope
		protected Service provideService(MessageContext messageContext) {
			return (Service) messageContext
					.getContextualProperty(Service.class);
		}

		@Provides
		@RequestScope
		protected HttpHeaders provideHttpHeaders(MessageContext messageContext) {
			return messageContext.getHttpHeaders();
		}

		@Provides
		@RequestScope
		protected Providers provideProviders(MessageContext messageContext) {
			return messageContext.getProviders();
		}

		@Provides
		@RequestScope
		protected UriInfo provideUriInfo(MessageContext messageContext) {
			return messageContext.getUriInfo();
		}

		@Provides
		@RequestScope
		protected Request provideRequest(MessageContext messageContext) {
			return messageContext.getRequest();
		}

		@Provides
		@RequestScope
		protected SecurityContext provideSecurityContext(
				MessageContext messageContext) {
			return messageContext.getSecurityContext();
		}

	}

	@Target({ TYPE, METHOD })
	@Retention(RUNTIME)
	@ScopeAnnotation
	public @interface RequestScope {
	}

	/**
	 * Dummy marker to bind {@value #MESSAGE_CONTEXT_KEYS} to
	 */
	@SuppressWarnings("rawtypes")
	private static final Provider DUMMY_PROVIDER = new Provider<Void>() {

		@Override
		public Void get() {
			throw new OutOfScopeException("This should never happen -- check "
					+ CXFScopes.class + ".MESSAGE_CONTEXT_KEYS");
		}
	};

	private CXFScopes() {
	}

	/**
	 * This keys will be retrieved from message via
	 * {@link #getKeyFromMessage(Message, Key)}
	 */
	private static final Set<Key<?>> MESSAGE_CONTEXT_KEYS = unmodifiableSet(new HashSet<Key<?>>(
			Arrays.<Key<?>> asList(Key.get(Message.class))));

	/** Marker for @Nullable providers */
	enum Marker {
		NULL
	}

	/**
	 * CXF message scope.
	 */
	public static final Scope REQUEST = new Scope() {
		public <T> Provider<T> scope(final Key<T> key, final Provider<T> creator) {
			final String name = key.toString();
			return new Provider<T>() {
				public T get() {

					final Message message = getOriginalMessage();
					synchronized (message) {

						if (MESSAGE_CONTEXT_KEYS.contains(key))
							return getKeyFromMessage(message, key);

						final Object obj = message.get(name);
						if (NULL == obj)
							return null;

						@SuppressWarnings("unchecked")
						T t = (T) obj;
						if (t == null) {
							t = creator.get();
							if (!isCircularProxy(t)) {
								message.put(name, (t != null) ? t : NULL);
							}
						}

						return t;
					}
				}

				@Override
				public String toString() {
					return String.format("%s[%s]", creator, REQUEST);
				}
			};
		}

		@Override
		public String toString() {
			return "CXFScopes.REQUEST";
		}
	};

	/**
	 * Retrieve an existing instance from message.
	 * <p>
	 * This method will be called for all keys in {@link #MESSAGE_CONTEXT_KEYS}
	 * 
	 * @param message
	 *            request message
	 * @param key
	 *            key to retrieve
	 * @return instance of T, never null
	 */
	private static <T> T getKeyFromMessage(Message message, Key<T> key) {
		checkArgument(key.getAnnotationType() == null,
				"Annotated keys not allowed");
		final Class<? super T> rt = key.getTypeLiteral().getRawType();

		if (Message.class.equals(rt)) {
			@SuppressWarnings("unchecked")
			final T t = (T) message;
			return t;
		}

		throw new UnsupportedOperationException("Key " + key);
	}

	/**
	 * Returns true if {@code binding} is message-scoped. If the binding is a
	 * {@link com.google.inject.spi.LinkedKeyBinding linked key binding} and
	 * belongs to an injector (i. e. it was retrieved via
	 * {@link Injector#getBinding Injector.getBinding()}), then this method will
	 * also return true if the target binding is message-scoped.
	 */
	public static boolean isRequestScoped(Binding<?> binding) {
		return ScopeUtils.isScoped(binding, REQUEST, RequestScope.class);
	}

}
