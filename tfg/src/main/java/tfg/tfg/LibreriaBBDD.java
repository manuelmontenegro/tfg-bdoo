package tfg.tfg;

import java.beans.PropertyVetoException;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import constraints.AndConstraint;
import constraints.Constraint;
import constraints.SimpleConstraint;
import excepciones.ObjetoInexistente;
import prueba.Usuario;
import prueba.Direccion;
import excepciones.LibreriaBBDDException;


public class LibreriaBBDD {

	// Atributos
	private String user;
	private String pass;
	private String nombrebbdd;
	private ComboPooledDataSource cpds;
	private IdentityHashMap<Object, Integer> objectMap; //objetos con su id
	private HashMap<Identificador, Object> idMap; //identificador de clase r id con su objeto
	private HashMap<String, String> classMap; //<Nombre de la clase, Nombre de la tabla>
	private int profundidad;
	private GuardadorOactualizador guaOa;
	private Activador act;

	/**
	 * Constructor
	 * 
	 * @param nombrebbdd
	 * @param user
	 * @param pass
	 * @throws PropertyVetoException 
	 * @throws SQLException 
	 */
	public LibreriaBBDD(String nombrebbdd, String user, String pass) throws LibreriaBBDDException{

		this.cpds = new ComboPooledDataSource();

		this.objectMap = new IdentityHashMap<Object, Integer>();
		this.idMap = new HashMap<Identificador, Object>();
		this.classMap = new HashMap<String,String>();
		this.user = user;
		this.pass = pass;
		this.nombrebbdd = nombrebbdd;
		this.profundidad = 2;
		conectar();
		crearTablaIndice();
		crearColumnaIndice();
		this.guaOa=new GuardadorOactualizador(this);
		this.act=new Activador(this);
	}

	public Connection getConnection() throws SQLException {
		Connection c = cpds.getConnection();

		return c;
	}

	/**
	 * Metodo para crear tabla indiceTabla
	 */
	private void crearTablaIndice() throws LibreriaBBDDException{
		String sql = "CREATE TABLE IF NOT EXISTS indicetabla " + "(id INTEGER not NULL AUTO_INCREMENT, "
				+ " nombreclase VARCHAR(255)," + " nombretabla VARCHAR(255)," + " PRIMARY KEY ( id ))";
		PreparedStatement pst;
		Connection c;
		try {
			c = this.getConnection();
			pst = c.prepareStatement(sql);
			pst.execute();
			c.close();
		} catch (SQLException e) {
			throw new LibreriaBBDDException(e);
		}
		

	}

	/**
	 * Metodo para crear tabla indiceColumna
	 * 
	 * @throws SQLException
	 */
	private void crearColumnaIndice() throws LibreriaBBDDException {
		String sql = "CREATE TABLE IF NOT EXISTS indicecolumna " 
				+ "(id INTEGER not NULL AUTO_INCREMENT, "
				+ " idtabla INTEGER, " 
				+ " atributo VARCHAR(255)," 
				+ " columna VARCHAR(255),"
				+ " nombrecolumnatipo VARCHAR(255)," + " PRIMARY KEY ( id ),"
				+"  CONSTRAINT fk_table FOREIGN KEY (idtabla) REFERENCES indicetabla(id)) ";
		PreparedStatement pst;
		//System.out.println(sql);
		Connection c;
		try {
			c = this.getConnection();
			pst = c.prepareStatement(sql);
			pst.execute();
			c.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}

	/**
	 * Metodo para conectar a la base de datos
	 * @throws PropertyVetoException 
	 */
	private void conectar()throws LibreriaBBDDException{

		try {
			this.cpds.setDriverClass("com.mysql.jdbc.Driver");
		} catch (PropertyVetoException e) {
			throw new LibreriaBBDDException(e);
		}

		this.cpds.setJdbcUrl("jdbc:mysql://localhost/" + this.nombrebbdd);
		this.cpds.setUser(this.user);
		this.cpds.setPassword(this.pass);

		this.cpds.setAcquireRetryAttempts(1);
		this.cpds.setAcquireRetryDelay(1);
	}
	
	public String getTableName(String nombreClase) throws SQLException{
		if(classMap.containsKey(nombreClase))
			return classMap.get(nombreClase);
		else{
			Connection con = this.cpds.getConnection();
			String str = "";
			String sqlStatement = "SELECT nombretabla "
					+ "FROM INDICETABLA "
					+ "WHERE nombreclase = \'" 
					+ nombreClase + "\'"; 		//Sentencia sql para obtener el nombre de la tabla asociado a la clase 'clase'
			PreparedStatement pst;
			pst = con.prepareStatement(sqlStatement); 			// Preparación de la sentencia
			ResultSet rs = pst.executeQuery(); 					// Ejecución de la sentencia
			rs.next();
			str = rs.getString("nombretabla"); 					// str = nombre de la tabla
			con.close();
			classMap.put(nombreClase, str);
			return str;
		}
	}
	
	int getId(Object obj){
		return this.objectMap.get(obj);
	}
	
	/**
	 * Devuelve una cadena con el formato atributo1=?,atributo2=?,... para usar en la sentencia SQL
	 */
	/*private String getObjectSets(Object o){
		ArrayList<String> list = new ArrayList<String>();
		Field[] campos = o.getClass().getDeclaredFields();		//Obtener los campos del objecto
		for(Field f: campos){									//Para cada uno de los campos:
			list.add(f.getName() + "= ?");						//Añade a la lista atributo = ?
		}
		return StringUtils.join(list, ",");
	}*/
	
	
	
	/**
	 * Metodo de la libreria para guardar un objeto y todos los que tenga a su vez
	 * si el objeto ya esta previamente guardado actualizarara sus valores
	 * Usa un mapa parcial de objetos visitados que a la vuelta vuelca en el general de objetos guardados
	 * @param o objeto a guardar
	 * @throws SQLException
	 */
	public void guardarOactualizar(Object o) throws LibreriaBBDDException{
		IdentityHashMap<Object, Integer> im=new IdentityHashMap<Object, Integer>();
		try {
			this.guaOa.guardarOactualizar(o, im);
		} catch (IllegalArgumentException | IllegalAccessException | SQLException e) {
			throw new LibreriaBBDDException(e);
		}
		this.objectMap.putAll(im);
	}

	

	/**
	 * Este metodo es para insertar en la base de datos teniendo en cuenta que
	 * no tienen porque estar todos los atributos de la clase metidos en el
	 * constructor que le pasemos, este metodo mira cual son nulos para no
	 * meterlos
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	ArrayList<Atributo> sacarAtributosNoNulos(Object o) throws IllegalArgumentException, IllegalAccessException {
		ArrayList<Atributo> atributos = new ArrayList<Atributo>();

		// Con o.getClass().getDeclaredFields() --> Se cogen atributos privados
		for (Field n : o.getClass().getDeclaredFields()) {
			n.setAccessible(true);
				if (n.get(o) != null) {
					String tipo = "";
					if (n.getType().getCanonicalName().contains("Java.Lang.String") || n.getType().getCanonicalName().contains("java.lang.String"))
						tipo = "VARCHAR(255)";
					else if (n.getType().getCanonicalName().contains("int"))
						tipo = "INTEGER";
					

					Atributo a = new Atributo(n.getName(), tipo, false,false);
					atributos.add(a);
				}
		}

		return atributos;

	}
	
	/**
	 * Elimina de la base de datos el objeto recibido
	 * @param o
	 * @throws SQLException
	 * @throws ObjetoInexistente 
	 */
	public void delete(Object o) throws LibreriaBBDDException{
		if(!this.objectMap.containsKey(o)){
			throw new LibreriaBBDDException(new ObjetoInexistente());
		}
		String tableName;
		Integer id;
		try {
			tableName = this.getTableName(o.getClass().getName());
			String sqlStatement = "DELETE FROM " + tableName +
					  " WHERE ID = ?";							//Sentencia SQL de eliminación
			Connection con = this.cpds.getConnection();
			PreparedStatement pst;
			id = this.objectMap.get(o);
			pst = con.prepareStatement(sqlStatement);			//Preparación de la sentencia
			pst.setObject(1, id);			//Añadir la ID parametrizada
			System.out.println(pst);
			pst.execute();
			con.close();
		} catch (SQLException e) {
			throw new LibreriaBBDDException(e);
		}
		this.objectMap.remove(o);
		this.idMap.remove(o.getClass().getName()+"-"+id);

	}
	
	/**
	 * Actualiza en la base de datos los atributos del objeto recibido
	 * 
	 * @param o
	 * @throws SQLException
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 * @throws ObjetoInexistente 
	 */
	public void update(Object o) throws LibreriaBBDDException{
		if(!this.objectMap.containsKey(o)){
			throw new LibreriaBBDDException(new ObjetoInexistente());
		}
		ArrayList<Atributo> atributos;
		try {
			atributos = sacarAtributosNoNulos(o);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new LibreriaBBDDException(e);
		}
		String claves = "";
		for (int i = 0; i < atributos.size(); i++) {
			if (i != 0)
				claves += " , ";
			Atributo a = atributos.get(i);
			claves += a.getNombre()+" = ?";
		}

		ArrayList<Object> valores = new ArrayList<Object>();
		for (int i = 0; i < atributos.size(); i++) {
			Atributo a = atributos.get(i);
			
				Field val = null;
				try {
					val = o.getClass().getDeclaredField(a.getNombre());
				} catch (NoSuchFieldException | SecurityException e) {
					throw new LibreriaBBDDException(e);
				}
				val.setAccessible(true);
				if(!val.getType().getCanonicalName().contains("java.lang.String") && !val.getType().getCanonicalName().contains("int")){ //Si el campo no es ni int ni string:
					try {
						valores.add(this.objectMap.get(val.get(o)));
					} catch (IllegalArgumentException | IllegalAccessException e) {
						throw new LibreriaBBDDException(e);
					}
				}
				else{
					try {
						valores.add(val.get(o));
					} catch (IllegalArgumentException | IllegalAccessException e) {
						throw new LibreriaBBDDException(e);
					}
				}
		
		}
		
		String tableName;
		try {
			tableName = this.getTableName(o.getClass().getName());//Nombre de la tabla de la base de datos perteneciente a la clase del objeto
			String sqlStatement = "UPDATE " + tableName +
					  " SET " + claves +
					  " WHERE ID = ?";		
			Connection con = this.cpds.getConnection();
			PreparedStatement pst;
			pst = con.prepareStatement(sqlStatement);											//Preparación de la sentencia
			for (int i = 1; i <= valores.size(); i++) { 
				pst.setObject(i, valores.get(i-1)); 						// Añadir el valor a la sentencia
			}
			pst.setObject(o.getClass().getDeclaredFields().length+1, this.objectMap.get(o));	//Añadir la ID parametrizada
			pst.execute();
			con.close();
		} catch (SQLException e) {
			throw new LibreriaBBDDException(e);
		}											
		
	}
	
	public void updateProfundidad(Object o) throws LibreriaBBDDException{
		try {
			updateProfundidad(o,this.profundidad);
		} catch (IllegalArgumentException | IllegalAccessException | SQLException | ObjetoInexistente e) {
			throw new LibreriaBBDDException(e);
		}
	}
	
	/**
	 * Actualiza en la base de datos los atributos del objeto recibido
	 * 
	 * @param o
	 * @throws SQLException
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 * @throws ObjetoInexistente 
	 */
	private void updateProfundidad(Object o, int prof) throws SQLException, IllegalArgumentException, IllegalAccessException, ObjetoInexistente{
		if(!this.objectMap.containsKey(o)){
			throw new ObjetoInexistente(); //Cambiar nombre
		}
		ArrayList<Atributo> atributos = sacarAtributosNoNulos(o);
		String claves = "";
		for (int i = 0; i < atributos.size(); i++) {
			if (i != 0)
				claves += " , ";
			Atributo a = atributos.get(i);
			claves += a.getNombre()+" = ?";
		}

		ArrayList<Object> valores = new ArrayList<Object>();
		for (int i = 0; i < atributos.size(); i++) {
			Atributo a = atributos.get(i);
			
				Field val = null;
				try {
					val = o.getClass().getDeclaredField(a.getNombre());
				} catch (NoSuchFieldException | SecurityException e) {
					throw new LibreriaBBDDException(e);
				}
				val.setAccessible(true);
				if(!val.getType().getCanonicalName().contains("java.lang.String") && !val.getType().getCanonicalName().contains("int")){ 					//Si el campo no es ni int ni string:
					if(prof == 0)
						valores.add(this.objectMap.get(val.get(o)));
					else{
						valores.add(this.objectMap.get(val.get(o)));
						updateProfundidad(val.get(o),prof-1);
					}
					
				}
				else{
					valores.add(val.get(o));
				}
		
		}
		
		String tableName = this.getTableName(o.getClass().getName());											//Nombre de la tabla de la base de datos perteneciente a la clase del objeto
		String sqlStatement = "UPDATE " + tableName +
							  " SET " + claves +
							  " WHERE ID = ?";		
		Connection con = this.cpds.getConnection();
		PreparedStatement pst;
		pst = con.prepareStatement(sqlStatement);											//Preparación de la sentencia
		for (int i = 1; i <= valores.size(); i++) { 
			pst.setObject(i, valores.get(i-1)); 						// Añadir el valor a la sentencia
		}
		pst.setObject(o.getClass().getDeclaredFields().length+1, this.objectMap.get(o));	//Añadir la ID parametrizada
		pst.execute();
		con.close();
	}

	/**
	 * Ejecuta la consulta recibida 
	 * @param q
	 * @return Lista de objetos recibidos por la consulta
	 * @throws SQLException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public List<Object> executeQuery(Query q) throws LibreriaBBDDException {
		Connection c;
		try {
			c = this.getConnection();
		} catch (SQLException e) {
			throw new LibreriaBBDDException(e);
		}
		List<Object> lista;
		try {
			lista = q.executeQuery(c, this.profundidad);
			c.close();
		} catch (InstantiationException | IllegalAccessException | SQLException | ClassNotFoundException e) {
			throw new LibreriaBBDDException(e);
		}
		
		return lista;
	}
	
	/**
	 * Ejecuta la consulta recibida 
	 * @param q
	 * @return Lista de objetos recibidos por la consulta
	 * @throws SQLException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public List<Object> executeQuery(Query q, int profundidad) throws LibreriaBBDDException {
		Connection c;
		List<Object> lista;
		try {
			c = this.getConnection();
			lista = q.executeQuery(c, profundidad);
			c.close();
		} catch (SQLException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			throw new LibreriaBBDDException(e);
		}
		
		return lista;
	}
	
	public void activar(Object o, int profundidad) throws LibreriaBBDDException{
		try {
			this.act.activar(o, profundidad);
		} catch (ClassNotFoundException e) {
			throw new LibreriaBBDDException(e);
		}
	}
	
	public void activar(Object o) throws LibreriaBBDDException{
		try {
			this.act.activar(o, this.profundidad);
		} catch (ClassNotFoundException e) {
			throw new LibreriaBBDDException(e);
		}
	}
	/**
	 * Devuelve una nueva Query
	 * @param class1
	 * @return
	 */
	public Query newQuery(Class<?> class1) {
		
		return new Query(class1, this);
	}
	
	/**
	 * consultar si el mapa idMap contiene la clave s
	 * @param s
	 * @return
	 */
	protected boolean constainsKeyIdMap(Identificador s){
		return this.idMap.containsKey(s);
	}
	/**
	 *devolver el objeto del idMap con la clave s 
	 * @param s
	 * @return
	 */
	protected Object getIdMap(Identificador s) {
		return this.idMap.get(s);
	}
	/**
	 * inserter en el idMap el par key value
	 * @param key
	 * @param value
	 */
	protected void putIdMap(Identificador key, Object value) {
		this.idMap.put(key, value);
	}
	/**
	 * Consultar si mapa objectMap contiene la clave o
	 * @param o
	 * @return
	 */
	protected boolean constainsKeyObjectMap(Object o){
		return this.objectMap.containsKey(o);
	}
	/**
	 * Devolver el objeto del objectMap con la clave s 
	 * @param s
	 * @return
	 */
	protected int getObjectMap(Object s) {
		return this.objectMap.get(s);
	}
	/**
	 * Inserter en el objectMap el par key value
	 * @param key
	 * @param value
	 */
	protected void putObjectMap(Object key, Integer value) {
		this.objectMap.put(key, value);
	}
	
	/**
	 * Metodo para cambiar la profundidad
	 * @param profundidad
	 */
	public void setProfundidad(int profundidad){
		this.profundidad = profundidad;
	}
	
	/**
	 * Implementa el método de consulta QueryByExample.
	 * Recibe un objeto modelo e ignora todos los atributos que sean 0 o nulos.
	 * @param o
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 * @throws SQLException
	 */
	public List<Object> queryByExample(Object o) throws LibreriaBBDDException{
		try {
			return queryByExample(o, new ArrayList<String>());
		} catch (SecurityException e) {
			throw new LibreriaBBDDException(e);
		}		//Devuelve la lista recibida del método QueryByExample con una lista en la que se ignoran todos los campos que sean 0 o nulos
	}
	
	/**
	 * Implementa el método de cosulta QueryByExample.
	 * Recibe un objeto modelo y una lista de atributos que no se ignorarán en caso de ser 0 o nulos.
	 * @param o
	 * @param notToIgnore
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws SQLException
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 */
	public List<Object> queryByExample(Object o, List<String> notToIgnore) throws LibreriaBBDDException{
		List<Object> l = new ArrayList<Object>();						//Lista a devolver
		List<Constraint> constraintList = new ArrayList<Constraint>();	//Lista de rectricciones que se aplicarán
		Field[] campos = o.getClass().getDeclaredFields();				//Obtener los campos del objecto
		for(Field f: campos){											//Para cada uno de los campos:
			f.setAccessible(true);										//Hacerlo accesible
			boolean notIgnoring = false;								//Booleano para comprobar si el campo se ignorará en caso de ser 0 o nulo
			
			if(f.getType().getCanonicalName().contains("int")){			//Si el campo es un entero:
				int n;
				try {
					n = (int)f.get(o);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new LibreriaBBDDException(e);
				}									//Obtener el valor del entero
				if(n != 0)												//Si el entero es distinto de 0:
					notIgnoring = true;									//El campo no se ignora
			}
			else{														//Si el campo no es un entero:
				try {
					if(f.get(o) != null)									//Si es distinto de nulo:
						notIgnoring = true;
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new LibreriaBBDDException(e);
				}									//El campo no se ignora
			}
			
			if(notToIgnore.contains(f.getName()) || notIgnoring)
				try {
					constraintList.add(SimpleConstraint.igualQueConstraint(f.getName(), f.get(o)));
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new LibreriaBBDDException(e);
				}		//Se añade a la lista de restricciones
		}
		
		Constraint c = new AndConstraint(constraintList);				//Se crea la restricción AND con las restricciones a cumplir de la lista
		for (int i = 0; i < constraintList.size(); i++) {
			System.out.println(constraintList.get(i).toSql() + constraintList.get(i).getValues().get(0));
		}
		Query q = new Query(o.getClass(), this);						//Se crea la consulta
		q.setConstraint(c);												//Se determina que la restricción de la consulta es la AND
		l = executeQuery(q);
		return l;
	}
	
	boolean atributoBasico(Class<?> c){
		return c.getCanonicalName().equalsIgnoreCase("Java.lang.String") 
				|| c.getCanonicalName().equalsIgnoreCase("Int");
	}

	public static void main(String[] argv) {
		LibreriaBBDD lib = null;
		
		lib = new LibreriaBBDD("tfg", "root", "");
		
		Direccion d = new Direccion();
		Query q = lib.newQuery(Usuario.class);
		Constraint c1 = SimpleConstraint.igualQueConstraint("domicilio", new Direccion());
		Constraint c2 = SimpleConstraint.igualQueConstraint("compañero.domicilio.calle", "i");
		Constraint c = new AndConstraint(c1,c2);
		q.setConstraint(c);
		lib.executeQuery(q);


	}

}


/*


5 junio borrador para profesor pra que le corrija, se puede ir corrijiendo antes
16 junio entrega memoria final en secretaria,  con el codigo y todo lo demas
28 29 30 junio exposicion 5 minutos cada uno con su parte en ingles



*/
