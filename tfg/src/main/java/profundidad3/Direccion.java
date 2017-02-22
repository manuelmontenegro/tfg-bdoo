package profundidad3;

public class Direccion {
	
	private String calle;
	
	private Numero numero;
	private Ciudad ciudad;
	
	public Direccion(String calle) {
		super();
		this.calle = calle;
	}
	
	public Direccion(){
		
	}

	public String getCalle() {
		return calle;
	}

	public void setCalle(String calle) {
		this.calle = calle;
	}

	public Numero getNumero() {
		return numero;
	}

	public void setNumero(Numero numero) {
		this.numero = numero;
	}
	
	public void setCiudad(Ciudad ciudad) {
		this.ciudad = ciudad;
	}
	
	
	
	
	

}
