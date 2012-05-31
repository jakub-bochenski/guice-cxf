package com.google.code.inject.jaxrs.scope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.cxf.message.Message;

import com.google.code.inject.jaxrs.util.ScopeUtils;
import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.ScopeAnnotation;
import com.google.inject.internal.util.$Preconditions;

public class CXFScopes {
	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	@ScopeAnnotation
	public @interface Request {
	}

	private CXFScopes() {
	}

	/**
	 * Keys bound in request-scope which are handled directly by
	 * GuiceInterceptorWrapper.
	 */
	private static final Set<Key<?>> REQUEST_CONTEXT_KEYS = Collections
			.unmodifiableSet(new HashSet<Key<?>>(Arrays.<Key<?>> asList(
			//					Key.get(HttpServletRequest.class),
			//					Key.get(HttpServletResponse.class),
			//					new Key<Map<String, String[]>>(RequestParameters.class) {}
					)));

	/**
	 * A threadlocal scope map for non-http request scopes. The {@link #REQUEST}
	 * scope falls back to this scope map if no http request is available, and
	 * requires {@link #scopeRequest} to be called as an alertnative.
	 */
	private static final ThreadLocal<Context> requestScopeContext = new ThreadLocal<Context>();

	/** A sentinel attribute value representing null. */
	enum NullObject {
		INSTANCE
	}

	/**
	 * CXF request scope.
	 */
	public static final Scope REQUEST = new Scope() {
		public <T> Provider<T> scope(final Key<T> key, final Provider<T> creator) {
			final String name = key.toString();
			return new Provider<T>() {
				public T get() {
					// Check if the alternate request scope should be used, if no HTTP
					// request is in progress.
					if (null == GuiceInterceptorWrapper.localContext.get()) {

						// We don't need to synchronize on the scope map
						// unlike the HTTP request because we're the only ones who have
						// a reference to it, and it is only available via a threadlocal.
						final Context context = requestScopeContext.get();
						if (null != context) {
							@SuppressWarnings("unchecked")
							T t = (T) context.map.get(name);

							// Accounts for @Nullable providers.
							if (NullObject.INSTANCE == t) {
								return null;
							}

							if (t == null) {
								t = creator.get();
								if (!ScopeUtils.isCircularProxy(t)) {
									// Store a sentinel for provider-given null values.
									context.map.put(name, t != null ? t
											: NullObject.INSTANCE);
								}
							}

							return t;
						} // else: fall into normal request scope and out of scope exception is thrown.
					}

					// Always synchronize and get/set attributes on the underlying request
					// object since Filters may wrap the request and change the value of
					// {@code GuiceInterceptorWrapper.getRequest()}.
					//
					// This _correctly_ throws up if the thread is out of scope.
					final Message request = GuiceInterceptorWrapper
							.getOriginalRequest();
					if (REQUEST_CONTEXT_KEYS.contains(key)) {
						// Don't store these keys as attributes, since they are handled by
						// GuiceInterceptorWrapper itself.
						return creator.get();
					}
					synchronized (request) {
						final Object obj = request.get(name);
						if (NullObject.INSTANCE == obj) {
							return null;
						}
						@SuppressWarnings("unchecked")
						T t = (T) obj;
						if (t == null) {
							t = creator.get();
							if (!ScopeUtils.isCircularProxy(t)) {
								request.put(name, (t != null) ? t
										: NullObject.INSTANCE);
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
			return "ServletScopes.REQUEST";
		}
	};

	/**
	 * Returns true if {@code binding} is request-scoped. If the binding is a
	 * {@link com.google.inject.spi.LinkedKeyBinding linked key binding} and
	 * belongs to an injector (i. e. it was retrieved via
	 * {@link Injector#getBinding Injector.getBinding()}), then this method will
	 * also return true if the target binding is request-scoped.
	 */
	public static boolean isRequestScoped(Binding<?> binding) {
		return ScopeUtils.isScoped(binding, REQUEST, Request.class);
	}

	/**
	 * Scopes the given callable inside a request scope.
	 * 
	 * @param callable
	 *            code to be executed which depends on the request scope.
	 *            Typically in another thread, but not necessarily so.
	 * @param seedMap
	 *            the initial set of scoped instances for Guice to seed the
	 *            request scope with. To seed a key with null, use {@code null}
	 *            as the value.
	 * @return a callable that when called will run inside the a request scope
	 *         that exposes the instances in the {@code seedMap} as scoped keys.
	 * @since 3.0
	 */
	public static <T> Callable<T> scopeRequest(final Callable<T> callable,
			Map<Key<?>, Object> seedMap) {
		$Preconditions
				.checkArgument(null != seedMap,
						"Seed map cannot be null, try passing in Collections.emptyMap() instead.");

		// Copy the seed values into our local scope map.
		final Context context = new Context();
		for (final Map.Entry<Key<?>, Object> entry : seedMap.entrySet()) {
			final Object value = validateAndCanonicalizeValue(entry.getKey(),
					entry.getValue());
			context.map.put(entry.getKey().toString(), value);
		}

		return new Callable<T>() {
			public T call() throws Exception {
				$Preconditions
						.checkState(
								null == GuiceInterceptorWrapper.localContext
										.get(),
								"An HTTP request is already in progress, cannot scope a new request in this thread.");
				$Preconditions
						.checkState(
								null == requestScopeContext.get(),
								"A request scope is already in progress, cannot scope a new request in this thread.");
				return context.call(callable);
			}
		};
	}

	/**
	 * Validates the key and object, ensuring the value matches the key type,
	 * and canonicalizing null objects to the null sentinel.
	 */
	private static Object validateAndCanonicalizeValue(Key<?> key, Object object) {
		if (object == null || object == NullObject.INSTANCE) {
			return NullObject.INSTANCE;
		}

		if (!key.getTypeLiteral().getRawType().isInstance(object)) {
			throw new IllegalArgumentException("Value[" + object + "] of type["
					+ object.getClass().getName()
					+ "] is not compatible with key[" + key + "]");
		}

		return object;
	}

	private static class Context {
		final Map<String, Object> map = new HashMap<String, Object>();
		volatile Thread owner;

		<T> T call(Callable<T> callable) throws Exception {
			final Thread oldOwner = owner;
			final Thread newOwner = Thread.currentThread();
			$Preconditions
					.checkState(oldOwner == null || oldOwner == newOwner,
							"Trying to transfer request scope but original scope is still active");
			owner = newOwner;
			final Context previous = requestScopeContext.get();
			requestScopeContext.set(this);
			try {
				return callable.call();
			} finally {
				owner = oldOwner;
				requestScopeContext.set(previous);
			}
		}
	}
}
