package oodb.library;

/**
 * Represents a Field object with extra data that helps in the implementation of the library methods.
 */
class Attribute {

	private String name;
	private String type;
	private boolean basic;
	private String constructorClass;
	private boolean multivalued;

	Attribute(String name, String type, boolean basic, boolean multivalued) {
		super();
		this.name = name;
		this.type = type;
		this.basic = basic;
		this.multivalued = multivalued;
	}

	String getName() {
		return name;
	}

	void setName(String nombre) {
		this.name = nombre;
	}

	String getType() {
		return type;
	}

	void setType(String type) {
		this.type = type;
	}

	boolean isBasic() {
		return basic;
	}

	void setBasic(boolean basic) {
		this.basic = basic;
	}

	boolean isMultivalued() {
		return multivalued;
	}

	String getConstructorClass() {
		return constructorClass;
	}

	void setConstructorClass(String claseConstructora) {
		this.constructorClass = claseConstructora;
	}

	public boolean equals(Object obj) {
		if (obj instanceof Attribute) {
			Attribute tmp = (Attribute) obj;
			if (this.type.equals(tmp.type) && this.name.equals(tmp.name)) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

}
