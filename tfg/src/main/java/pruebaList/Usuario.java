package pruebaList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Usuario {
	
	private String nombre;
	private int edad;
	
	private List<Direccion>direccionesL;
	private List<String> gustosL;
	private List<Integer> numerosL;
	private List<Usuario> usuariosL;
	
	private Set<Direccion>direccionesS;
	private Set<String> gustosS;
	private Set<Integer> numerosS;
	private Set<Usuario> usuariosS;

	
	public Usuario(){
		
	}
	
	public Usuario(String nombre, int edad){
		this.nombre=nombre;
		this.edad=edad;
		

		this.direccionesL=new ArrayList<Direccion>();
		this.gustosL=new ArrayList<String>();	
		this.numerosL=new ArrayList<Integer>();	
		this.usuariosL=new ArrayList<Usuario>();	
		
		this.direccionesS=new HashSet<Direccion>();
		this.gustosS=new HashSet<String>();	
		this.numerosS=new HashSet<Integer>();	
				
		this.usuariosS=new HashSet<Usuario>();
	}
	
	public void addDireccionL(Direccion direcion){
		this.direccionesL.add(direcion);
	}
	public void setDireccionL(Direccion direccion, int pos){
		this.direccionesL.set(pos, direccion);
	}
	
	
	public void addGustoL(String gusto){
		this.gustosL.add(gusto);
	}
	public void setGustoL(String gusto, int pos){
		this.gustosL.set(pos, gusto);
	}
	
	
	public void addNumeroL(int numero){
		this.numerosL.add(numero);
	}
	public void setNumeroL(int numero, int pos){
		this.numerosL.set(pos, numero);
	}
	
	
	public void addUsuarioL(Usuario usuario){
		this.usuariosL.add(usuario);
	}
	public void setUsuarioL(Usuario usuario, int pos){
		this.usuariosL.set(pos, usuario);
	}

	
	
	
	public void addDireccionS(Direccion direcion){
		this.direccionesS.add(direcion);
	}
	public void setDireccionesS(Set<Direccion> direcciones){
		this.direccionesS=direcciones;
	}
	
	
	public void addGustoS(String gusto){
		this.gustosS.add(gusto);
	}
	public void setGustosS(Set<String> gustos){
		this.gustosS=gustos;
	}
	
	public void addNumeroS(int numero){
		this.numerosS.add(numero);
	}
	public void setNumerosS(Set<Integer> numeros){
		this.numerosS=numeros;
	}
	
	
	
	public void addUsuarioS(Usuario usuario){
		this.usuariosS.add(usuario);
	}
	public void setUsuarioS(Set<Usuario> usuarios){
		this.usuariosS=usuarios;
	}

}
