package pruebaList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Usuario {
	
	private String nombre;
	private int edad;
	private Usuario usuario;
	
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
	}
	
	public void addDireccionL(Direccion direcion){
		if(this.direccionesL==null)
			this.direccionesL=new ArrayList<Direccion>();
		this.direccionesL.add(direcion);
	}
	public void setDireccionL(Direccion direccion, int pos){
		if(this.direccionesL==null)
			this.direccionesL=new ArrayList<Direccion>();
		this.direccionesL.set(pos, direccion);
	}
	
	
	public void addGustoL(String gusto){
		if(this.gustosL==null)
			this.gustosL=new ArrayList<String>();	
		this.gustosL.add(gusto);
	}
	public void setGustoL(String gusto, int pos){
		if(this.gustosL==null)
			this.gustosL=new ArrayList<String>();	
		this.gustosL.set(pos, gusto);
	}
	
	
	public void addNumeroL(int numero){
		if(this.numerosL==null)
			this.numerosL=new ArrayList<Integer>();	
		this.numerosL.add(numero);
	}
	public void setNumeroL(int numero, int pos){
		if(this.numerosL==null)
			this.numerosL=new ArrayList<Integer>();
		this.numerosL.set(pos, numero);
	}
	
	
	public void addUsuarioL(Usuario usuario){
		if(this.usuariosL==null)
			this.usuariosL=new ArrayList<Usuario>();	
		this.usuariosL.add(usuario);
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

	public List<Direccion> getDireccionesL() {
		return direccionesL;
	}

	public void setDireccionesL(List<Direccion> direccionesL) {
		this.direccionesL = direccionesL;
	}

	public List<String> getGustosL() {
		return gustosL;
	}

	public void setGustosL(List<String> gustosL) {
		this.gustosL = gustosL;
	}

	public List<Integer> getNumerosL() {
		return numerosL;
	}

	public void setNumerosL(List<Integer> numerosL) {
		this.numerosL = numerosL;
	}

	public List<Usuario> getUsuariosL() {
		return usuariosL;
	}

	public void setUsuariosL(List<Usuario> usuariosL) {
		this.usuariosL = usuariosL;
	}

	public Set<Usuario> getUsuariosS() {
		return usuariosS;
	}

	public void setUsuariosS(Set<Usuario> usuariosS) {
		this.usuariosS = usuariosS;
	}

	public Set<Direccion> getDireccionesS() {
		return direccionesS;
	}

	public Set<String> getGustosS() {
		return gustosS;
	}

	public Set<Integer> getNumerosS() {
		return numerosS;
	}

	public void setUsuarioL(Usuario usuario, int pos){
		if(this.usuariosL==null)
			this.usuariosL=new ArrayList<Usuario>();
		this.usuariosL.set(pos, usuario);
	}

	
	
	
	public void addDireccionS(Direccion direcion){
		if(this.direccionesS==null)
			this.direccionesS=new HashSet<Direccion>();
		this.direccionesS.add(direcion);
	}
	public void setDireccionesS(Set<Direccion> direcciones){
		this.direccionesS=direcciones;
	}
	
	
	public void addGustoS(String gusto){
		if(this.gustosS==null)
			this.gustosS=new HashSet<String>();	
		this.gustosS.add(gusto);
	}
	public void setGustosS(Set<String> gustos){
		this.gustosS=gustos;
	}
	
	public void addNumeroS(int numero){
		if(this.numerosS==null)
			this.numerosS=new HashSet<Integer>();	
		this.numerosS.add(numero);
	}
	public void setNumerosS(Set<Integer> numeros){
		this.numerosS=numeros;
	}
	
	
	
	public void addUsuarioS(Usuario usuario){
		if(this.usuariosS==null)
			this.usuariosS=new HashSet<Usuario>();
		this.usuariosS.add(usuario);
	}
	public void setUsuarioS(Set<Usuario> usuarios){
		this.usuariosS=usuarios;
	}

	public void showGustos() {
		System.out.println("start");
		for(String s: gustosS)
			System.out.println(s);
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

}
