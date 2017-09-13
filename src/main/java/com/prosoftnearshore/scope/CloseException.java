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

/**
 * Thrown when a checked exception is thrown during the execution of {@link Scope#close()}.
 * <p>
 * The checked originally thrown would be accessible through {@link #getCause()} method. If additional exceptions
 * are thrown while any remaining resources are also attempted to be closed, they all will be
 * {@linkplain Exception#addSuppressed(Throwable) suppressed}.
 */
@SuppressWarnings("WeakerAccess")
public class CloseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    CloseException(Throwable cause) {
        super(cause);
    }

}
