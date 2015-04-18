package com.prosoftnearshore.scope;

import java.util.ArrayDeque;
import java.util.Queue;

public class WrapperScope implements Scope {

	/**
	 * Construct and return a new instance of {@link WrapperScope}.
	 * 
	 * @return a new instance of {@code WrapperScope}.
	 */
	public static WrapperScope getNew() {
		return new WrapperScope();
	}

	public <T extends AutoCloseable> T add(T resource) {
		this.resources.add(resource);
		return resource;
	}

	@Override
	public void close() throws RuntimeException {
		if (this.resources.isEmpty())
			return;

		try (AutoCloseable r = this.resources.remove()) {
			this.close();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new CloseException(e);
		}
	}

	//
	// Package-Private Members
	//
	void swap(WrapperScope other) {
		Queue<AutoCloseable> t = this.resources;
		this.resources = other.resources;
		other.resources = t;
	}

	//
	// Private Members
	//
	private Queue<AutoCloseable> resources = new ArrayDeque<>();

	private WrapperScope() {
		// hide constructor from API
	}

}
