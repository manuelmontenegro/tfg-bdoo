package oodb.exception;

/**
 * Exception thrown when an object can't be found for deletion.
 */
public class NonExistentObject extends Exception {

	public NonExistentObject() {
		super("The object couldn't be found for deletion.");
	}
}
