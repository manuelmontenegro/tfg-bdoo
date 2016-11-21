package prueba;

import java.util.Date;

import prueba2.Plato;
import tfg.tfg.LibreriaBBDD;

public class Empleado {
	private String DNI;
	private String nombre;
	private String direccion;
	private int telefono;
	private String sexo;
	private String tipo;
	private String contrasenya;
	
	public Empleado(){}
	public Empleado(String dNI, String nombre, String direccion, int telefono,
			String sexo, String tipo, String contrasenya) {
		
		this.DNI = dNI;
		this.nombre = nombre;
		this.direccion = direccion;
		this.telefono = telefono;
		this.sexo = sexo;
		this.tipo = tipo;
		this.contrasenya = contrasenya;
	}
	
	public Empleado(String dni, String contrasenya){
		this.contrasenya = contrasenya;
		this.DNI = dni;
	}
	public Empleado(int telefono){
		this.telefono = telefono;
	}

	public String getDNI() {
		return DNI;
	}

	public void setDNI(String dNI) {
		DNI = dNI;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getDireccion() {
		return direccion;
	}

	public void setDireccion(String direccion) {
		this.direccion = direccion;
	}

	public int getTelefono() {
		return telefono;
	}

	public void setTelefono(int telefono) {
		this.telefono = telefono;
	}

	public String getSexo() {
		return sexo;
	}

	public void setSexo(String sexo) {
		this.sexo = sexo;
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public String getContrasenya() {
		return contrasenya;
	}

	public void setContrasenya(String contrasenya) {
		this.contrasenya = contrasenya;
	}
	
	public String toString(){
		return "DNI: " + this.DNI + "\n" + "Nombre: " + this.nombre + "\n";
	}
	
	public static void main(String[] argv){
//		Empleado empleado = new Empleado("0234","vic","ramiro",24234,"hombre","cocinero","123");
//		Empleado emp = new Empleado("32","vic");
//		Plato p = new Plato("lentejas","aceite y tomate");
//		LibreriaBBDD lib = new LibreriaBBDD("tfg","tfg","tfg");
//		lib.guardar(emp);
//		lib.guardar(empleado);
//		lib.guardar(p);
//		
	}
	
	
}
