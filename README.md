# Java Resource Scopes

Consider what it takes in Java to open a text file bundled as a resource in the jar:
```java
try (InputStream stream = this.getClass().getResourceAsStream(RESOURCE_TXT);
     InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
     BufferedReader br = new BufferedReader(reader);) {

	// use the resource
}
```
Sometimes, we'd like to abstract such patterns of instantiation of resources, like when we want to manipulate more than one at a time:
```java
try (BufferedReader br = getReader(RESOURCE_TXT);
     BufferedReader br2 = getReader(OTHER_RESOURCE_TXT);) {

	// use the resources
}
```
The problem is that writing `getReader` properly is not easy because of the care one must be have of not leaving open any of the resources that could have been requested if something goes wrong (i.e. an exception is thrown at any point during the execution of the function. Thus, `getReader` would have to be coded like this:
```java
BufferedReader getReader(String filename, NewBufferedReader newReader) {
	InputStream stream = null;
	InputStreamReader reader = null;
	BufferedReader br = null;
	try {
		stream = this.getClass().getResourceAsStream(filename);
		reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
		br = newReader.apply(reader);
		return br;
	} catch (Exception e) {
		// Ensure to close any resources that may have been allocated before
		// the failure; do the checks & cleanup in the reverse order they
		// may have been allocated, and watch out for failures from calls to
		// close()!
		if (br != null)
			try {
				br.close();
			} catch (IOException e2) {
				e.addSuppressed(e2);
			}
		else if (reader != null)
			try {
				reader.close();
			} catch (IOException e2) {
				e.addSuppressed(e2);
			}
		else if (stream != null)
			try {
				stream.close();
			} catch (IOException e2) {
				e.addSuppressed(e2);
			}
		throw e;
	}
}
```
The purpose of this library is to provide abstractions that greatly simplify the proper coding functions like `getReader`. So, for example, with the use of the `ChainScope` class, `getReader` could be written as follows:
```java
BufferedReader newReader(String filename) {
  try (ChainScope s = ChainScope.getNew()) {
    InputStream stream = s.hook(this.getClass().getResourceAsStream(filename));
    InputStreamReader reader = s.hook(new InputStreamReader(stream, StandardCharsets.UTF_8));
    BufferedReader br = s.hook(new BufferedReader(reader));
    return s.release(br);
  }
}
```
Note that `try-with-resources` could not have been used directly on `stream`, `reader` or `br` above as that would have caused them to be closed upon return from `getReader`. Instead, it is applied to the chain scope instance, which tracks the resources as they're created and only closes them if the scope exits prematurely due to an exception; otherwise, if the function reaches the execution of the `release()` call, then the scope will not close any resource and the function will return a working buffered reader to the caller.

Other scenario where this library aims to help is when coding a new `AutoCloseable` object that wraps two or more resources. They usually get complex at two places: their constructor, and somewhat less in their implementation of `close()`.

In the case of the constructor, such a wrapper will usually allocate each resource, one at a time. Here again, if an exception strikes, all resources should have to be immediately closed. On the other hand, if the construction succeeds, we'll want to ensure that they are all closed when the wrapper's `close()` method is called.

This library provides the `CollectScope` and the `WrapperScope` objects, with which an object wrapping two buffered readers could look like this:
```java
class ReadersWrapper implements Closeable {
	final BufferedReader br;
	final BufferedReader br2;
	final WrapperScope resources;

	ReadersWrapper(NewBufferedReader bufferedReaderFactory1,
			NewBufferedReader bufferedReaderFactory2) {
		try (CollectScope s = CollectScope.getNew()) {
			this.br = s.add(getReader(RESOURCE_TXT));
			this.br2 = s.add(getReader(OTHER_RESOURCE_TXT));
			this.resources = s.release();
		}
	}

	@Override
	public void close() throws CloseException {
		this.resources.close();
	}
}
```
Just like `ChainScope`, `CollectScope` greatly simplifies the initialization of several resources but this time in the context of writing the constructor for a wrapper of resources. 

The later also returns a `WrapperScope` when its `release()` method is called, which will conveniently close all the collected resources when its `close()` method is called. That's precisely what `ReadersWrapper` needs for implementing its own `close()` method. As an added bonus, the `close()` is guaranteed to be idempotent without any additional effort from the author of `ReadersWrapper`.

How would `ReadersWrapper` had looked like without the help of the scope classes? That is left as an exercise to the reader ;-) ... [Or check the test cases for the gory details](src/test/java/com/prosoftnearshore/scope/ScopeTests.java).

- - -

Copyright 2015 Prosoft, LLC. [Licensed under the Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
