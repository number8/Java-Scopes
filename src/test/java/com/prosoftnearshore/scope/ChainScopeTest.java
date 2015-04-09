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
