/*
 * Copyright 2012 Jakub Bocheñski (kuba.bochenski@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.code.inject.jaxrs;

import static com.google.inject.internal.util.$Preconditions.checkNotNull;
import static com.google.inject.internal.util.$Preconditions.checkState;
import static com.google.inject.multibindings.Multibinder.newSetBinder;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.ext.RequestHandler;
import org.apache.cxf.jaxrs.ext.ResponseHandler;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.invoker.Invoker;

import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.multibindings.Multibinder;

/**
 * CXF EDSL Module.
 * <p>
 * 
 * <b>Example usage:</b>
 * 
 * <pre>
 * protected void configureResources() {
 * 	serve().atAddress(&quot;/rest&quot;);
 * 
 * 	publish(MyResource.class);
 * 
 * 	readAndWriteBody(JAXBElementProvider.class);
 * 	readAndWriteBody(JSONProvider.class);
 * 
 * 	mapExceptions(ApplicationExceptionMapper.class);
 * }
 * </pre>
 * 
 * then do
 * 
 * <pre>
 * injector.getInstance(JAXRSServerFactoryBean.class).create();
 * </pre>
 * 
 * <p>
 * 
 * <h3>Language elements</h3>
 * <p>
 * Use <tt>publish()</tt> to register a resource class - a custom
 * <tt>ResourceProvider</tt> will be bound for each resource class. It's a
 * 'per-request' type and will get a new instance for each incoming request.
 * </p>
 * <p>
 * Use <tt>serve()</tt> to configure server, e.g. set the root address.
 * </p>
 * <p>
 * The following methods let you register JAX-RS <tt>@Provider</tt>s:
 * </p>
 * <ul>
 * <li><tt>handleRequest()</tt> - register a <tt>RequestHandler</tt>;</li>
 * <li><tt>handleResponse()</tt> - register a <tt>ResponseHandler</tt>;</li>
 * <li><tt>mapExceptions()</tt> - register an <tt>ExceptionMapper</tt>;</li>
 * <li><tt>readBody()</tt> - register a <tt>MessageBodyReader</tt>;</li>
 * <li><tt>writeBody()</tt> - register a <tt>MessageBodyWriter</tt>;</li>
 * <li><tt>writeAndReadBody()</tt> - register a class that is both a
 * <tt>MessageBodyReader</tt> and a <tt>MessageBodyWriter</tt> (e.g.
 * <tt>JAXBElementProvider</tt> or <tt>JSONProvider</tt>);</li>
 * <li><tt>provide()</tt> - generic method to register any JAX-RS
 * <tt>@Provider</tt>, it's best to use specific methods if available, since
 * they are type safe;</li>
 * </ul>
 * <p>
 * Above methods return a standard <tt>ScopedBindingBuilder</tt> so that you can
 * use all the regular Guice constructs.
 * </p>
 * <p>
 * <i>Please note that a single instance of each <tt>@Provider</tt> class will
 * be passed to the <tt>ServerFactoryBean</tt>, regardless of the scope.</i>
 * </p>
 * <p>
 * Use <tt>invokeVia()</tt> to register custom invoker.
 * </p>
 * <h3>Binding resources and
 * providers</h3>
 * <p>
 * It is possible to bind concrete classes, but a very nice feature is the
 * ability to bind interfaces.
 * </p>
 * 
 * <pre>
 * protected void configureResources() {
 * 	publish(ResourceInterface.class);
 * }
 * </pre>
 * <p>
 * Then in a separate module you can do something like
 * <tt>bind(ResourceInterface.class).to(ResourceImpl.class)</tt> to define the
 * concrete implementation. This let's you easily register mock objects for
 * testing.
 * </p>
 * <p>
 * Another use of indirect binding is configuring the <tt>@Provider</tt>s, see
 * here an example configuration of the <tt>JSONProvider</tt>.
 * </p>
 * 
 * <pre>
 * &#064;Provides
 * public JSONProvider provdeJsonProvider(
 * 		&#064;Named(&quot;ignoreNamespaces&quot;) boolean ignoreNamespaces) {
 * 	final JSONProvider json = new JSONProvider();
 * 	json.setIgnoreNamespaces(ignoreNamespaces);
 * 	return json;
 * }
 * </pre>
 * <p>
 * <i>Of course if you implement your own <tt>@Provider</tt>s it's best to use
 * constructor/method injections directly on them.</i>
 * 
 * <h3>Bindings</h3>
 * 
 * The most important binding provided by the CXFModule is the
 * <tt>JAXRSServerFactoryBean</tt>. You can use it to easily create a server by
 * doing <tt>injector.getInstance(JAXRSServerFactoryBean.class).create()</tt>
 * </p>
 * <p>
 * A set of <tt>ResourceProvider</tt>s will be bound using the multibinder. For
 * each resource class a <tt>{@link GuicePerRequestResourceProvider}</tt>
 * parametrized with the resource type will be registered.
 * </p>
 * <p>
 * A <tt>Set&lt;Object&gt;</tt> annotated with <tt>{@link JaxRsProvider}</tt>
 * will be bound containting an instance of each registered JAX-RS Provider
 * </p>
 * <p>
 * A singleton instance of <tt>{@link ServerConfiguration}</tt> will be bound
 * with configuration options.
 * </p>
 * <p>
 * If a custom <tt>Invoker</tt> was configured it will be bound, otherwise a
 * <tt>{@link DefaultInvoker}</tt> will be bound.
 * </p>
 * <p>
 * With the exception of <tt>{@link ServerConfiguration}</tt> bean no instances
 * of business classes are created during binding.
 * </p>
 */
public abstract class CXFModule extends AbstractModule {

	/**
	 * Default invoker marker class. Do not use for actual operation.
	 */
	public final static class DefaultInvoker implements Invoker {

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

	/**
	 * Annotation for marking JAX-RS providers
	 */
	@BindingAnnotation
	@Target({ FIELD, PARAMETER, METHOD })
	@Retention(RUNTIME)
	public @interface JaxRsProvider {

	}

	private final static class ServerConfig implements ServerConfiguration,
			ServerConfigurationBuilder {
		private String address = "/";
		private boolean staticResourceResolution = false;

		@Override
		public ServerConfigurationBuilder atAddress(String address) {
			this.address = address;
			return this;
		}

		@Override
		public String getAddress() {
			return address;
		}

		@Override
		public boolean isStaticResourceResolution() {
			return staticResourceResolution;
		}

		@Override
		public ServerConfigurationBuilder withStaticResourceResolution() {
			this.staticResourceResolution = true;
			return this;
		}

	}

	/**
	 * Server config bean
	 */
	public interface ServerConfiguration {

		String getAddress();

		boolean isStaticResourceResolution();

	}

	/**
	 * Server config builder
	 */
	protected interface ServerConfigurationBuilder {

		/**
		 * Server root address. Defaults to "/".
		 * 
		 * @param address
		 *            root address
		 * @return self
		 */
		ServerConfigurationBuilder atAddress(String address);

		/**
		 * Use static resource resolution
		 * 
		 * @return self
		 */
		ServerConfigurationBuilder withStaticResourceResolution();

	}

	private ServerConfig config;

	private boolean customInvoker;

	private Multibinder<Object> providers;

	private Multibinder<ResourceProvider> resourceProviders;

	@Override
	protected final void configure() {
		checkState(resourceProviders == null, "Re-entry is not allowed.");
		checkState(providers == null, "Re-entry is not allowed.");
		checkState(config == null, "Re-entry is not allowed.");

		resourceProviders = newSetBinder(binder(), ResourceProvider.class);
		providers = newSetBinder(binder(), Object.class, JaxRsProvider.class);
		config = new ServerConfig();
		customInvoker = false;

		try {
			configureResources();
			bind(ServerConfiguration.class).toInstance(config);
			bind(JAXRSServerFactoryBean.class).toProvider(
					JaxRsServerFactoryBeanProvider.class).in(Singleton.class);
			if (!customInvoker)
				bind(Invoker.class).to(DefaultInvoker.class);

		} finally {
			resourceProviders = null;
			providers = null;
			config = null;
		}
	}

	/**
	 * Override this method to configure CXF
	 */
	protected abstract void configureResources();

	/**
	 * DRY binding helper for @Provider
	 * 
	 * @param key
	 *            key to bind
	 */
	private void doBind(Key<?> key) {
		checkNotNull(key);
		providers.addBinding().to(key);
	}

	/**
	 * DRY binding helper for @Provider
	 * 
	 * @param type
	 *            type to bind
	 */
	private void doBind(Type type) {
		checkNotNull(type);
		doBind(Key.get(type));
	}

	/**
	 * DRY binding helper for @Provider
	 * 
	 * @param type
	 *            type to bind
	 */
	private void doBind(TypeLiteral<?> type) {
		checkNotNull(type);
		doBind(Key.get(type));
	}

	/**
	 * Configure server
	 * 
	 * @return configuration builder
	 */
	protected final ServerConfigurationBuilder serve() {
		return this.config;
	}

	/**
	 * Bind custom invoker
	 * 
	 * @param type
	 *            type to bind
	 * @return binding builder for the invoker
	 */
	protected final ScopedBindingBuilder invokeVia(Class<? extends Invoker> type) {
		checkState(!customInvoker, "Custom invoker bound twice");
		this.customInvoker = true;
		return bind(Invoker.class).to(type);
	}

	/**
	 * Bind custom invoker
	 * 
	 * @param type
	 *            type to bind
	 * @return binding builder for the invoker
	 */
	protected final ScopedBindingBuilder invokeVia(Key<? extends Invoker> type) {
		checkState(!customInvoker, "Custom invoker bound twice");
		this.customInvoker = true;
		return bind(Invoker.class).to(type);
	}

	/**
	 * Bind custom invoker
	 * 
	 * @param type
	 *            type to bind
	 * @return binding builder for the invoker
	 */
	protected final ScopedBindingBuilder invokeVia(
			TypeLiteral<? extends Invoker> type) {
		checkState(!customInvoker, "Custom invoker bound twice");
		this.customInvoker = true;
		return bind(Invoker.class).to(type);
	}

	/**
	 * Bind a resource class
	 * 
	 * @param key
	 *            to bind
	 */
	protected final void publish(final Key<?> key) {
		checkNotNull(key);

		@SuppressWarnings("unchecked")
		final Key<? extends ResourceProvider> providerKey = (Key<? extends ResourceProvider>) Key
				.get(new ParameterizedType() {

					@Override
					public Type[] getActualTypeArguments() {
						return new Type[] { key.getTypeLiteral().getType() };
					}

					@Override
					public Type getOwnerType() {
						return null;
					}

					@Override
					public Type getRawType() {
						return GuicePerRequestResourceProvider.class;
					}
				});
		resourceProviders.addBinding().to(providerKey).in(Singleton.class);

	}

	/**
	 * Bind a resource class
	 * 
	 * @param type
	 *            to bind
	 */
	protected final void publish(final Type type) {
		checkNotNull(type);
		publish(Key.get(type));
	}

	/**
	 * Bind a resource class
	 * 
	 * @param type
	 *            to bind
	 */
	protected final void publish(final TypeLiteral<?> type) {
		checkNotNull(type);
		publish(Key.get(type));
	}

	protected final void handleRequest(Class<? extends RequestHandler> type) {
		doBind(type);
	}

	protected final void handleRequest(Key<? extends RequestHandler> key) {
		doBind(key);
	}

	protected final void handleRequest(
			TypeLiteral<? extends RequestHandler> type) {
		doBind(type);
	}

	protected final void handleResponse(Class<? extends ResponseHandler> type) {
		doBind(type);
	}

	protected final void handleResponse(Key<? extends ResponseHandler> key) {
		doBind(key);
	}

	protected final void handleResponse(
			TypeLiteral<? extends ResponseHandler> type) {
		doBind(type);
	}

	protected final void mapExceptions(Class<? extends ExceptionMapper<?>> type) {
		doBind(type);
	}

	protected final void mapExceptions(Key<? extends ExceptionMapper<?>> key) {
		doBind(key);
	}

	protected final void mapExceptions(
			TypeLiteral<? extends ExceptionMapper<?>> type) {
		doBind(type);
	}

	protected final void provide(Class<?> type) {
		doBind(type);
	}

	protected final void provide(Key<?> key) {
		doBind(key);
	}

	protected final void provide(TypeLiteral<?> type) {
		doBind(type);
	}

	protected final void readBody(Class<? extends MessageBodyReader<?>> type) {
		doBind(type);
	}

	protected final void readBody(Key<? extends MessageBodyReader<?>> key) {
		doBind(key);
	}

	protected final <T extends MessageBodyReader<?> & MessageBodyWriter<?>> void readBody(
			TypeLiteral<T> type) {
		doBind(type);
	}

	protected final <T extends MessageBodyReader<?> & MessageBodyWriter<?>> void writeAndReadBody(
			Class<T> type) {
		doBind(type);
	}

	protected final <T extends MessageBodyReader<?> & MessageBodyWriter<?>> void writeAndReadBody(
			Key<T> key) {
		doBind(key);
	}

	protected final void writeAndReadBody(
			TypeLiteral<? extends MessageBodyReader<?>> type) {
		doBind(type);
	}

	protected final void writeBody(Class<? extends MessageBodyWriter<?>> type) {
		doBind(type);
	}

	protected final void writeBody(Key<? extends MessageBodyWriter<?>> key) {
		doBind(key);
	}

	protected final void writeBody(
			TypeLiteral<? extends MessageBodyWriter<?>> type) {
		doBind(type);
	}

}
