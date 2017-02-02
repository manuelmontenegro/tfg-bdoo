package prueba2;

public class Plato {
	private String nombre;
	private String tipo;
	private String ingredientes;
	
	public Plato(String nombre, String tipo, String ingredientes) {
		this.nombre = nombre;
		this.tipo = tipo;
		this.ingredientes = ingredientes;
	}
	
	public Plato(String nombre, String ingredientes) {
		this.nombre = nombre;
		this.ingredientes = ingredientes;
	}

	public String getNombre() {
		return nombre;
	}
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	public String getTipo() {
		return tipo;
	}
	public void setTipo(String tipo) {
		this.tipo = tipo;
	}
	public String getIngredientes() {
		return ingredientes;
	}
	public void setIngredientes(String ingredientes) {
		this.ingredientes = ingredientes;
	}

}
