package pruebaList2;

import java.util.ArrayList;
import java.util.List;

public class Usuario {
	
	private String nombre;
	private int edad;
	
	private List<String> direcciones;

	
	public Usuario(){}
	
	public Usuario(String nombre, int edad){
		this.nombre=nombre;
		this.edad=edad;
		this.direcciones=new ArrayList<String>();
	}
	
	public void addDireccion(String direcion){
		this.direcciones.add(direcion);
	}
	
	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public int getEdad() {
		return edad;
	}

	public void setEdad(int edad) {
		this.edad = edad;
	}

	public List<String> getDirecciones() {
		return direcciones;
	}

}
