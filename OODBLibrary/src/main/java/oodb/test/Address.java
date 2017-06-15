package oodb.test;

public class Address {
	
	private String street;
	private int number;
	
	public Address(String street, int number) {
		super();
		this.street = street;
		this.number = number;
	}
	
	public Address(){
		
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

}
