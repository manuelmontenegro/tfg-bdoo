package prueba;

public class Ciclo {
	
	private String nombre;
	private int edad;
	private Ciclo ci;
	
	public Ciclo(String nombre, int edad){
		this.nombre=nombre;
		this.edad=edad;
	}
	
	public void setCiclo(Ciclo ciclo){
		this.ci=ciclo;
	}

}
