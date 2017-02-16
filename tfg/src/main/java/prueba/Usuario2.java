package prueba;

public class Usuario2 {

	private String nombre;
	private Direccion2 direccion;

	public Usuario2(String nombre) {
		super();
		this.nombre = nombre;
	}
	
	public Usuario2() {
		super();
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public Direccion2 getDireccion() {
		return direccion;
	}

	public void setDireccion(Direccion2 direccion) {
		this.direccion = direccion;
	}
	
	
}
