package prueba;

public class Usuario {

	private String nombre;
	private int edad;
	private Direccion domicilio;
	
	public Usuario(String nombre, Direccion direccion) {
		super();
		this.nombre = nombre;
		edad=22;
		this.domicilio = direccion;
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
	
	
	
	
}
