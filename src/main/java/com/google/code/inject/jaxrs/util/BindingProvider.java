package com.google.code.inject.jaxrs.util;

import static com.google.inject.internal.util.$Preconditions.checkArgument;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import com.google.inject.Binding;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.ProvisionException;
import com.google.inject.spi.ConstructorBinding;
import com.google.inject.spi.DefaultBindingTargetVisitor;
import com.google.inject.spi.LinkedKeyBinding;
import com.google.inject.spi.ProviderBinding;
import com.google.inject.spi.UntargettedBinding;

public class BindingProvider<T> {
	private final Key<T> key;
	private Class<?> actualType;

	public BindingProvider(Key<T> key) {
		this.key = key;
	}

	@Inject
	void setInjector(Injector injector) {
		this.actualType = injector.getBinding(key).acceptTargetVisitor(
				new DefaultBindingTargetVisitor<T, Class<?>>() {
					public Class<?> visit(UntargettedBinding<? extends T> b) {
						return b.getKey().getTypeLiteral().getRawType();
					}

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

					@Override
					protected Class<?> visitOther(Binding<? extends T> binding) {
						throw new ProvisionException(
								"Unable to resolve target class for " + binding);
					}
				});
	}

	public Class<?> getActualType() {
		return actualType;
	}
}
