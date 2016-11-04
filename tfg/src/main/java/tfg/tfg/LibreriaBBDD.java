package tfg.tfg;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import prueba.Empleado;
import prueba2.Plato;


public class LibreriaBBDD {

	//Atributos
	private String user;
	private String pass;
	private String nombrebbdd;
	private Connection con;
	private String nombreTabla;
	
	//Constructor
	public LibreriaBBDD(String nombrebbdd, String user, String pass){
		this.user = user;
		this.pass = pass;
		this.nombrebbdd = nombrebbdd;
		this.con = conectar();
		this.nombreTabla = "";
		crearTablaIndice();
		crearColumnaIndice();
	}
	
	public Connection getConnection(){
		return this.con;
	}
	//Metodo para crear tabla indiceTabla
	private void crearTablaIndice() {
		String sql = "CREATE TABLE IF NOT EXISTS indicetabla " +
                "(id INTEGER not NULL AUTO_INCREMENT, " +
				" nombreclase VARCHAR(255)," +
                " nombretabla VARCHAR(255)," +
                " PRIMARY KEY ( id ))"; 
		PreparedStatement pst;
		try {
			pst = this.con.prepareStatement(sql);
			pst.execute();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	//Metodo para crear tabla indiceColumna
	private void crearColumnaIndice() {
		String sql = "CREATE TABLE IF NOT EXISTS indicecolumna " +
                "(id INTEGER not NULL AUTO_INCREMENT, " +
				" idtabla INTEGER," +
				" atributo VARCHAR(255)," +
                " columna VARCHAR(255)," +
                " PRIMARY KEY ( id ))"; 
		PreparedStatement pst;
		try {
			pst = this.con.prepareStatement(sql);
			pst.execute();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// Metodo para conectar a la base de datos
	private Connection conectar(){
	       Connection link = null;
	       try{
	           Class.forName("org.gjt.mm.mysql.Driver");
	            link = DriverManager.getConnection("jdbc:mysql://localhost/"+this.nombrebbdd, this.user,this.pass);
	       }catch(Exception ex){
	           JOptionPane.showMessageDialog(null, ex);
	       }
	       return link;
	   }
	
	/*Este metodo es para crear la base de datos
	 * 	cogemos todos los atributos de la clase que le pasemos
	 */
	private ArrayList<Atributo> sacarAtributos(Object o){
		ArrayList<Atributo> atributos = new ArrayList<Atributo>();
		
		for(Field n : o.getClass().getDeclaredFields()){
			String tipo = "";
			if(n.getType().getCanonicalName().contains("String"))
				tipo = "VARCHAR(255)";
			else if(n.getType().getCanonicalName().contains("int"))
				tipo = "INTEGER";
				
			Atributo a = new Atributo(n.getName(),tipo);
			atributos.add(a);
		}
		
		return atributos;

	}

	/*Este metodo es para insertar en la base de datos
	 * teniendo en cuenta que no tienen porque estar todos los atributos de la clase
	 * metidos en el constructor que le pasemos,
	 * este metodo mira cual son nulos para no meterlos
	 */
	private ArrayList<Atributo> sacarAtributosNoNulos(Object o){
		ArrayList<Atributo> atributos = new ArrayList<Atributo>();
		
		// Con o.getClass().getDeclaredFields() --> Se cogen atributos privados
		for(Field n : o.getClass().getDeclaredFields()){
			n.setAccessible(true);
			try {
				if(n.get(o)!= null){
					String tipo = "";
					if(n.getType().getCanonicalName().contains("String"))
						tipo = "VARCHAR(255)";
					else if(n.getType().getCanonicalName().contains("int"))
						tipo = "INTEGER";
						
					Atributo a = new Atributo(n.getName(),tipo);
					atributos.add(a);
				}
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return atributos;

	}
	
	/*Metodo para crear la base de datos
	 * Te crea la base de datos con el nombreclase que le pasas
	 * Si existe no la crea
	 */
	private void crearTabla(ArrayList<Atributo> atributos, String nombreClase) {
		
		insertarTablaIndice(nombreClase);

		String sql = "CREATE TABLE IF NOT EXISTS "+ this.nombreTabla +
                "(id INTEGER not NULL AUTO_INCREMENT, " +
                " PRIMARY KEY ( id ))"; 
		System.out.println(sql);
		PreparedStatement pst;
		try {
			pst = this.con.prepareStatement(sql);
			pst.execute();

		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		for(Atributo a : atributos){
			insertarColumnaIndice(setIDtabla(nombreClase),a.getNombre(),a.getNombre(),a);
		}
		
		
		borrarAtributo(setIDtabla(nombreClase),atributos);

		
	}
	
	/*Borra el atributo de la base de datos en la columna indicecolumna que no este en el objeto
	 * y aparte borra la columna de la clase en cuestion
	 */
	private void borrarAtributo(String idtabla, ArrayList<Atributo> atributos){
		String sql = "SELECT atributo FROM indicecolumna WHERE idtabla = ?";
		PreparedStatement pst;
		try {
			pst = this.con.prepareStatement(sql);
			pst.setString(1, idtabla);
			ResultSet rs = pst.executeQuery();
			boolean esta = false;
			while(rs.next()){
				for(Atributo a : atributos){
					if(rs.getString("atributo").equalsIgnoreCase(a.getNombre())){			
						esta = true;
					}					
				}
				if(!esta){
					
					String del = "ALTER TABLE "+this.nombreTabla+" DROP COLUMN "+rs.getString("atributo");
					pst = this.con.prepareStatement(del);
					pst.execute();
					System.out.println(del);
					
					del = "DELETE FROM indicecolumna WHERE idtabla = ? and atributo = ?";
					pst = this.con.prepareStatement(del);
					System.out.println(del);

					pst.setString(1, idtabla);
					pst.setString(2, rs.getString("atributo"));
					pst.execute();					
				}
				esta = false;

			}
		} catch (SQLException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*Metodo para recibir el id de la tabla indicetabla
	 * y asi insertarle en la tabla indicecolumna
	 */
	private String setIDtabla( String nombreClase) {
		String id = "";
		String sql = "SELECT id FROM indicetabla WHERE nombreclase = ? and nombretabla = ?";
		PreparedStatement pst;
		try {
			pst = this.con.prepareStatement(sql);
			pst.setString(1, nombreClase);
			pst.setString(2, this.nombreTabla);
			ResultSet rs = pst.executeQuery();
			if(rs.next()){
				id = rs.getString("id");
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return id;
	}

	/* Metodo para insertar filas en la tabla indicetabla
	 * Primero se mira si existe el nombre de la clase en la tabla
	 * si existe no se hace nada pero si no existe se inserta
	 * el nombre de la clase y el nombre de la tabla
	 */
	private void insertarTablaIndice(String nombreClase) {
		String sql = "SELECT nombreclase,nombretabla FROM indicetabla WHERE nombreclase = ?";
		PreparedStatement pst;
		try {
			pst = this.con.prepareStatement(sql);
			pst.setString(1, nombreClase);
			ResultSet rs = pst.executeQuery();
			if(!rs.next()){
				String id = ""; 
				int idsuma = 0;
				sql = "SELECT max(id) FROM indicetabla";
				pst = this.con.prepareStatement(sql);
				rs = pst.executeQuery();
				if(rs.next()){
					
					idsuma = rs.getInt("max(id)") + 1;
					id = idsuma+"";
				}
				else
					id = "1";
					
				
				this.nombreTabla = this.nombreTabla+id;

				sql = "INSERT INTO indicetabla (nombreclase,nombretabla) "+
					     " VALUES ( \""+ nombreClase + "\" , \"" + this.nombreTabla + "\" )";
				System.out.println(sql);
				try {
					pst = this.con.prepareStatement(sql);
					pst.execute();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
			}
			else{
				this.nombreTabla = rs.getString("nombretabla");
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/* Metodo para insertar filas en la tabla indicecolumna
	 * Primero se mira si existe el idtabla y el atributo en la tabla
	 * si existe no se hace nada pero si no existe se inserta
	 * el idtabla,atributo y columna
	 */
	private void insertarColumnaIndice(String idtabla, String atributo, String columna, Atributo a) {
		String sql = "SELECT id FROM indicecolumna WHERE idtabla = ? and atributo = ?";
		PreparedStatement pst;
		try {
			pst = this.con.prepareStatement(sql);
			pst.setString(1, idtabla);
			pst.setString(2, atributo);
			ResultSet rs = pst.executeQuery();
			if(!rs.next()){
				sql = "INSERT INTO indicecolumna (idtabla,atributo,columna) "+
					     " VALUES ( \""+ idtabla + "\" , \"" + atributo + "\" , \"" + columna + "\"  )";
				System.out.println(sql);
				try {
					pst = this.con.prepareStatement(sql);
					pst.execute();
					
					String anyadir = "ALTER TABLE "+this.nombreTabla+" ADD " +atributo + " " +a.getTipo();
					pst = this.con.prepareStatement(anyadir);
					System.out.println(anyadir);
					pst.execute();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	
	/*Metodo para insertar en la base de datos
	 * Inserta en la base de datos nombreclase
	 * Inserta solo los atributos que le pases en el ArrayList
	 */
	private void insertarObjeto(Object o,ArrayList<Atributo> atributos) {
		String claves = "";
		for(int i = 0; i < atributos.size(); i++){
			if(i != 0)
				claves += " , ";
			Atributo a = atributos.get(i);
			claves += a.getNombre();	
		}
		
		String valores = "";
		for(int i = 0; i <atributos.size(); i++){
			Atributo a = atributos.get(i);
			if(i != 0)
				valores += " , ";
			try {
				Field val = o.getClass().getDeclaredField(a.getNombre());
				val.setAccessible(true);
				valores +=  "\""+val.get(o)+"\"";
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		String sql = "INSERT INTO "+this.nombreTabla+ " ("+claves+") "+
				     " VALUES "+ "("+valores+")";
			
		System.out.println(sql);
		PreparedStatement pst;
			try {
				pst = this.con.prepareStatement(sql);
				pst.execute();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		
	}
	
	//Metodo para guardar ese objeto en la base de datos
	public void guardar(Object o){		
		ArrayList<Atributo> atributos = sacarAtributos(o);
		this.nombreTabla = o.getClass().getSimpleName();
		String nombreClase = o.getClass().getName();

		crearTabla(atributos,nombreClase);
		insertarObjeto(o,sacarAtributosNoNulos(o));
		System.out.println("Objeto Insertado");
	}	
	
	public static void main(String[] argv){
		LibreriaBBDD lib = new LibreriaBBDD("tfg","root","");
		/*
		Empleado empleado1 = new Empleado("0001","A","ramiro",24234,"hombre","cocinero","123");
		Empleado empleado2 = new Empleado("0002","B","ramiro",24234,"hombre","cocinero","123");
		Empleado empleado3 = new Empleado("0003","C","ramiro",24234,"mujer","cocinero","123");
		Empleado empleado4 = new Empleado("0004","D","ramiro",24234,"mujer","cocinero","123");
		Empleado empleado5 = new Empleado("0005","E","ramiro",24234,"mujer","cocinero","123");
		lib.guardar(empleado1);
		lib.guardar(empleado2);
		lib.guardar(empleado3);
		lib.guardar(empleado4);
		lib.guardar(empleado5);
		*/
		
		//EJEMPLO: Empleados que sean hombres o se llamen E (Empleados 0001,0002,0005)
		Query q = new Query(Empleado.class);
		//->HAY QUE CAMBIARLO PARA QUE NO HAYA QUE PONER LAS COMILLAS
		SimpleConstraint sc1 = SimpleConstraint.igualQueConstraint("sexo", "\"hombre\"");
		SimpleConstraint sc2 = SimpleConstraint.igualQueConstraint("nombre", "\"E\"");
		OrConstraint oc = new OrConstraint(sc1,sc2);
		q.setConstraint(oc);
		List<Object> l = q.executeQuery(lib.getConnection());
		for(Object o: l){
			System.out.println(o.toString());
		}
		
		/*
		 cambiar el par de constrain por una lista y en el constructos que reciba  constraint con puntos suspensivos....
		-----------
		Query q = new Query(Empleado.class, oc);//tambien se puede hacer un constructor solo con el .class y que la constraian la ponga a TrueConstrain
		
		bd.exequteQuerry(q); // esto coge una coxion de un pool de conexiones
		-----------------------
		
		
		hay que hacer consultas parametricas "?"
		*/
		
		
		
		 
		
	}
	
	
	

	
}

