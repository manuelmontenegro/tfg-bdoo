package oodb.exception;

/**
 * Library exception wrapper.
 */
public class OODBLibraryException extends RuntimeException {

	private Exception cause;
	private String message;

	public OODBLibraryException(Exception e) {
		this.cause = e;
		this.message = null;
	}

	public OODBLibraryException(Exception e, String m) {
		this.cause = e;
		this.message = m;
	}

	public Exception getCause() {
		return this.cause;
	}

	public String getMessage() {
		if (message == null)
			return this.cause.getMessage();
		else
			return this.message;
	}
}
