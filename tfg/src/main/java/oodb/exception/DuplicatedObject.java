package oodb.exception;

/**
 * Exception thrown when an object already has been inserted before.
 */
public class DuplicatedObject extends Exception {

	public DuplicatedObject() {
		super("The object has already been inserted before.");
	}
}
