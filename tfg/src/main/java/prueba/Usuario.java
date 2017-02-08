package prueba;

public class Usuario {

	private String nombre;
	private int edad;
	private Direccion domicilio;
	private Usuario compañero;

	public Usuario(String nombre, int edad) {
		super();
		this.nombre = nombre;
		this.edad=edad;
	}
	
	public Usuario() {
		super();
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public Direccion getDireccion() {
		return domicilio;
	}

	public void setDireccion(Direccion direccion) {
		this.domicilio = direccion;
	}
	
	public int getEdad() {
		return edad;
	}

	public void setEdad(int edad) {
		this.edad = edad;
	}

	public Direccion getDomicilio() {
		return domicilio;
	}

	public void setDomicilio(Direccion domicilio) {
		this.domicilio = domicilio;
	}

	public Usuario getCompañero() {
		return compañero;
	}

	public void setCompañero(Usuario compañero) {
		this.compañero = compañero;
	}
	
	
}
