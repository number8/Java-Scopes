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
