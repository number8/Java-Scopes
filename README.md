# Java Resource Scopes

Consider what it takes in Java to open a text file bundled as a resource in the jar:
```java
try (InputStream stream = this.getClass().getResourceAsStream(RESOURCE_TXT);
     InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
     BufferedReader br = new BufferedReader(reader);) {

	// use the resource
}
```
Sometimes, we'd like to abstract such patterns of instantiaton of resources, like when we want to manipulate more than one at a time:
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
BufferedReader getReader(String filename, NewBufferedReader newReader) {
	try (ChainScope s = ChainScope.getNew()) {
		InputStream stream = s.hook(this.getClass().getResourceAsStream(filename));
		InputStreamReader reader = s.hook(new InputStreamReader(stream, StandardCharsets.UTF_8));
		BufferedReader br = s.hook(newReader.apply(reader));
		return s.release(br);
	}
}
```
Note that `try-with-resources` could not have been used directly on `stream`, `reader` or `br` above as that would have caused them to be closed upon return from `getReader`. Instead, it is applied to the chain scope instance, which tracks the resources as they're created and only closes them if the scope exits prematurely due to an exception; otherwise, if the function reaches the execution of the `release()` call, then the scope will not close any resource and the function will return a working buffered reader to the caller.
