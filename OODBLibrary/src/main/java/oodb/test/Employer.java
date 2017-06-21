package oodb.test;

public class Employer {
	private String id;
	private String name;
	private String address;
	private int phone;
	private String gender;
	private String type;
	private String password;
	
	public Employer(){}
	public Employer(String id, String name, String address, int phone,
			String gender, String type, String password) {
		
		this.id = id;
		this.name = name;
		this.address = address;
		this.phone = phone;
		this.gender = gender;
		this.type = type;
		this.password = password;
	}
	
	public Employer(String dni, String contrasenya){
		this.password = contrasenya;
		this.id = dni;
	}
	public Employer(int phone){
		this.phone = phone;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setAddress(String address) {
		this.name = address;
	}

	public String getAddress() {
		return address;
	}

	public int getPhone() {
		return phone;
	}

	public void setPhone(int phone) {
		this.phone = phone;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	public String toString(){
		return "ID: " + this.id + "\n" + "Name: " + this.name + "\n";
	}
		
}
