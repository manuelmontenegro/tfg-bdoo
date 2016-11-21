package tfg.tfg;

public class Atributo {

	private String nombre;
	private String tipo;
	
	public Atributo(String nombre, String tipo) {
		super();
		this.nombre = nombre;
		this.tipo = tipo;
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
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Atributo) {
			Atributo tmpPersona = (Atributo) obj;
			if (this.tipo.equals(tmpPersona.tipo) && this.nombre.equals(tmpPersona.nombre)) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
	public static void main(String[] argv) {
		Atributo a = new Atributo("paco","pao");
		Atributo b = new Atributo("paco","paco");
		
		if(a.equals(b))
			System.out.println("Iguales");
		else
			System.out.println("pues no");
		
	}
	
	
	
}
