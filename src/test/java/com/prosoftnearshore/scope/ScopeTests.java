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

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class ScopeTests {

	private static final String RESOURCE_TXT = "/com/prosoftnearshore/scope/some-resource.txt"; //$NON-NLS-1$

	/**
	 * Illustrates what it takes to open a text file bundled as a resource.
	 * Notice that the only variable we care for inside the {@code try} block is
	 * {@code br}; the others were introduced merely to ensure the
	 * try-with-resources manages them appropriately.
	 *
	 * @throws IOException
	 */

	@Test
	public void testOpenResource() throws IOException {
		try (InputStream stream = this.getClass().getResourceAsStream(RESOURCE_TXT);
			 InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
			 BufferedReader br = new BufferedReader(reader)) {

			assertEquals("resource contents", br.readLine());
		}
	}

	private static final String OTHER_RESOURCE_TXT = "/com/prosoftnearshore/scope/other-resource.txt"; //$NON-NLS-1$

	/**
	 * Illustrates how the same procedure in {@link #testOpenResource} gets much
	 * more cumbersome if we have to handle more than one resource.
	 *
	 * @throws IOException
	 */
	@Test
	public void testOpenTwoResources() throws IOException {
		try (InputStream stream = this.getClass().getResourceAsStream(RESOURCE_TXT);
			 InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
			 BufferedReader br = new BufferedReader(reader);

			 // watch out with copy-n-paste: don't forget to append a '2' to
			 // all references of 'stream', 'reader' and 'br'. I got bit by
			 // that.
			 InputStream stream2 = this.getClass().getResourceAsStream(OTHER_RESOURCE_TXT);
			 InputStreamReader reader2 = new InputStreamReader(stream2, StandardCharsets.UTF_8);
			 BufferedReader br2 = new BufferedReader(reader2)) {

			assertEquals("resource contents", br.readLine());
			assertEquals("other resource contents", br2.readLine());
		}
	}

	/**
	 * So, let's try abstracting away the instantiation of the BufferedReader
	 * we're after in order to read the resource contents. But this time
	 * try-with-resources gets in the way: the resources are always closed by
	 * the time the caller gets them.
	 *
	 * @param filename The name of the bundled resource to open.
	 * @return A {@code BufferedReader} for reading the resource contents (or is
	 * it?).
	 * @throws IOException
	 */
	BufferedReader brokenGetReader(String filename) throws IOException {
		try (InputStream stream = this.getClass().getResourceAsStream(filename);
			 InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
			 BufferedReader br = new BufferedReader(reader)) {
			return br;
		}
	}

	/**
	 * Illustrates that {@link #brokenGetReader} doesn't really work to abstract
	 * away the instantiation of the readers. A shame because the client code
	 * could be cleaned up quite a bit.
	 *
	 * @throws IOException
	 */
	@Test
	public void testBrokenGetReader() throws IOException {
		try (BufferedReader br = brokenGetReader(RESOURCE_TXT);
			 BufferedReader br2 = brokenGetReader(OTHER_RESOURCE_TXT)) {

			try {
				assertEquals("resource contents", br.readLine());
				fail("Huh? We got this far?");
			} catch (IOException e) {
				// Calling br.readLine() should have got us here, as br was
				// already closed
				assertNotNull(e);
			}
		}
	}

	/**
	 * Alas, it seems we have to forego the safety of try-with-resources in
	 * order to make our code less painful to write.
	 *
	 * @param filename The name of the bundled resource to open.
	 * @return A {@code BufferedReader} for reading the resource contents
	 */
	BufferedReader unsafeGetReader(String filename) {
		InputStream stream = this.getClass().getResourceAsStream(filename);
		InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
		return new BufferedReader(reader);
	}

	/**
	 * Illustrates that {@link #unsafeGetReader(String)} at least returns a
	 * working reader, and client code can be nice to write.
	 *
	 * @throws IOException
	 */
	@Test
	public void testUnsafeGetReader() throws IOException {
		try (BufferedReader br = unsafeGetReader(RESOURCE_TXT);
			 BufferedReader br2 = unsafeGetReader(OTHER_RESOURCE_TXT)) {

			assertEquals("resource contents", br.readLine());
			assertEquals("other resource contents", br2.readLine());
		}
	}

	/**
	 * But the time will likely come when we can no longer just close our eyes
	 * to the fact that {@code unsafeGetReader} is, hum, unsafe. Let's make room
	 * for injecting some failures.
	 *
	 * @param filename  The name of the bundled resource to open.
	 * @param newReader A factory method that may or may not succeed at instantiating
	 *                  the {@code BufferedReader}.
	 * @return A {@code BufferedReader} for reading the resource contents (if
	 * all goes well, that is).
	 */
	@SuppressWarnings("resource")
	BufferedReader unsafeGetReader(String filename, NewBufferedReader newReader) {
		InputStream stream = this.getClass().getResourceAsStream(filename);
		InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
		return newReader.apply(reader);
	}

	/**
	 * Illustrates how {@link #unsafeGetReader(String, NewBufferedReader)} does
	 * fail to close resources it creates internally when a failure strikes half
	 * way through. If it weren't for the mocking infrastructure, there's
	 * nothing the client code could do to clean up after.
	 *
	 * @throws IOException
	 */
	@Test
	public void testThatUnsafeDoesNotCloseResources() throws IOException {
		NewBufferedReader evilReaderFactory = mock(NewBufferedReader.class);
		when(evilReaderFactory.apply(Matchers.<Reader>any())).thenThrow(new RuntimeException());

		try (BufferedReader br = unsafeGetReader(RESOURCE_TXT, evilReaderFactory)) {
			fail("Should have thrown!");
		} catch (RuntimeException e) {
			assertThatLeakedTheReaderPassedTo(evilReaderFactory);
		}

	}

	/**
	 * A functional interface for the BufferedReader factory method.
	 */
	interface NewBufferedReader extends Function<Reader, BufferedReader> {
	}

	static final NewBufferedReader newBufferedReader = new NewBufferedReader() {
		@Override
		public BufferedReader apply(Reader t) {
			return new BufferedReader(t);
		}
	};

	private static void assertThatLeakedTheReaderPassedTo(
			final NewBufferedReader mockedReaderFactory) throws IOException {
		ArgumentCaptor<InputStreamReader> readerArgument = ArgumentCaptor
				.forClass(InputStreamReader.class);

		verify(mockedReaderFactory).apply(readerArgument.capture());
		// The reader is still open, so ensure it's closed after
		// assertion is done
		try (InputStreamReader reader = readerArgument.getValue()) {
			assertTrue(readerArgument.getValue().ready());
		}
	}

	/**
	 * Then, let's not give up on doing The Right Thing(tm) and ensure no
	 * resources are leaked, even if something goes wrong. The price we pay,
	 * though, is code that is not so pretty.
	 */

	@SuppressWarnings("resource")
	BufferedReader uglyGetReader(String filename, NewBufferedReader newReader) {
		InputStream stream = null;
		InputStreamReader reader = null;
		try {
			stream = this.getClass().getResourceAsStream(filename);
			reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
			return newReader.apply(reader);
		} catch (RuntimeException e) {
			// Ensure to close any resources that may have been allocated before
			// the failure; do the checks & cleanup in the reverse order they
			// may have been allocated.
			try (InputStream streamToClose = stream;
				 InputStreamReader readerToClose = reader) {
				// no-op, just close the resources
			} catch (IOException e2) {
				e.addSuppressed(e2);
			}
			throw e;
		}
	}

	/**
	 * Illustrates that uglyGetReader closes its resources in the case of
	 * failure. With it, callers' job is much easier; if only uglyGetReader were
	 * not so painful to write...
	 *
	 * @throws IOException
	 */

	@Test
	public void testThatUglyClosesResources() throws IOException {
		NewBufferedReader evilReaderFactory = mock(NewBufferedReader.class);
		when(evilReaderFactory.apply(Matchers.<Reader>any())).thenThrow(new RuntimeException());

		try (BufferedReader br = uglyGetReader(RESOURCE_TXT, evilReaderFactory)) {
			fail("Should have thrown!");
		} catch (RuntimeException e) {
			assertThatIsClosedTheReaderPassedTo(evilReaderFactory);
		}

	}

	private static void assertThatIsClosedTheReaderPassedTo(
			final NewBufferedReader factoryMock) {
		ArgumentCaptor<InputStreamReader> readerArgument = ArgumentCaptor
				.forClass(InputStreamReader.class);

		verify(factoryMock).apply(readerArgument.capture());
		try (InputStreamReader reader = readerArgument.getValue()) {
			assertTrue(readerArgument.getValue().ready());
			fail("Hey, reader should have been closed already!");
		} catch (IOException e2) {
			// All is good: we expected that reader would be closed and that
			// ready() would throw.
		}
	}

	/**
	 * Enter {@link ChainScope}. This class attempts to abstract the chained
	 * initialization pattern in the *GetReader methods, where a series of
	 * {@code AutoCloseable} objects are initialized, each wrapping the
	 * previously initialized resource, with the intention of obtaining a single
	 * final resource to work with. At the same time, through all this
	 * initialization process, any failure or exception should trigger the
	 * closing of the last successfully initialized resource. *
	 * <p>
	 * {@code ChainScope} works by temporarily hooking in (owning) a resource.
	 * The hooked resource is then passed to the initialization of the next
	 * resource, which is then immediately hooked in by the scope. The process
	 * continues initializing and hooking one resource at a time until the final
	 * resource is obtained. At that point, we release that final resource from
	 * the scope so that it can be safely returned by the function (i.e. the
	 * resource survives outside the initializing scope so that it can be
	 * attached into the {@code try-with-resources} block of the caller).
	 * <p>
	 * However, if the scope prematurely closes before releasing the last hooked
	 * resource in the chain (as is the case when an exception is thrown along
	 * the way), then the scope, being itself a resource attached to a
	 * {@code try-with-resources} block, calls into the {@code close()} method
	 * of the last hooked resource, which should be enough to trigger the
	 * release of all the resources in the chain that were successfully
	 * initialized.
	 * <p>
	 * So, in general, exception-safe functions abstracting the
	 * chained-initialization pattern of resources can be easily written as
	 * follows:
	 * <pre>{@code
	 * try (ChainScope s = ChainScope.getNew();) {
	 *   R1 r1 = s.hook(initR1());
	 *   R2 r2 = s.hook(initR2(r1));
	 *   R3 r3 = s.hook(initR3(r2));
	 *   return s.release(r3);
	 * }
	 * }</pre>
	 */
	@SuppressWarnings("resource")
	BufferedReader easyGetReader(String filename, NewBufferedReader newReader) {
		try (ChainScope s = ChainScope.getNew()) {
			InputStream stream = s.hook(this.getClass().getResourceAsStream(filename));
			InputStreamReader reader = s.hook(new InputStreamReader(stream, StandardCharsets.UTF_8));
			BufferedReader br = s.hook(newReader.apply(reader));
			return s.release(br);
		}
	}

	/**
	 * Illustrates that {@link #easyGetReader(String, NewBufferedReader)}
	 * returns a working reader, and client code can be nice to write.
	 *
	 * @throws IOException
	 */
	@Test
	public void testEasyGetReader() throws IOException {
		try (BufferedReader br = easyGetReader(RESOURCE_TXT, newBufferedReader);
			 BufferedReader br2 = easyGetReader(OTHER_RESOURCE_TXT, newBufferedReader)) {

			assertEquals("resource contents", br.readLine());
			assertEquals("other resource contents", br2.readLine());
		}
	}

	/**
	 * Illustrates that easyGetReader closes its resources in the case of
	 * failure. Thus, not only the callers' job is much easier, but also it
	 * wasn't painful to write the function abstracting the initialization of
	 * the resources. Yay!
	 *
	 * @throws IOException
	 */
	@Test
	public void testThatEasyClosesResources() throws IOException {
		NewBufferedReader evilReaderFactory = mock(NewBufferedReader.class);
		when(evilReaderFactory.apply(Matchers.<Reader>any())).thenThrow(new RuntimeException());

		try (BufferedReader br = easyGetReader(RESOURCE_TXT, evilReaderFactory)) {
			fail("Should have thrown!");
		} catch (RuntimeException e) {
			assertThatIsClosedTheReaderPassedTo(evilReaderFactory);
		}

	}

	/**
	 * Now lets say we want to create a class that wraps two or more resources
	 * to be used together. That wrapper itself must be an AutoCloseable which,
	 * quite likely, will be a) initializing the inner resources in its
	 * constructor and b) will have to ensure they're all closed when its close
	 * method is called.
	 * <p>
	 * But it is not enough to close the resources when {@link #close()} is
	 * called: if the constructor can throw, in particular, it should be very
	 * careful of not leaking resources that may have been initialized just
	 * before throwing.
	 */
	class UnsafeReadersWrapper implements Closeable {
		BufferedReader br;
		BufferedReader br2;

		UnsafeReadersWrapper(NewBufferedReader bufferedReaderFactory1,
							 NewBufferedReader bufferedReaderFactory2) {
			this.br = easyGetReader(RESOURCE_TXT, bufferedReaderFactory1);
			this.br2 = easyGetReader(OTHER_RESOURCE_TXT, bufferedReaderFactory2);
		}

		@Override
		public void close() throws IOException {
			try (BufferedReader thisBr = this.br;
				 BufferedReader thisBr2 = this.br2) {
				// no-op, just close the resources
			}
		}
	}

	/**
	 * Illustrates that {@link UnsafeReadersWrapper} fails to close resources it
	 * creates internally when a failure strikes half way through the
	 * constructor.
	 *
	 * @throws IOException
	 */
	@Test
	public void testThatUnsafeWrapperLeaks() throws IOException {
		NewBufferedReader goodReaderFactory = mock(NewBufferedReader.class);
		when(goodReaderFactory.apply(Matchers.<Reader>any())).then(returnNewBufferedReader());
		NewBufferedReader evilReaderFactory = mock(NewBufferedReader.class);
		when(evilReaderFactory.apply(Matchers.<Reader>any())).thenThrow(new RuntimeException());

		try (UnsafeReadersWrapper w = new UnsafeReadersWrapper(
				goodReaderFactory, // init the first resource
				evilReaderFactory)) { // but throw when creating the second
			fail("Should have thrown!");
		} catch (RuntimeException e) {
			assertThatLeakedTheReaderPassedTo(goodReaderFactory);
		}

	}

	private static Answer<BufferedReader> returnNewBufferedReader() {
		return new Answer<BufferedReader>() {

			@Override
			public BufferedReader answer(
					@Nullable InvocationOnMock invocation) {
				return new BufferedReader(requireNotNull(invocation)
						.getArgumentAt(0, Reader.class));
			}

		};
	}

	static <T> T requireNotNull(@Nullable T obj) {
		if (obj == null)
			throw new NullPointerException();
		return obj;
	}

	/**
	 * Then, let's see what it takes to write a wrapper with a robust
	 * constructor that does not leaks any resources even if exceptions strike
	 * at any point... Yuck!
	 */
	class UglyReadersWrapper implements Closeable {
		BufferedReader br;
		BufferedReader br2;

		UglyReadersWrapper(NewBufferedReader bufferedReaderFactory1,
						   NewBufferedReader bufferedReaderFactory2) {
			try {
				this.br = easyGetReader(RESOURCE_TXT, bufferedReaderFactory1);
				try {
					this.br2 = easyGetReader(OTHER_RESOURCE_TXT, bufferedReaderFactory2);
				} catch (Exception e) {
					try {
						this.br2.close();
					} catch (Exception e2) {
						e.addSuppressed(e2);
					}
					throw e;

				}
			} catch (Exception e) {
				try {
					this.br.close();
				} catch (Exception e2) {
					e.addSuppressed(e2);
				}
				throw e;
			}

		}

		@Override
		public void close() throws IOException {
			try (BufferedReader thisBr = this.br;
				 BufferedReader thisBr2 = this.br2) {
				// no-op, just close the resources
			}
		}

	}

	/**
	 * Illustrates that {@link UglyReadersWrapper} closes the resources it
	 * creates internally even when a failure strikes half way through the
	 * constructor. However, properly coding such constructor was tedious and
	 * this approach is quite error-prone in general.
	 *
	 * @throws IOException
	 */

	@Test
	public void testThatUglyWrapperClosesResources() throws IOException {
		NewBufferedReader goodReaderFactory = mock(NewBufferedReader.class);
		when(goodReaderFactory.apply(Matchers.<Reader>any())).then(returnNewBufferedReader());
		NewBufferedReader evilReaderFactory = mock(NewBufferedReader.class);
		when(evilReaderFactory.apply(Matchers.<Reader>any())).thenThrow(new RuntimeException());

		try (UglyReadersWrapper w = new UglyReadersWrapper(
				goodReaderFactory, // init the first resource
				evilReaderFactory)) { // but throw when creating the second
			fail("Should have thrown!");
		} catch (RuntimeException e) {
			assertThatIsClosedTheReaderPassedTo(goodReaderFactory);
		}

	}

	/**
	 * Well, so here comes {@link CollectScope} and {@link WrapperScope} to the
	 * rescue: the first one is meant to assist in the writing of the robust
	 * constructor that will never leak resources even in the presence of
	 * exceptions.
	 * <p>
	 * The second one is a convenience {@code AutoCloseable} that will close all
	 * the collected resources when it's called, so that all we need to do in
	 * the {@link #close()} of our wrapper is close it and we'll be done with
	 * our wrapped resources.
	 */
	class EasyReadersWrapper implements Closeable {
		final BufferedReader br;
		final BufferedReader br2;
		final WrapperScope resources;

		EasyReadersWrapper(NewBufferedReader bufferedReaderFactory1,
						   NewBufferedReader bufferedReaderFactory2) {
			try (CollectScope s = CollectScope.getNew()) {
				this.br = s.add(easyGetReader(RESOURCE_TXT, bufferedReaderFactory1));
				this.br2 = s.add(easyGetReader(OTHER_RESOURCE_TXT, bufferedReaderFactory2));
				this.resources = s.release();
			}
		}

		@Override
		public void close() throws CloseException {
			this.resources.close();

		}
	}

	/**
	 * Illustrates that {@link EasyReadersWrapper} is a robust wrapper of
	 * resources that does not leak even when a failure strikes half way through
	 * the constructor. Once again, scopes made such abstraction easy to write.
	 */
	@Test
	public void testThatEasyWrapperClosesResources() {
		NewBufferedReader goodReaderFactory = mock(NewBufferedReader.class);
		when(goodReaderFactory.apply(Matchers.<Reader>any())).then(returnNewBufferedReader());
		NewBufferedReader evilReaderFactory = mock(NewBufferedReader.class);
		when(evilReaderFactory.apply(Matchers.<Reader>any())).thenThrow(new RuntimeException());

		try (EasyReadersWrapper w = new EasyReadersWrapper(
				goodReaderFactory, // init the first resource
				evilReaderFactory)) { // but throw when creating the second
			fail("Should have thrown!");
		} catch (RuntimeException e) {
			assertThatIsClosedTheReaderPassedTo(goodReaderFactory);
		}
	}

}
