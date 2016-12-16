package tfg.tfg;

import java.beans.PropertyVetoException;
import java.lang.reflect.Field;
import java.security.acl.NotOwnerException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import excepciones.BorrarObjetoInexistente;
import excepciones.InsertarDuplicado;
import prueba.Empleado;

public class LibreriaBBDD {

	// Atributos
	private String user;
	private String pass;
	private String nombrebbdd;
	private ComboPooledDataSource cpds;
	private String nombreTabla;
	private IdentityHashMap<Object, Integer> objectMap;
	private HashMap<Identificador, Object> idMap;

	/**
	 * Constructor
	 * 
	 * @param nombrebbdd
	 * @param user
	 * @param pass
	 * @throws PropertyVetoException 
	 * @throws SQLException 
	 */
	public LibreriaBBDD(String nombrebbdd, String user, String pass) throws PropertyVetoException, SQLException {

		this.cpds = new ComboPooledDataSource();

		this.objectMap = new IdentityHashMap<Object, Integer>();
		this.idMap = new HashMap<Identificador, Object>();
		this.user = user;
		this.pass = pass;
		this.nombrebbdd = nombrebbdd;
		conectar();
		this.nombreTabla = "";
		crearTablaIndice();
		crearColumnaIndice();
	}

	public Connection getConnection() throws SQLException {
		Connection c = cpds.getConnection();

		return c;
	}

	/**
	 * Metodo para crear tabla indiceTabla
	 */
	private void crearTablaIndice() throws SQLException {
		String sql = "CREATE TABLE IF NOT EXISTS indicetabla " + "(id INTEGER not NULL AUTO_INCREMENT, "
				+ " nombreclase VARCHAR(255)," + " nombretabla VARCHAR(255)," + " PRIMARY KEY ( id ))";
		PreparedStatement pst;
		Connection c = this.getConnection();
		pst = c.prepareStatement(sql);
		pst.execute();
		c.close();

	}

	/**
	 * Metodo para crear tabla indiceColumna
	 * 
	 * @throws SQLException
	 */
	private void crearColumnaIndice() throws SQLException {
		String sql = "CREATE TABLE IF NOT EXISTS indicecolumna " + "(id INTEGER not NULL AUTO_INCREMENT, "
				+ " idtabla INTEGER," + " atributo VARCHAR(255)," + " columna VARCHAR(255)," + " PRIMARY KEY ( id ))";
		PreparedStatement pst;
		Connection c = this.getConnection();
		pst = c.prepareStatement(sql);
		pst.execute();
		c.close();

	}

	/**
	 * Metodo para conectar a la base de datos
	 * @throws PropertyVetoException 
	 */
	private void conectar() throws PropertyVetoException {

		this.cpds.setDriverClass("com.mysql.jdbc.Driver");

		this.cpds.setJdbcUrl("jdbc:mysql://localhost/" + this.nombrebbdd);
		this.cpds.setUser(this.user);
		this.cpds.setPassword(this.pass);

		this.cpds.setAcquireRetryAttempts(1);
		this.cpds.setAcquireRetryDelay(1);
	}

	/**
	 * Este metodo es para crear la base de datos cogemos todos los atributos de
	 * la clase que le pasemos
	 */
	private ArrayList<Atributo> sacarAtributos(Object o) {
		ArrayList<Atributo> atributos = new ArrayList<Atributo>();

		for (Field n : o.getClass().getDeclaredFields()) {
			String tipo = "";
			if (n.getType().getCanonicalName().equalsIgnoreCase("Java.lang.String"))
				tipo = "VARCHAR(255)";
			else if (n.getType().getCanonicalName().equalsIgnoreCase("Int"))
				tipo = "INTEGER";

			Atributo a = new Atributo(n.getName(), tipo);
			atributos.add(a);
		}

		return atributos;

	}

	/**
	 * Este metodo es para insertar en la base de datos teniendo en cuenta que
	 * no tienen porque estar todos los atributos de la clase metidos en el
	 * constructor que le pasemos, este metodo mira cual son nulos para no
	 * meterlos
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	private ArrayList<Atributo> sacarAtributosNoNulos(Object o) throws IllegalArgumentException, IllegalAccessException {
		ArrayList<Atributo> atributos = new ArrayList<Atributo>();

		// Con o.getClass().getDeclaredFields() --> Se cogen atributos privados
		for (Field n : o.getClass().getDeclaredFields()) {
			n.setAccessible(true);
				if (n.get(o) != null) {
					String tipo = "";
					if (n.getType().getCanonicalName().contains("Java.Lang.String"))
						tipo = "VARCHAR(255)";
					else if (n.getType().getCanonicalName().contains("int"))
						tipo = "INTEGER";

					Atributo a = new Atributo(n.getName(), tipo);
					atributos.add(a);
				}
		}

		return atributos;

	}
	
	/**
	 * Devuelve el nombre de la tabla que representa la clase del objeto recibido en la base de datos.
	 * @throws SQLException 
	 */
	public String getTableName(Object o) throws SQLException{
		Connection con = this.cpds.getConnection();
		String str = "";
		String sqlStatement = "SELECT nombretabla "
				+ "FROM INDICETABLA "
				+ "WHERE nombreclase = \'" 
				+ o.getClass().getName() + "\'"; 			//Sentencia sql para obtener el nombre de la tabla asociado a la clase 'clase'
		
		PreparedStatement pst;
		pst = con.prepareStatement(sqlStatement); 			// Preparación de la sentencia
		ResultSet rs = pst.executeQuery(); 					// Ejecución de la sentencia
		rs.next();
		str = rs.getString("nombretabla"); 					// str = nombre de la tabla
		con.close();
		return str;
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
	 * Metodo para crear la base de datos Te crea la base de datos con el
	 * nombreclase que le pasas Si existe no la crea
	 * @throws SQLException 
	 */
	private void crearTabla(ArrayList<Atributo> atributos, String nombreClase) throws SQLException {

		insertarTablaIndice(nombreClase);

		String sql = "CREATE TABLE IF NOT EXISTS " + this.nombreTabla + "(id INTEGER not NULL AUTO_INCREMENT, "
				+ " PRIMARY KEY ( id ))";
		//System.out.println(sql);
		PreparedStatement pst;
		Connection c = this.getConnection();
		pst = c.prepareStatement(sql);
		pst.execute();
		c.close();
	

		for (Atributo a : atributos) {
			insertarColumnaIndice(setIDtabla(nombreClase), a.getNombre(), a.getNombre(), a);
		}

		borrarAtributo(setIDtabla(nombreClase), atributos);

	}

	/**
	 * Borra el atributo de la base de datos en la columna indicecolumna que no
	 * este en el objeto y aparte borra la columna de la clase en cuestion
	 */
	private void borrarAtributo(String idtabla, ArrayList<Atributo> atributos) throws SQLException {
		String sql = "SELECT atributo FROM indicecolumna WHERE idtabla = ?";
		PreparedStatement pst;
		Connection c = this.getConnection();
		pst = c.prepareStatement(sql);

		pst.setString(1, idtabla);
		ResultSet rs = pst.executeQuery();
		boolean esta = false;
		while (rs.next()) {
			for (Atributo a : atributos) {
				if (rs.getString("atributo").equalsIgnoreCase(a.getNombre())) {
					esta = true;
				}
			}
			if (!esta) {

				String del = "ALTER TABLE " + this.nombreTabla + " DROP COLUMN " + rs.getString("atributo");
				c = this.getConnection();
				pst = c.prepareStatement(del);
				pst.execute();
				c.close();
				System.out.println(del);

				del = "DELETE FROM indicecolumna WHERE idtabla = ? and atributo = ?";
				c = this.getConnection();
				pst = c.prepareStatement(del);
				System.out.println(del);

				pst.setString(1, idtabla);
				pst.setString(2, rs.getString("atributo"));
				pst.execute();
				c.close();
			}
			esta = false;

		}
		c.close();
	
	}

	/**
	 * Metodo para recibir el id de la tabla indicetabla y asi insertarle en la
	 * tabla indicecolumna
	 */
	private String setIDtabla(String nombreClase) throws SQLException {
		String id = "";
		String sql = "SELECT id FROM indicetabla WHERE nombreclase = ? and nombretabla = ?";
		PreparedStatement pst;
		Connection c = this.getConnection();
		pst = c.prepareStatement(sql);
		pst.setString(1, nombreClase);
		pst.setString(2, this.nombreTabla);
		ResultSet rs = pst.executeQuery();
		if (rs.next()) {
			id = rs.getString("id");
		}
		c.close();


		return id;
	}

	/**
	 * Metodo para insertar filas en la tabla indicetabla Primero se mira si
	 * existe el nombre de la clase en la tabla si existe no se hace nada pero
	 * si no existe se inserta el nombre de la clase y el nombre de la tabla
	 */
	private void insertarTablaIndice(String nombreClase) throws SQLException {
		String sql = "SELECT nombreclase,nombretabla FROM indicetabla WHERE nombreclase = ?";
		PreparedStatement pst;
		Connection c = this.getConnection();
		pst = c.prepareStatement(sql);
		pst.setString(1, nombreClase);
		ResultSet rs = pst.executeQuery();
		if (!rs.next()) {
			String id = "";
			int idsuma = 0;
			sql = "SELECT max(id) FROM indicetabla";
			c = this.getConnection();
			pst = c.prepareStatement(sql);
			rs = pst.executeQuery();
			if (rs.next()) {

				idsuma = rs.getInt("max(id)") + 1;
				id = idsuma + "";
			} else
				id = "1";

			this.nombreTabla = this.nombreTabla + id;

			sql = "INSERT INTO indicetabla (nombreclase,nombretabla) " + " VALUES ( \"" + nombreClase + "\" , \""
					+ this.nombreTabla + "\" )";
			// System.out.println(sql);
			c = this.getConnection();
			pst = c.prepareStatement(sql);
			pst.execute();
			c.close();

		} else {
			this.nombreTabla = rs.getString("nombretabla");
		}
		c.close();
	

	}

	/**
	 * Metodo para insertar filas en la tabla indicecolumna Primero se mira si
	 * existe el idtabla y el atributo en la tabla si existe no se hace nada
	 * pero si no existe se inserta el idtabla,atributo y columna
	 */
	private void insertarColumnaIndice(String idtabla, String atributo, String columna, Atributo a) throws SQLException {
		String sql = "SELECT id FROM indicecolumna WHERE idtabla = ? and atributo = ?";
		PreparedStatement pst;
 
		Connection c = this.getConnection();
		pst = c.prepareStatement(sql);
		pst.setString(1, idtabla);
		pst.setString(2, atributo);
		ResultSet rs = pst.executeQuery();
		if (!rs.next()) {
			sql = "INSERT INTO indicecolumna (idtabla,atributo,columna) " + " VALUES ( \"" + idtabla + "\" , \""
					+ atributo + "\" , \"" + columna + "\"  )";
			System.out.println(sql);
			c = this.getConnection();
			pst = c.prepareStatement(sql);
			pst.execute();
			c.close();

			String anyadir = "ALTER TABLE " + this.nombreTabla + " ADD " + atributo + " " + a.getTipo();
			c = this.getConnection();
			pst = c.prepareStatement(anyadir);
			System.out.println(anyadir);
			pst.execute();
			c.close();

		}
		c.close();

	}

	/**
	 * Metodo para insertar en la base de datos Inserta en la base de datos
	 * nombreclase Inserta solo los atributos que le pases en el ArrayList
	 */
	private int insertarObjeto(Object o, ArrayList<Atributo> atributos) throws SQLException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		String claves = "";
		for (int i = 0; i < atributos.size(); i++) {
			if (i != 0)
				claves += " , ";
			Atributo a = atributos.get(i);
			claves += a.getNombre();
		}

		String valores = "";
		for (int i = 0; i < atributos.size(); i++) {
			Atributo a = atributos.get(i);
			if (i != 0)
				valores += " , ";
				Field val = o.getClass().getDeclaredField(a.getNombre());
				val.setAccessible(true);
				valores += "\"" + val.get(o) + "\"";
		
		}

		String sql = "INSERT INTO " + this.nombreTabla + " (" + claves + ") " + " VALUES " + "(" + valores + ")";

//		System.out.println(sql);
		PreparedStatement pst;
		Connection c = this.getConnection();
		pst = c.prepareStatement(sql);
		pst.execute();
		
		
		sql = "SELECT MAX(id) FROM " + this.nombreTabla ;
		pst = c.prepareStatement(sql);
		ResultSet rs = pst.executeQuery();

		int ret;
		if (rs.next()) {
			ret =rs.getInt("MAX(id)");
		}
		else
			ret= -1;
		c.close();
		return ret;
	}

	/**
	 *  Metodo para guardar ese objeto en la base de datos
	 * @param o
	 * @throws SQLException
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InsertarDuplicado
	 */
	public void guardar(Object o) throws SQLException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, InsertarDuplicado {
		ArrayList<Atributo> atributos = sacarAtributos(o);
		this.nombreTabla = o.getClass().getSimpleName();
		String nombreClase = o.getClass().getName();
		crearTabla(atributos, nombreClase);
		if(!this.objectMap.containsKey(o)) {
			int id = insertarObjeto(o, sacarAtributosNoNulos(o));
			this.objectMap.put(o, id);
			Identificador iden=new Identificador(id, o.getClass().getName());
			this.idMap.put(iden, o);
		} else {
			throw new InsertarDuplicado();
		}
	}
	
	/**
	 * Elimina de la base de datos el objeto recibido
	 * @param o
	 * @throws SQLException
	 * @throws BorrarObjetoInexistente 
	 */
	public void delete(Object o) throws SQLException, BorrarObjetoInexistente{
		if(!this.objectMap.containsKey(o)){
			throw new BorrarObjetoInexistente();
		}
		String tableName = this.getTableName(o);
		String sqlStatement = "DELETE FROM " + tableName +
				  " WHERE ID = ?";							//Sentencia SQL de eliminación
		Connection con = this.cpds.getConnection();
		PreparedStatement pst;
		Integer id = this.objectMap.get(o);
		pst = con.prepareStatement(sqlStatement);			//Preparación de la sentencia
		pst.setObject(1, id);			//Añadir la ID parametrizada
		System.out.println(pst);
		pst.execute();
		con.close();
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
	 * @throws BorrarObjetoInexistente 
	 */
	public void update(Object o) throws SQLException, IllegalArgumentException, IllegalAccessException, BorrarObjetoInexistente{
		if(!this.objectMap.containsKey(o)){
			throw new BorrarObjetoInexistente();
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
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				val.setAccessible(true);
				valores.add(val.get(o));
		
		}
		
		String tableName = this.getTableName(o);											//Nombre de la tabla de la base de datos perteneciente a la clase del objeto
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
	public List<Object> executeQuery(Query q) throws SQLException, InstantiationException, IllegalAccessException {
		Connection c = this.getConnection();
		List<Object> lista = q.executeQuery(c);
		c.close();
		return lista;
	}
	/**
	 * Devuelve una nueva Query
	 * @param class1
	 * @return
	 */
	private Query newQuery(Class class1) {
		
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
	protected Object getObjectMap(Object s) {
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
	public List<Object> queryByExample(Object o) throws InstantiationException, IllegalAccessException, NoSuchFieldException, SecurityException, SQLException{
		return queryByExample(o, new ArrayList<String>());		//Devuelve la lista recibida del método QueryByExample con una lista en la que se ignoran todos los campos que sean 0 o nulos
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
	public List<Object> queryByExample(Object o, List<String> notToIgnore) throws InstantiationException, IllegalAccessException, SQLException, NoSuchFieldException, SecurityException{
		List<Object> l = new ArrayList<Object>();						//Lista a devolver
		List<Constraint> constraintList = new ArrayList<Constraint>();	//Lista de rectricciones que se aplicarán
		Field[] campos = o.getClass().getDeclaredFields();				//Obtener los campos del objecto
		for(Field f: campos){											//Para cada uno de los campos:
			f.setAccessible(true);										//Hacerlo accesible
			boolean notIgnoring = false;								//Booleano para comprobar si el campo se ignorará en caso de ser 0 o nulo
			
			if(f.getType().getCanonicalName().contains("int")){			//Si el campo es un entero:
				int n = (int)f.get(o);									//Obtener el valor del entero
				if(n != 0)												//Si el entero es distinto de 0:
					notIgnoring = true;									//El campo no se ignora
			}
			else{														//Si el campo no es un entero:
				if(f.get(o) != null)									//Si es distinto de nulo:
					notIgnoring = true;									//El campo no se ignora
			}
			
			if(notToIgnore.contains(f.getName()) || notIgnoring)		//Si el campo aparece en la lista de no ignorados o se ha determinado que no se va a ignorar:
				constraintList.add(SimpleConstraint.igualQueConstraint(f.getName(), f.get(o)));		//Se añade a la lista de restricciones
		}
		
		Constraint c = new AndConstraint(constraintList);				//Se crea la restricción AND con las restricciones a cumplir de la lista
		Query q = new Query(o.getClass(), this);						//Se crea la consulta
		q.setConstraint(c);												//Se determina que la restricción de la consulta es la AND
		l = executeQuery(q);											//Se ejecuta la consulta
		return l;
	}

	public static void main(String[] argv) {
		LibreriaBBDD lib = null;
		try {
			lib = new LibreriaBBDD("tfg", "root", "");
		} catch (PropertyVetoException | SQLException e1) {e1.printStackTrace();}
		
		/*
		 Empleado empleado1 = new Empleado("0001","A","ramiro",24234,"hombre","cocinero","123");
		 Empleado empleado2 = new Empleado("0002","B","ramiro",24234,"hombre","cocinero","123");
		 Empleado empleado3 = new Empleado("0003","C","ramiro",24234,"mujer","cocinero","123");
		 Empleado empleado4 = new Empleado("0004","D","ramiro",24234,"mujer","cocinero","123");
		 Empleado empleado5 = new Empleado("0005","E","ramiro",24234,"hombre","cocinero","123");
		 Empleado empleado6 = new Empleado("0006","E","ramiro",0,"hombre","cocinero","123");
		 try {
			lib.guardar(empleado1);
			lib.guardar(empleado2);
			lib.guardar(empleado3); 
			lib.guardar(empleado4);
			lib.guardar(empleado5);
			lib.guardar(empleado6);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException | SQLException | InsertarDuplicado e2) {e2.printStackTrace();} 
		*/

		// EJEMPLO QUERYBYEXAMPLE IGNORANDO TODOS LOS CAMPOS: 
		//Empleados que sean hombres y se llamen E (Empleados 0005 y 0006)

		Empleado emp = new Empleado(null,"E",null,0,"hombre",null,null);
		
		System.out.println("Empleados que se llaman E y son hombres");
		List<Object> l1 = null;
		try {
			
			l1 = lib.queryByExample(emp);
			
		} catch (InstantiationException | IllegalAccessException | NoSuchFieldException | SecurityException | SQLException e) {e.printStackTrace();}
		
		for(Object o: l1){
			System.out.println(o.toString());
		}
		
		// EJEMPLO QUERYBYEXAMPLE IGNORANDO TODOS LOS CAMPOS MENOS EL TELÉFONO: 
		//Empleados que sean hombres, se llamen E y su teléfono sea 0 (Empleado 0006)
		//SE UTILIZA EL MISMO OBJETO emp DE LA CONSULTA ANTERIOR
		
		System.out.println("Empleados que se llaman E, son hombres y su teléfono es 0");
		List<Object> l2 = null;
		List<String> notToIgnore = new ArrayList<String>();
		notToIgnore.add("telefono");
		try {
			l2 = lib.queryByExample(emp,notToIgnore);
		} catch (InstantiationException | IllegalAccessException | NoSuchFieldException | SecurityException | SQLException e) {e.printStackTrace();}

		for (Object o : l2) {
			System.out.println(o.toString());
		}
		
		// EJEMPLO QUERYBYEXAMPLE SIN IGNORAR NINGUN CAMPO: 
		//Todos los empleados

		Empleado emp2 = new Empleado(null,null,null,0,null,null,null);
		
		System.out.println("Todos los empleados");
		List<Object> l3 = null;
		try {
			
			l3 = lib.queryByExample(emp2);
			
		} catch (InstantiationException | IllegalAccessException | NoSuchFieldException | SecurityException | SQLException e) {e.printStackTrace();}
		
		for(Object o: l3){
			System.out.println(o.toString());
		}

	}

	

	


}

/*

	cambiar el HasMap idMap la clave deve ser la clase identificador con el metodo equals() y el hashCode()

	usar el metodo de consulta QeryByExample
	Empleado e=new Empleado("Pepe", 0);
	List<Object>l=db.qeryByExampe(e);
	public <T> List<T> querryByExample(T obj){}
	devuelve todos los empleados con nombre pepe y la edad que sea 
	siempre encadenadas por ANDs y no tener en cuenta los null y los ceros
	
	
	se puede hacer otra version que si busque por el ceros y NULLs
	List<T> QueryByExample(T obj, List<String> dontIgnore);
	con una lista de los atributos que no hay que ignorar aunque sean nulos o ceros
	
	
	qBE(New Empleado(null, 0)) -> todos los empleados
	qBE(New Empleado("pepe", 21)) -> nombre pepe and edad 21
	qBE(New Empleado(null, 21)) -> edad=21
	qBe(New Empleado(pepe, 0)) -> nombre=pepe (ignora edad 0)
	qBE(New Empleado(pepe, 0),["edad"]) ->nombre=pepe and edad=0
	qBE(New Empleado(null, 0), [nombre, edad]) -> nombre is null and edad=0 ***importante is null
	qBE(New Empleado(null, 0), [nombre]) ->nombre is null
	
	
	cambiar simpleConstarin el toSql debe devolver "[campo] is null" cuando la parte derecha sea null y igual en los demas


 */


/*
patron que podemos usar para hacer esto
Foreign Key Mapping




1)cadena de punteros
si objeto apunta a otro que apunta a otro se deben guardar todos
2)los ciclos
si un objeto tiene otro que tiene otro que a su vez tie al primero no debe quedarse en u buqle infinito
3)actualizar en cadena con un boolean o otro nombre para que actualize en cascada o no
al actualizar un objeto actualizar tambien los objetos que tiene
4)borrar 
on delete set NULL
al borrar un empleado no borrar su direccion
5)al recuperar
si un objeto tiene referencias a varios objetos indicar un nivel maxio de profundiada por ejemplo 5
asi no recupera todos los objetos que tenga este y a partir de esa profundidad maxima poner NULLs
y si si quieres profundizar mas hacer un metodo acitvate() --en pag 96
este metodo recupera los NULLs que tiene un objeto que se halla recuperado antes para recuperarlos


*/
