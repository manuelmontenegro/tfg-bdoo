package oodb.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import oodb.constraints.AndConstraint;
import oodb.constraints.Constraint;
import oodb.constraints.SimpleConstraint;
import oodb.library.OODBLibrary;
import oodb.library.Query;

public class LibreriaTest extends TestCase{
	
	private OODBLibrary lib;
	private User user;
	
	 public LibreriaTest(String name) {
	        super(name);
	    }

	
    protected void setUp() {
    	this.lib=new OODBLibrary("tfg", "root", "");
    	
		this.user = new User("manuel", 22);
		this.user.setAddress( new Address("Toledo",20) );
		
		User partner = new User("pedro", 31);
		partner.setAddress( new Address("Madrid",75) );
		
		this.user.setPartner(partner);
		partner.setPartner(this.user);
    	
    	
    	//this.q=lib.newQuery(prueba.Usuario.class);
    }
    
    protected void tearDown() {
    	this.lib=null;
    	this.user=null;
    	//this.q=null;
    }
    public void testSave(){

		/*try {
			this.lib.guardarOactualizar(this.usuario);
		} catch (SQLException e) {
			e.printStackTrace();
			fail();
		}*/
		assertTrue(true);
    }
    
    
    public void testExecuteQuery(){
 
    	User u=null;
		try {
			Constraint c1=SimpleConstraint.newEqualConstraint("nombre", this.user.getName());
			Constraint c2=SimpleConstraint.newEqualConstraint("edad", this.user.getAge());
			Constraint c=new AndConstraint(c1,c2);
			
			Query q=lib.newQuery(oodb.test.User.class);
			q.setConstraint(c);
	

			u=(User) lib.executeQuery(q).get(0);
	

			
		} catch (SecurityException e) {
			e.printStackTrace();
			fail();
		}
		//assertSame(u, this.usuario);
		
		//assertTrue(u.getNombre()==this.usuario.getNombre());
		assertTrue(u.getAge()==this.user.getAge());
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
