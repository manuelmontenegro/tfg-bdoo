package pruebaList;

import java.util.ArrayList;
import java.util.List;

public class Usuario {
	
	private String nombre;
	private int edad;
	private List<Direccion>direcciones;
	private List<String> gustos;
	private List<Integer> numeros;
	private List<Usuario> usuarios;
	
	public Usuario(){
		
	}
	
	public Usuario(String nombre, int edad){
		this.nombre=nombre;
		this.edad=edad;
		this.direcciones=new ArrayList<Direccion>();
		this.gustos=new ArrayList<String>();	
		this.numeros=new ArrayList<Integer>();	
		this.usuarios=new ArrayList<Usuario>();	
	}
	
	public void addDireccion(Direccion direcion){
		this.direcciones.add(direcion);
	}
	
	
	public void addGusto(String gusto){
		this.gustos.add(gusto);
	}
	public void setGusto(String gusto, int pos){
		this.gustos.set(pos, gusto);
	}
	
	
	public void addNumero(int numero){
		this.numeros.add(numero);
	}
	public void setNumero(int numero, int pos){
		this.numeros.set(pos, numero);
	}
	
	
	public void addUsuario(Usuario usuario){
		this.usuarios.add(usuario);
	}
	public void setUsuario(Usuario usuario, int pos){
		this.usuarios.set(pos, usuario);
	}

	

}
