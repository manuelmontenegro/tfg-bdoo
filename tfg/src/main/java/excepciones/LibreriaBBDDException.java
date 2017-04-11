package excepciones;

public class LibreriaBBDDException extends RuntimeException {
	
	private Exception cause;
	private String message;

	public LibreriaBBDDException(Exception e) {
		this.cause = e;
		this.message = null;
	}

	public LibreriaBBDDException(Exception e, String m) {
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
