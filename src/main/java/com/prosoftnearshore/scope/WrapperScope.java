/*
Copyright 2015 Prosoft, LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package com.prosoftnearshore.scope;

import java.util.ArrayDeque;
import java.util.Queue;

public class WrapperScope implements Scope {

	/**
	 * Construct and return a new instance of {@link WrapperScope}.
	 *
	 * @return a new instance of {@code WrapperScope}.
	 */
	public static WrapperScope getNew() {
		return new WrapperScope();
	}

	public <T extends AutoCloseable> T add(T resource) {
		this.resources.add(resource);
		return resource;
	}

	@Override
	public void close() throws RuntimeException {
		if (this.resources.isEmpty())
			return;

		try (AutoCloseable r = this.resources.remove()) {
			this.close();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new CloseException(e);
		}
	}

	//
	// Package-Private Members
	//
	void swap(WrapperScope other) {
		Queue<AutoCloseable> t = this.resources;
		this.resources = other.resources;
		other.resources = t;
	}

	//
	// Private Members
	//
	private Queue<AutoCloseable> resources = new ArrayDeque<>();

	private WrapperScope() {
		// hide constructor from API
	}

}
