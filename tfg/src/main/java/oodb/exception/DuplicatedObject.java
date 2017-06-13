package oodb.exception;

public class DuplicatedObject extends Exception {

	public DuplicatedObject() {
		super("The object has already been inserted before.");
	}
}
