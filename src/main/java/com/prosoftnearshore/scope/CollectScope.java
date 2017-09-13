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
 * An {@link Scope} that collects several resources that are all used and eventually released together.
 * <p>
 * Sometimes, it makes sense to always use and release several resources together. One common way to achieve this is
 * by wrapping all those resources in a single object... initializes resources in its constructor... the {@code
 * close()} method closes all the wrapped resources.
 * <p>
 * Because some care is needed in order to ensure that none of the resources is leaked (even if an exception is thrown
 * before all of them are fully initialized), the initialization of all those resources is usually coded directly and
 * explicitly in a single {@code try}-with-resources statement. For example, you could safely obtain and use a buffered
 * reader into some bundled resource as follows:
 * <pre> {@code
 * String resourcename = ...
 * try (InputStream stream = this.getClass().getResourceAsStream(resourcename);
 *      InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
 *      BufferedReader br = new BufferedReader(reader)) {
 *   // use br here
 * }
 * }</pre>
 * <p>
 * But sometimes, it's desirable to abstract away the details of such initialization. For example, you may need to work
 * with two or more readers at the same time, each requiring the same steps to get initialized; but putting everything
 * directly in the same {@code try}-with-resources would lead to a very cluttered block of code.
 * So, instead of that you may want to be able to write something like:
 * <pre> {@code
 * try (BufferedReader br = getReader(RESOURCE_TXT);
 *      BufferedReader br2 = getReader(OTHER_RESOURCE_TXT);) {
 *   // use the resources
 * }
 * }</pre>
 * <p>
 * The {@code ChainScope} class is intended to help with writing methods such as {@code getReader()}, which must not
 * only return an initialized resource ready for use, but also prevent any leakage in the event of a failure
 * happening at any time during the initialization process. For example, using {@code ChainScope}, {@code getReader}
 * could be coded as follows:
 * <pre> {@code
 * BufferedReader getReader(String resourcename) {
 *   try (ChainScope s = ChainScope.getNew()) {
 *     InputStream stream = s.hook(this.getClass().getResourceAsStream(resourcename));
 *     InputStreamReader reader = s.hook(new InputStreamReader(stream, StandardCharsets.UTF_8));
 *     BufferedReader br = s.hook(new BufferedReader(reader));
 *     return s.release(br);
 *   }
 * }
 * }</pre>
 * <p>
 * Note that the code inside of {@code getReader} is very similar to the version before it was put inside the function,
 * with the following differences:
 * <ol>
 * <li>What's put in the resource specification of the {@code try} statement is a {@code ChainScope} variable, rather
 * than {@code stream}, {@code reader}, etc. This is because those resources should not get closed when exiting the
 * scope of the {@code try} statement if the entire block executes successfully.</li>
 * <li>Every time a new resource instance is created, it's immediately passed to a call of
 * {@code hook()}. This is for the {@code ChainScope} object to obtain a reference to the newly created
 * resource and close it if the scope exits prematurely due to an exception.</li>
 * <li>Finally, when the fully initialized resource is being returned, it's passed to a call of
 * {@code release()} to indicate the {@code ChainScope} that thet process succeeded and that the
 * resource should not be closed when leaving the scope.</li>
 * </ol>
 */
@SuppressWarnings("WeakerAccess" /* public API */)
public class CollectScope implements Scope {

    public static CollectScope getNew() {
        return new CollectScope();
    }

    <T extends AutoCloseable> T add(T resource) {
        return this.wrapper.add(resource);
    }

    @Override
    public void close() {
        try {
            this.wrapper.close();
        } finally {
            this.wrapper = WrapperScope.getNew();
        }
    }

    public WrapperScope release() {
        try {
            return this.wrapper;
        } finally {
            this.wrapper = WrapperScope.getNew();
        }
    }

    //
    // Private Members
    //

    private WrapperScope wrapper = WrapperScope.getNew();

    private CollectScope() {
        // hide constructor from API
    }

}
