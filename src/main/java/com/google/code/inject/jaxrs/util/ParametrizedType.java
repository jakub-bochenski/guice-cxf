package com.google.code.inject.jaxrs.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.google.inject.Key;

public abstract class ParametrizedType implements ParameterizedType {
	private final Type rawType;

	protected ParametrizedType(Type rawType) {
		super();
		this.rawType = rawType;
	}

	@Override
	public final Type getRawType() {
		return rawType;
	}

	@Override
	public Type getOwnerType() {
		return null;
	}

	@SuppressWarnings("unchecked")
	public <T> Key<T> asKey() {
		return (Key<T>) Key.get(this);
	}
}