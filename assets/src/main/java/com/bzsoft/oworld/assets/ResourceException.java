package com.bzsoft.oworld.assets;

public class ResourceException extends Exception {

	private static final long serialVersionUID = 1L;

	public ResourceException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public ResourceException(String msg) {
		this(msg, null);
	}
}
