package org.ragna.exception;

public class UnverifiedPasswordException extends Exception {

	public UnverifiedPasswordException() {
	}

	public UnverifiedPasswordException(String message) {
		super(message);
	}

	public UnverifiedPasswordException(Throwable cause) {
		super(cause);
	}

	public UnverifiedPasswordException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnverifiedPasswordException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	
	
}
