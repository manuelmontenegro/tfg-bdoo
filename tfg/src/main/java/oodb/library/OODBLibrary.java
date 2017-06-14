package oodb.library;

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

import oodb.constraints.AndConstraint;
import oodb.constraints.Constraint;
import oodb.constraints.SimpleConstraint;
import oodb.exception.NonExistentObject;
import oodb.exception.OODBLibraryException;

/**
 * Main class of the library. 
 */
public class OODBLibrary {

	private String user;
	private String pass;
	private String dbname;
	private ComboPooledDataSource cpds;
	private IdentityHashMap<Object, Integer> objectMap;
	private HashMap<Identificator, Object> idMap;
	private HashMap<String, String> classMap;
	private int depth;
	private Saver saver;
	private Activator activator;

	/**
	 * Class constructor.
	 * @param dbname
	 * @param user
	 * @param pass
	 * @throws OODBLibraryException
	 */
	public OODBLibrary(String dbname, String user, String pass) throws OODBLibraryException{

		this.cpds = new ComboPooledDataSource();
		this.objectMap = new IdentityHashMap<Object, Integer>();
		this.idMap = new HashMap<Identificator, Object>();
		this.classMap = new HashMap<String,String>();
		this.user = user;
		this.pass = pass;
		this.dbname = dbname;
		this.depth = 2;
		connect();
		createIndexTable();
		createColumnIndex();
		this.saver=new Saver(this);
		this.activator=new Activator(this);
	}

	/**
	 * Returns a connection to the database.
	 * @return
	 * @throws SQLException
	 */
	Connection getConnection() throws SQLException {
		Connection c = cpds.getConnection();

		return c;
	}

	/**
	 * Creates the class index table.
	 * @throws OODBLibraryException
	 */
	private void createIndexTable() throws OODBLibraryException{
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
			throw new OODBLibraryException(e);
		}
		

	}

	/**
	 * Creates the column index table.
	 * @throws OODBLibraryException
	 */
	private void createColumnIndex() throws OODBLibraryException {
		String sql = "CREATE TABLE IF NOT EXISTS indicecolumna " 
				+ "(id INTEGER not NULL AUTO_INCREMENT, "
				+ " idtabla INTEGER, " 
				+ " atributo VARCHAR(255)," 
				+ " columna VARCHAR(255),"
				+ " nombrecolumnatipo VARCHAR(255)," + " PRIMARY KEY ( id ),"
				+"  CONSTRAINT fk_table FOREIGN KEY (idtabla) REFERENCES indicetabla(id)) ";
		PreparedStatement pst;
		Connection c;
		try {
			c = this.getConnection();
			pst = c.prepareStatement(sql);
			pst.execute();
			c.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		

	}

	/**
	 * Connects to the database.
	 * @throws OODBLibraryException
	 */
	private void connect()throws OODBLibraryException{

		try {
			this.cpds.setDriverClass("com.mysql.jdbc.Driver");
		} catch (PropertyVetoException e) {
			throw new OODBLibraryException(e);
		}

		this.cpds.setJdbcUrl("jdbc:mysql://localhost/" + this.dbname);
		this.cpds.setUser(this.user);
		this.cpds.setPassword(this.pass);

		this.cpds.setAcquireRetryAttempts(1);
		this.cpds.setAcquireRetryDelay(1);
	}
	
	/**
	 * Returns the table name associated to a class.
	 * @param nombreClase
	 * @return
	 * @throws SQLException
	 */
	String getTableName(String nombreClase) throws SQLException{
		if(classMap.containsKey(nombreClase))
			return classMap.get(nombreClase);
		else{
			Connection con = this.cpds.getConnection();
			String str = "";
			String sqlStatement = "SELECT nombretabla "
					+ "FROM INDICETABLA "
					+ "WHERE nombreclase = \'" 
					+ nombreClase + "\'";
			PreparedStatement pst;
			pst = con.prepareStatement(sqlStatement);
			ResultSet rs = pst.executeQuery();
			rs.next();
			str = rs.getString("nombretabla");
			con.close();
			classMap.put(nombreClase, str);
			return str;
		}
	}
	
	/**
	 * Returns the id of an object.
	 * @param obj
	 * @return
	 */
	int getId(Object obj){
		return this.objectMap.get(obj);
	}

	/**
	 * Saves an object in the database.
	 * If the object has been saved before updates the object.
	 * @param o
	 * @throws OODBLibraryException
	 */
	public void save(Object o) throws OODBLibraryException{
		IdentityHashMap<Object, Integer> im=new IdentityHashMap<Object, Integer>();
		try {
			this.saver.save(o, im);
		} catch (IllegalArgumentException | IllegalAccessException | SQLException e) {
			throw new OODBLibraryException(e);
		}
		this.objectMap.putAll(im);
	}

	/**
	 * Return the non-null attributes of an object.
	 * @param o
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	ArrayList<Attribute> getNonNullAttributes(Object o) throws IllegalArgumentException, IllegalAccessException {
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();

		for (Field n : o.getClass().getDeclaredFields()) {
			n.setAccessible(true);
				if (n.get(o) != null) {
					String type = "";
					if (n.getType().getCanonicalName().contains("Java.Lang.String") || n.getType().getCanonicalName().contains("java.lang.String"))
						type = "VARCHAR(255)";
					else if (n.getType().getCanonicalName().contains("int"))
						type = "INTEGER";
					

					Attribute a = new Attribute(n.getName(), type, false,false);
					attributes.add(a);
				}
		}

		return attributes;

	}
	
	/**
	 * Deletes an object from the database.
	 * @param o
	 * @throws OODBLibraryException
	 */
	public void delete(Object o) throws OODBLibraryException{
		if(!this.objectMap.containsKey(o)){
			throw new OODBLibraryException(new NonExistentObject());
		}
		String tableName;
		Integer id;
		try {
			tableName = this.getTableName(o.getClass().getName());
			String sqlStatement = "DELETE FROM " + tableName +
					  " WHERE ID = ?";
			Connection con = this.cpds.getConnection();
			PreparedStatement pst;
			id = this.objectMap.get(o);
			pst = con.prepareStatement(sqlStatement);
			pst.setObject(1, id);
			pst.execute();
			con.close();
		} catch (SQLException e) {
			throw new OODBLibraryException(e);
		}
		this.objectMap.remove(o);
		this.idMap.remove(o.getClass().getName()+"-"+id);

	}
	
	/**
	 * Executes a given query and retrieves a list of objects created with the default depth.
	 * @param q
	 * @return
	 * @throws OODBLibraryException
	 */
	public List<Object> executeQuery(Query q) throws OODBLibraryException {
		Connection c;
		try {
			c = this.getConnection();
		} catch (SQLException e) {
			throw new OODBLibraryException(e);
		}
		List<Object> list;
		try {
			list = q.executeQuery(c, this.depth);
			c.close();
		} catch (InstantiationException | IllegalAccessException | SQLException | ClassNotFoundException | IllegalArgumentException | NoSuchFieldException | SecurityException e) {
			throw new OODBLibraryException(e);
		}
		
		return list;
	}
	
	/**
	 * Executes a given query and retrieves a list of objects created with the given depth.
	 * @param q
	 * @param profundidad
	 * @return
	 * @throws OODBLibraryException
	 */
	public List<Object> executeQuery(Query q, int profundidad) throws OODBLibraryException {
		Connection c;
		List<Object> list;
		try {
			c = this.getConnection();
			list = q.executeQuery(c, profundidad);
			c.close();
		} catch (SQLException | InstantiationException | IllegalAccessException | ClassNotFoundException | IllegalArgumentException | NoSuchFieldException | SecurityException e) {
			throw new OODBLibraryException(e);
		}
		
		return list;
	}
	
	/**
	 * Activates the null pointers of an object using a given depth.
	 * @param o
	 * @param depth
	 * @throws OODBLibraryException
	 */
	public void activate(Object o, int depth) throws OODBLibraryException{
		try {
			this.activator.activate(o, depth);
		} catch (ClassNotFoundException e) {
			throw new OODBLibraryException(e);
		}
	}
	
	/**
	 * Activates the null pointers of an object using the default depth.
	 * @param o
	 * @throws OODBLibraryException
	 */
	public void activate(Object o) throws OODBLibraryException{
		try {
			this.activator.activate(o, this.depth);
		} catch (ClassNotFoundException e) {
			throw new OODBLibraryException(e);
		}
	}

	/**
	 * Creates a new Query to retrieve objects of a given class.
	 * @param class1
	 * @return
	 */
	public Query newQuery(Class<?> class1) {	
		return new Query(class1, this);
	}
	
	protected boolean containsKeyIdMap(Identificator s){
		return this.idMap.containsKey(s);
	}

	protected Object getIdMap(Identificator s) {
		return this.idMap.get(s);
	}

	protected void putIdMap(Identificator key, Object value) {
		this.idMap.put(key, value);
	}

	protected boolean containsKeyObjectMap(Object o){
		return this.objectMap.containsKey(o);
	}

	protected int getObjectMap(Object s) {
		return this.objectMap.get(s);
	}

	protected void putObjectMap(Object key, Integer value) {
		this.objectMap.put(key, value);
	}
	
	public void setDepth(int depth){
		this.depth = depth;
	}
	
	/**
	 * Retrieves a list of objects from the database that match the given Object o field values.
	 * 0 or Null fields will not be considered as constraints.
	 * @param o
	 * @return
	 * @throws OODBLibraryException
	 */
	public List<Object> queryByExample(Object o) throws OODBLibraryException{
		try {
			return queryByExample(o, new ArrayList<String>());
		} catch (SecurityException e) {
			throw new OODBLibraryException(e);
		}
	}
	
	/**
	 * Retrieves a list of objects from the database that match the given Object o field values.
	 * Uses a given list of fields to consider even though they have 0 or Null values.
	 * @param o
	 * @param notToIgnore
	 * @return
	 * @throws OODBLibraryException
	 */
	public List<Object> queryByExample(Object o, List<String> notToIgnore) throws OODBLibraryException{
		List<Object> l = new ArrayList<Object>();
		List<Constraint> constraintList = new ArrayList<Constraint>();
		Field[] fields = o.getClass().getDeclaredFields();
		for(Field f: fields){
			f.setAccessible(true);
			boolean notIgnoring = false;
			
			if(f.getType().getCanonicalName().contains("int")){
				int n;
				try {
					n = (int)f.get(o);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new OODBLibraryException(e);
				}
				if(n != 0)
					notIgnoring = true;
			}
			else{
				try {
					if(f.get(o) != null)
						notIgnoring = true;
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new OODBLibraryException(e);
				}
			}
			
			if(notToIgnore.contains(f.getName()) || notIgnoring)
				try {
					constraintList.add(SimpleConstraint.newEqualConstraint(f.getName(), f.get(o)));
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new OODBLibraryException(e);
				}
		}
		
		Constraint c = new AndConstraint(constraintList);
		Query q = new Query(o.getClass(), this);
		q.setConstraint(c);
		l = executeQuery(q);
		return l;
	}
	
	/**
	 * Returns true if the given class is a basic Java type.
	 * @param c
	 * @return
	 */
	boolean basicType(Class<?> c){
		return c.getCanonicalName().equalsIgnoreCase("Java.lang.String") 
				|| c.getCanonicalName().equalsIgnoreCase("Int")
				|| c.getCanonicalName().equalsIgnoreCase("Java.lang.Integer");
		
	}

}
