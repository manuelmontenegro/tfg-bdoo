package oobd.exception;

public class NonExistentObject extends Exception {

	public NonExistentObject() {
		super("The object couldn't be found for deletion.");
	}
}
