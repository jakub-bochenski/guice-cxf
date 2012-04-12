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

import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.message.Message;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class GuicePerRequestResourceProvider<T> implements ResourceProvider {

	private final Provider<T> provider;
	private final Class<?> actualType;

	@Inject
	protected GuicePerRequestResourceProvider(Provider<T> provider) {
		this.provider = provider;
		this.actualType = provider.get().getClass();
	}

	@Override
	public Object getInstance(Message m) {
		return provider.get();
	}

	@Override
	public void releaseInstance(Message m, Object o) {
		// NOOP
	}

	@Override
	public Class<?> getResourceClass() {
		return actualType;
	}

	@Override
	public boolean isSingleton() {
		return false;
	}

}