package oodb.exception;

public class OOBDLibraryException extends RuntimeException {

	private Exception cause;
	private String message;

	public OOBDLibraryException(Exception e) {
		this.cause = e;
		this.message = null;
	}

	public OOBDLibraryException(Exception e, String m) {
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
