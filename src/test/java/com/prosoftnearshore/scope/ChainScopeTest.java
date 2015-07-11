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

import static org.junit.Assert.*;

import org.junit.Test;

import static org.mockito.Mockito.*;

public class ChainScopeTest {

	@SuppressWarnings("static-method")
	@Test
	public void closeIsIdempotent() throws Exception {
		@SuppressWarnings("resource")
		ChainScope scope = ChainScope.getNew();
		@SuppressWarnings("resource")
		AutoCloseable resource = mock(AutoCloseable.class);

		scope.hook(resource);
		scope.close();
		scope.close();
		// verify that resource.close() was called exactly once
		verify(resource).close();
	}

	@SuppressWarnings("static-method")
	@Test
	public void closeIsIdempotentOnFailure() throws Exception {
		@SuppressWarnings("resource")
		ChainScope scope = ChainScope.getNew();

		@SuppressWarnings("resource")
		AutoCloseable resource = mock(AutoCloseable.class);
		doThrow(Exception.class).when(resource).close();

		scope.hook(resource);
		try {
			scope.close();
			fail("close() should throw the first time");
		} catch (CloseException e) {
			// the scope should not attempt to close the resource a second time
			// so close() should not throw again
			scope.close();
			verify(resource).close();
		}
	}

	@SuppressWarnings("static-method")
	@Test(expected = CloseException.class)
	public void checkedExceptionIsWrappedInCloseException() throws Exception {
		@SuppressWarnings("resource")
		ChainScope scope = ChainScope.getNew();

		@SuppressWarnings("resource")
		AutoCloseable resource = mock(AutoCloseable.class);
		doThrow(Exception.class).when(resource).close();

		scope.hook(resource);
		scope.close();
	}

	@SuppressWarnings("static-method")
	@Test
	public void releasePreventsClosingResources() throws Exception {
		@SuppressWarnings("resource")
		ChainScope scope = ChainScope.getNew();
		@SuppressWarnings("resource")
		AutoCloseable resource = mock(AutoCloseable.class);
		
		scope.hook(resource);
		assertSame(resource, scope.release(resource));
		scope.close();
		
		// verify that the resource was not in fact attempted to be closed
		verify(resource, never()).close();
	}
}
