package pruebaList;

import java.util.ArrayList;
import java.util.List;

public class Usuario {
	
	private String nombre;
	private int edad;
	private List<Direccion>direcciones;
	
	public Usuario(){
		
	}
	
	public Usuario(String nombre, int edad){
		this.nombre=nombre;
		this.edad=edad;
		this.direcciones=new ArrayList<Direccion>();	
	}
	
	public void addDireccion(Direccion direcion){
		this.direcciones.add(direcion);
	}

}
