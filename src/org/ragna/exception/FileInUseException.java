package org.ragna.exception;

import java.io.IOException;

/** Thrown to indicate that a given file description belongs to a file which
 * is currently in use by some other semantical section. 
 */
public class FileInUseException extends IOException {

	public FileInUseException () {
	}

	public FileInUseException (String message) {
		super(message);
	}

	public FileInUseException (Throwable cause) {
		super(cause);
	}

	public FileInUseException (String message, Throwable cause) {
		super(message, cause);
	}

}
