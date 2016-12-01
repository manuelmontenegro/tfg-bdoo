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

import org.apache.commons.lang3.StringUtils;

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
	private HashMap<String, Object> idMap;

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
		this.idMap = new HashMap<String, Object>();
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
			String pair = o.getClass().getName()+"-"+id;
			this.idMap.put(pair, o);
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
	private List<Object> executeQuery(Query q) throws SQLException, InstantiationException, IllegalAccessException {
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
	protected boolean constainsKeyIdMap(String s){
		return this.idMap.containsKey(s);
	}
	/**
	 *devolver el objeto del idMap con la clave s 
	 * @param s
	 * @return
	 */
	protected Object getIdMap(String s) {
		return this.idMap.get(s);
	}
	/**
	 * inserter en el idMap el par key value
	 * @param key
	 * @param value
	 */
	protected void putIdMap(String key, Object value) {
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

	public static void main(String[] argv) {
		LibreriaBBDD lib = null;
		try {
			lib = new LibreriaBBDD("tfg", "root", "");
		} catch (PropertyVetoException | SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		/*
		 * Empleado empleado1 = new
		 * Empleado("0001","A","ramiro",24234,"hombre","cocinero","123");
		 * Empleado empleado2 = new
		 * Empleado("0002","B","ramiro",24234,"hombre","cocinero","123");
		 * Empleado empleado3 = new
		 * Empleado("0003","C","ramiro",24234,"mujer","cocinero","123");
		 * Empleado empleado4 = new
		 * Empleado("0004","D","ramiro",24234,"mujer","cocinero","123");
		 * Empleado empleado5 = new
		 * Empleado("0005","E","ramiro",24234,"mujer","cocinero","123");
		 * lib.guardar(empleado1); lib.guardar(empleado2);
		 * lib.guardar(empleado3); lib.guardar(empleado4);
		 * lib.guardar(empleado5);
		 */

		// EJEMPLO: Empleados que sean hombres o se llamen E (Empleados
		// 0001,0002,0005)

		
		Empleado empleado5 = new Empleado("0005","E","ramiro",24234,"mujer","cocinero","123");
		 
		 try {
			lib.guardar(empleado5);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException
				| SQLException | InsertarDuplicado e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		Query q=lib.newQuery(Empleado.class);

		SimpleConstraint sc1 = SimpleConstraint.igualQueConstraint("sexo", "mujer");
		SimpleConstraint sc2 = SimpleConstraint.igualQueConstraint("nombre", "E");
		Constraint oc = new AndConstraint(sc1, sc2);
		q.setConstraint(oc);
		List<Object> l1 = null;
		try {
			l1 = lib.executeQuery(q);
		} catch (SQLException | InstantiationException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Empleado e = (Empleado) l1.get(0);
		e.setContrasenya("54321");
		try {
			lib.update(e);
		} catch (SQLException | IllegalArgumentException | IllegalAccessException | BorrarObjetoInexistente e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}


		
	}

	

	


}

/*
 * Empleado e1=new Empleado(paco,33); Empleado e2=new Empleado(paco,33);
 * db.guardar(e1); db.guardar(e2); en realidad son objetos distintos
 * 
 * Empleado e3=e1; db.guardar(e3); este sin encambio es igual
 * ------------------------ patron identiti map en la libreria de base de datos
 * tener un Map<Object, Integer> activos tu buscas por objetos y si no esta le
 * insertas en el mapa y le guardas en la base de datos y si si esta que le
 * ignore o sobreescriba o actualize
 * 
 * ----------- si acabas de inicializar la libreria y haces querys debes meterle
 * en el mapa, tambien al guardar y si haces update de un objeto que no esta en
 * el mapa es un error exepcion nuestra
 * 
 * 
 * ----------------- Empleado e1=db.querry(dni=7941); Empleado
 * e2=db.querry(dni=7941); e1 y e2 son el mismo objeto y no debe hacer dos news
 * la primera vez se recupera y se mete en el mapa la segunda vez se comprueba
 * el mapa y se devuelve el objeto del mapa mapa inverso Class puede ser el
 * nombre de tabla (String) o el objeto class que es tambien unico Map<<Class,
 * Integer>, Object> asi o se hace hay que crear una clase que contenga estas
 * dos clases<Class, Integer> y implementar equals() <<Empleado,1>,(objeto
 * java)>
 * 
 * 
 * ------------
 * 
 * para borrar el objeto deve estar en el mapa, si no exepcion nuestra se bora
 * la entrada del mapa y la entrada en la base de datos
 * 
 * 
 * ----------------------- Map en java tiene varias implementaciones HasMap,
 * TreeMap pero ninguna de estas nos vale
 * 
 * String s1="Hola"; HashMap<>m=new HashMap<String, Integer>(); String
 * s2="Hola"; m.put(s1,3) Integer i=m.get(s2); el problema es que usa equals()
 * para buscar en el mapa y equals de s1 y s1 es true
 * 
 * hay una clase IdentityHashMap que usa el igual y no el equals() esta es la
 * que hay que usar
 * 
 * 
 */
