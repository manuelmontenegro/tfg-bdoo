package tfg.tfg;

import java.util.HashMap;
import java.util.Map;

public class Identificador {
	
	private Integer identificador;
	private String clase;
	
	
	public Identificador(Integer identificador, String clas) {
		super();
		this.identificador = identificador;
		this.clase = clas;
	}


	public Integer getIdentificador() {
		return identificador;
	}


	public void setIdentificador(Integer identificador) {
		this.identificador = identificador;
	}


	public String getClase() {
		return clase;
	}


	public void setClase(String clase) {
		this.clase = clase;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((clase == null) ? 0 : clase.hashCode());
		result = prime * result + ((identificador == null) ? 0 : identificador.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Identificador other = (Identificador) obj;
		if (clase == null) {
			if (other.clase != null)
				return false;
		} else if (!clase.equals(other.clase))
			return false;
		if (identificador == null) {
			if (other.identificador != null)
				return false;
		} else if (!identificador.equals(other.identificador))
			return false;
		return true;
	}
	
	public static void main(String[] argv) {
		
		Integer i=123;
		String s="pa";
		
		Identificador i1=new Identificador(i,s);
		Identificador i2=new Identificador(i,s);
		
		
		Map<Identificador, Object> m=new HashMap<Identificador, Object>();
		
		m.put(i1, new String());
		System.out.println(m.containsKey(i2));
		
		
	}

}
