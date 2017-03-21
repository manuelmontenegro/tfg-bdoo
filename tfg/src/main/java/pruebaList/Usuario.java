package pruebaList;

import java.util.ArrayList;
import java.util.List;

public class Usuario {
	
	private String nombre;
	private int edad;
	private List<Direccion>direcciones;
	private List<String> gustos;
	private List<Integer> numeros;
	
	public Usuario(){
		
	}
	
	public Usuario(String nombre, int edad){
		this.nombre=nombre;
		this.edad=edad;
		this.direcciones=new ArrayList<Direccion>();
		this.gustos=new ArrayList<String>();	
		this.numeros=new ArrayList<Integer>();	
	}
	
	public void addDireccion(Direccion direcion){
		this.direcciones.add(direcion);
	}
	
	public void addGusto(String gusto){
		this.gustos.add(gusto);
	}
	
	public void addNumero(int numero){
		this.numeros.add(numero);
	}

}
