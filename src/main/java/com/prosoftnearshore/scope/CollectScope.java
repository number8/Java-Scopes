package com.prosoftnearshore.scope;

public class CollectScope implements Scope {

	public static CollectScope getNew() {
		return new CollectScope();
	}

	public <T extends AutoCloseable> T add(T resource) {
		return this.wrapper.add(resource);
	}

	@Override
	public void close() throws RuntimeException {
		this.wrapper.close();
	}

	public WrapperScope release() {
		WrapperScope r = WrapperScope.getNew();
		this.wrapper.swap(r);
		return r;
	}

	//
	// Private Members
	//

	private final WrapperScope wrapper = WrapperScope.getNew();

	private CollectScope() {
		// hide constructor from API
	}

}
