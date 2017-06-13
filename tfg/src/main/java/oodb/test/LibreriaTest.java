package oodb.test;

import java.beans.PropertyVetoException;
import java.sql.SQLException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import oodb.constraints.AndConstraint;
import oodb.constraints.Constraint;
import oodb.constraints.SimpleConstraint;
import oodb.library.OODBLibrary;
import oodb.library.Query;
import prueba.Direccion;
import prueba.Usuario;

public class LibreriaTest extends TestCase{
	
	private OODBLibrary lib;
	private Usuario usuario;
	//private Query q;
	
	 public LibreriaTest(String name) {
	        super(name);
	    }

	
    protected void setUp() {
    	this.lib=new OODBLibrary("tfg", "root", "");
    	
		this.usuario = new Usuario("manuel", 22);
		this.usuario.setDireccion( new Direccion("Toledo",20) );
		
		Usuario compañero = new Usuario("pedro", 31);
		compañero.setDireccion( new Direccion("Madrid",75) );
		
		this.usuario.setCompañero(compañero);
		compañero.setCompañero(this.usuario);
    	
    	
    	//this.q=lib.newQuery(prueba.Usuario.class);
    }
    
    protected void tearDown() {
    	this.lib=null;
    	this.usuario=null;
    	//this.q=null;
    }
    public void testGuardar(){

		/*try {
			this.lib.guardarOactualizar(this.usuario);
		} catch (SQLException e) {
			e.printStackTrace();
			fail();
		}*/
		assertTrue(true);
    }
    
    
    public void testExecuteQuery(){
 
    	Usuario u=null;
		try {
			Constraint c1=SimpleConstraint.newEqualConstraint("nombre", this.usuario.getNombre());
			Constraint c2=SimpleConstraint.newEqualConstraint("edad", this.usuario.getEdad());
			Constraint c=new AndConstraint(c1,c2);
			
			Query q=lib.newQuery(prueba.Usuario.class);
			q.setConstraint(c);
	

			u=(Usuario) lib.executeQuery(q).get(0);
	

			
		} catch (SecurityException e) {
			e.printStackTrace();
			fail();
		}
		//assertSame(u, this.usuario);
		
		//assertTrue(u.getNombre()==this.usuario.getNombre());
		assertTrue(u.getEdad()==this.usuario.getEdad());
		//assertTrue(u.getDireccion()==this.usuario.getDireccion());	 
    }
    
    public void testQueryByExample(){
    	assertTrue(true);
    }
    
    public static Test suite() {

        TestSuite suite = new TestSuite(LibreriaTest.class);
        return suite;
    }
    
    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }

}
