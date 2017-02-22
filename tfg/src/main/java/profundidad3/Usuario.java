package profundidad3;

public class Usuario {

	private String nombre;
	private Direccion direccion;

	public Usuario(String nombre) {
		super();
		this.nombre = nombre;
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
		return direccion;
	}

	public void setDireccion(Direccion direccion) {
		this.direccion = direccion;
	}
	
	
}
