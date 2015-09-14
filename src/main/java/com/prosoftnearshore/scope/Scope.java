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

public interface Scope extends AutoCloseable {

	/**
	 * Closes this scope, relinquishing any underlying resources it owns.
	 * This method is invoked automatically on objects managed by the
	 * {@code try-with-resources} statement.
	 * <p>
	 * An {@link Scope} is expected to manage one or more arbitrary
	 * implementations of {@link AutoCloseable}, meaning that any
	 * {@link Exception} could potentially be thrown when closing them and
	 * scopes are expected to handle properly the release of all the underlying
	 * resources even in that case.
	 * <p>
	 * If the scope owns more than one resource, it is guaranteed that all of
	 * them are attempted to be closed even if some of them fail by throwing
	 * exceptions. In such cases, all exceptions thrown are collected until all
	 * the resources have been closed, and then the primary exception is thrown
	 * with any additional ones suppressed in a way similar to what's specified
	 * for {@code try-with-resources}.
	 * <p>
	 * If the scope is already closed then invoking this method should have no
	 * effect.
	 *
	 * @throws CloseException   if some underlying resource throws a checked exception. The
	 *                          {@code CloseException} would wrap the checked exception
	 *                          originally thrown.
	 * @throws RuntimeException if any of the resources managed by this scope throw an
	 *                          unchecked exception when trying to close them.
	 */
	@Override
	void close() throws RuntimeException;
}
