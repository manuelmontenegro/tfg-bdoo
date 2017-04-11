package tfg.tfg;

public class Atributo {

	private String nombre;
	private String tipo;
	private boolean basico;
	private String claseConstructora;
	private boolean multi;
	
	public Atributo(String nombre, String tipo, boolean basico, boolean multi) {
		super();
		this.nombre = nombre;
		this.tipo = tipo;
		this.basico = basico;
		this.multi=multi;
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

	public boolean isBasico() {
		return basico;
	}
	public boolean isMulti() {
		return multi;
	}

	public void setBasico(boolean basico) {
		this.basico = basico;
	}

	public String getClaseConstructora() {
		return claseConstructora;
	}

	public void setClaseConstructora(String claseConstructora) {
		this.claseConstructora = claseConstructora;
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
	
}
