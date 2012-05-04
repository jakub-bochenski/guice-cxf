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
package com.google.code.inject.jaxrs.util;

import static com.google.inject.Scopes.SINGLETON;
import static com.google.inject.internal.util.$Preconditions.checkArgument;
import static com.google.inject.internal.util.$Preconditions.checkState;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;

import com.google.inject.Binding;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.google.inject.Scope;
import com.google.inject.spi.ConstructorBinding;
import com.google.inject.spi.DefaultBindingScopingVisitor;
import com.google.inject.spi.DefaultBindingTargetVisitor;
import com.google.inject.spi.LinkedKeyBinding;
import com.google.inject.spi.ProviderBinding;
import com.google.inject.spi.UntargettedBinding;

public class BindingProvider<T> {
	private static final Scope NO_SCOPE = new Scope() {

		@Override
		public <T> Provider<T> scope(Key<T> arg0, Provider<T> arg1) {
			throw new UnsupportedOperationException();
		}
	};

	private final Key<T> key;
	private Class<?> actualType;
	private Binding<T> binding;
	private Scope scope;

	public BindingProvider(Key<T> key) {
		this.key = key;
	}

	public Class<?> getActualType() {
		return actualType;
	}

	public Binding<T> getBinding() {
		return binding;
	}

	public Key<T> getKey() {
		return key;
	}

	public Scope getScope() {
		checkState(null != scope, "Scope was not resolved");
		if (NO_SCOPE == scope)
			return null;

		return scope;
	}

	protected void setBinding(final Binding<T> binding,
			final Map<Class<? extends Annotation>, Scope> scopeBindings) {
		this.binding = binding;
		this.scope = binding
				.acceptScopingVisitor(new DefaultBindingScopingVisitor<Scope>() {
					@Override
					public Scope visitEagerSingleton() {
						return SINGLETON;
					}

					@Override
					public Scope visitNoScoping() {
						return NO_SCOPE;
					}

					@Override
					protected Scope visitOther() {
						return null;
					}

					@Override
					public Scope visitScope(Scope scope) {
						return scope;
					}

					@Override
					public Scope visitScopeAnnotation(
							Class<? extends Annotation> scopeAnnotation) {
						return scopeBindings.get(scopeAnnotation);
					}
				});

		this.actualType = binding
				.acceptTargetVisitor(new DefaultBindingTargetVisitor<T, Class<?>>() {
					@Override
					public Class<?> visit(ConstructorBinding<? extends T> b) {
						return b.getConstructor().getDeclaringType()
								.getRawType();
					}

					@Override
					public Class<?> visit(LinkedKeyBinding<? extends T> b) {
						return b.getLinkedKey().getTypeLiteral().getRawType();
					}

					@Override
					public Class<?> visit(
							ProviderBinding<? extends T> providerBinding) {
						try {
							// FIXME this will incorrectly return any first type variable, 
							// not necessarily that of Provider<T>
							final TypeVariable<?> providerType = providerBinding
									.getProvider().getClass()
									.getTypeParameters()[0];

							final Type[] bounds = providerType.getBounds();
							checkArgument(bounds.length == 1);

							final Class<?> bound = (Class<?>) bounds[0];
							checkArgument(key.getTypeLiteral().getRawType()
									.isAssignableFrom(bound));

							return bound;
						} catch (final RuntimeException e) {
							throw new ProvisionException(
									"Unable to resolve target class for " + key,
									e);
						}
					}

					public Class<?> visit(UntargettedBinding<? extends T> b) {
						return b.getKey().getTypeLiteral().getRawType();
					}

					@Override
					protected Class<?> visitOther(Binding<? extends T> binding) {
						throw new ProvisionException(
								"Unable to resolve target class for " + binding);
					}
				});
	}

	@Inject
	void setInjector(final Injector injector) {
		setBinding(injector.getBinding(key), injector.getScopeBindings());
	}
}
