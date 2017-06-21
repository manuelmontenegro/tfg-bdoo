package oodb.library;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import com.mysql.jdbc.Statement;

/**
 * Implements the insertion or update process of an object.
 */
class Saver {

	private OODBLibrary lib;

	/**
	 * Class constructor.
	 * 
	 * @param lib
	 */
	Saver(OODBLibrary lib) {
		this.lib = lib;
	}

	/**
	 * Saves the object in the database.
	 * 
	 * @param o
	 * @param im
	 * @throws SQLException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	void save(Object o, IdentityHashMap<Object, Integer> im)
			throws SQLException, IllegalArgumentException, IllegalAccessException {
		int id = -1;
		String tableName;

		tableName = createTable(o);

		if (!im.containsKey(o) && !this.lib.containsKeyObjectMap(o)) {
			ArrayList<Attribute> atributos = alterTable(tableName, o);
			id = insertEmptyRow(tableName, atributos);
		} else {
			if (im.containsKey(o)) {
				id = im.get(o);
			} else {
				id = this.lib.getObjectMap(o);
			}
		}
		im.put(o, id);
		Identificator key = new Identificator(id, o.getClass().getName());
		this.lib.putIdMap(key, o);
		for (Field f : o.getClass().getDeclaredFields()) {

			f.setAccessible(true);
			Object attribute = null;

			attribute = f.get(o);

			if (!lib.basicType(f.getType())) {
				if (attribute instanceof List<?> || attribute instanceof Set<?>)
					emptyMiddleTableById(tableName, f.getName(), id);

				if (attribute != null) {

					if (attribute instanceof List<?> || attribute instanceof Set<?> || attribute.getClass().isArray()) {

						Class<?> componentType = null;
						if (attribute.getClass().isArray()) {
							componentType = attribute.getClass().getComponentType();
						} else {
							Type friendsGenericType = f.getGenericType();
							ParameterizedType friendsParameterizedType = (ParameterizedType) friendsGenericType;
							Type[] friendsType = friendsParameterizedType.getActualTypeArguments();
							componentType = (Class<?>) friendsType[0];
						}

						String instanceType = "";
						if (attribute instanceof List<?>)
							instanceType = "List";
						else if (attribute instanceof Set<?>)
							instanceType = "Set";
						else
							instanceType = "Array";

						int i = 0;
						if (lib.basicType(componentType)) {
							if (instanceType.equalsIgnoreCase("List")) {
								for (Object obj : ((List<?>) attribute)) {
									insertMultivalued(tableName, f.getName(), id, obj, i, instanceType);
									i++;
								}
							} else if (instanceType.equalsIgnoreCase("Set")) {
								for (Object obj : ((Set<?>) attribute)) {
									insertMultivalued(tableName, f.getName(), id, obj, i, instanceType);
									i++;
								}
							} else {
								int length = Array.getLength(attribute);
								for (i = 0; i < length; i++) {
									insertMultivalued(tableName, f.getName(), id, Array.get(attribute, i), i,
											instanceType);
								}

							}
						} else {
							if (instanceType.equalsIgnoreCase("List")) {
								for (Object field : ((List<?>) attribute)) {
									if (!im.containsKey(field)) {
										save(field, im);
									}
									insertListObject(tableName, id, lib.getTableName(field.getClass().getName()),
											f.getName(), im.get(field), i, instanceType);
									i++;
								}
							} else if (instanceType.equalsIgnoreCase("Set")) {
								for (Object field : ((Set<?>) attribute)) {
									if (!im.containsKey(field)) {
										save(field, im);
									}
									insertListObject(tableName, id, lib.getTableName(field.getClass().getName()),
											f.getName(), im.get(field), i, instanceType);
									i++;
								}
							} else {

							}
						}

					} else {
						if (!im.containsKey(attribute)) {
							save(attribute, im);
						}
					}

				}
			}
		}
		update(tableName, o, im);
	}

	/**
	 * Empties a middle table.
	 * 
	 * @param tableName
	 * @param attributeName
	 * @param id
	 * @throws SQLException
	 */
	private void emptyMiddleTableById(String tableName, String attributeName, int id) throws SQLException {
		StringBuilder sql = new StringBuilder("DELETE FROM ");
		sql.append(tableName);
		sql.append("_");
		sql.append(attributeName);
		sql.append(" WHERE id1_");
		sql.append(tableName);
		sql.append(" = ?");
		
		PreparedStatement pst;
		Connection c = this.lib.getConnection();
		pst = c.prepareStatement(sql.toString());
		pst.setInt(1, id);
		pst.execute();
		c.close();
	}

	/**
	 * Inserts an Object from a list.
	 * 
	 * @param parentTableName
	 * @param parentId
	 * @param paramTableName
	 * @param paramName
	 * @param paramId
	 * @param position
	 * @param instanceType
	 * @throws SQLException
	 */
	private void insertListObject(String parentTableName, int parentId, String paramTableName, String paramName,
			Integer paramId, int position, String instanceType) throws SQLException {
		Connection c = this.lib.getConnection();

		StringBuilder sqlInsert = new StringBuilder();
		if (instanceType.equalsIgnoreCase("List") || instanceType.equalsIgnoreCase("Array")){
			sqlInsert.append("INSERT INTO ");
			sqlInsert.append(parentTableName);
			sqlInsert.append("_");
			sqlInsert.append(paramName);
			sqlInsert.append(" ( id1_");
			sqlInsert.append(parentTableName);
			sqlInsert.append(", id2_");
			sqlInsert.append(paramTableName);
			sqlInsert.append(", posicion) VALUES (?, ?, ?)");
		}
		else{
			sqlInsert.append("INSERT INTO ");
			sqlInsert.append(parentTableName);
			sqlInsert.append( "_");
			sqlInsert.append(paramName);
			sqlInsert.append(" ( id1_");
			sqlInsert.append(parentTableName);
			sqlInsert.append(", id2_");
			sqlInsert.append(paramTableName);
			sqlInsert.append(") VALUES (?, ?)");
		}

		PreparedStatement pstI;
		pstI = c.prepareStatement(sqlInsert.toString());
		pstI.setInt(1, parentId);
		pstI.setObject(2, paramId);
		if (instanceType.equalsIgnoreCase("List") || instanceType.equalsIgnoreCase("Array"))
			pstI.setInt(3, position);
		pstI.execute();

		c.close();
	}

	/**
	 * Inserts a multivalued object.
	 * 
	 * @param parentTableName
	 * @param listName
	 * @param idPadre
	 * @param element
	 * @param position
	 * @param instanceType
	 * @throws SQLException
	 */
	private void insertMultivalued(String parentTableName, String listName, int idPadre, Object element, int position,
			String instanceType) throws SQLException {

		Connection c = this.lib.getConnection();

		StringBuilder sqlInsert = new StringBuilder();
		if (instanceType.equalsIgnoreCase("List") || instanceType.equalsIgnoreCase("Array")){
			sqlInsert.append("INSERT INTO ");
			sqlInsert.append(parentTableName);
			sqlInsert.append("_");
			sqlInsert.append(listName);
			sqlInsert.append(" ( id1_");
			sqlInsert.append(parentTableName);
			sqlInsert.append(", ");
			sqlInsert.append(listName);
			sqlInsert.append(", posicion) VALUES (?, ?, ?)");
		}
		else{
			sqlInsert.append("INSERT INTO ");
			sqlInsert.append(parentTableName);
			sqlInsert.append("_");
			sqlInsert.append(listName);
			sqlInsert.append(" ( id1_");
			sqlInsert.append(parentTableName);
			sqlInsert.append(", ");
			sqlInsert.append(listName);
			sqlInsert.append(") VALUES (?, ?)");
		}

		PreparedStatement pstI;
		pstI = c.prepareStatement(sqlInsert.toString());
		pstI.setInt(1, idPadre);
		pstI.setObject(2, element);
		if (instanceType.equalsIgnoreCase("List") || instanceType.equalsIgnoreCase("Array"))
			pstI.setInt(3, position);
		pstI.execute();

		c.close();
	}

	/**
	 * 
	 * @param tableName
	 * @param attributes
	 * @return
	 * @throws SQLException
	 */
	private int insertEmptyRow(String tableName, ArrayList<Attribute> attributes) throws SQLException {
		int id = 0;
		ArrayList<String> values = new ArrayList<String>();
		StringBuilder names = new StringBuilder();
		StringBuilder keys = new StringBuilder();
		boolean first = true;
		for (int i = 0; i < attributes.size(); i++) {
			if (!attributes.get(i).isBasic()) {

				if (!first) {
					keys.append(" , ");
					names.append(" , ");
				}
				values.add(attributes.get(i).getConstructorClass());
				keys.append(" ? ");
				names.append("2_");
				names.append(attributes.get(i).getName());
				first = false;
			}
		}
		StringBuilder sql = new StringBuilder("INSERT INTO ");
		sql.append(tableName);
		sql.append( " (");
		sql.append(names);
		sql.append(") VALUES (");
		sql.append(keys);
		sql.append(")");

		Connection c = this.lib.getConnection();
		PreparedStatement pst = c.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS);
		int i = 1;
		for (String valor : values) {
			pst.setString(i, valor);
			i++;
		}

		pst.executeUpdate();
		ResultSet rs = pst.getGeneratedKeys();
		if (rs.next())
			id = rs.getInt(1);

		c.close();
		return id;
	}

	/**
	 * Updates an Object.
	 * 
	 * @param tableName
	 * @param o
	 * @param im
	 * @throws SQLException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	private void update(String tableName, Object o, IdentityHashMap<Object, Integer> im)
			throws SQLException, IllegalArgumentException, IllegalAccessException {
		StringBuilder keys = new StringBuilder();
		ArrayList<String> values = new ArrayList<String>();

		Field[] fields = o.getClass().getDeclaredFields();
		int tam = o.getClass().getDeclaredFields().length;
		for (int i = 0; i < tam; i++) {
			Field f = fields[i];
			f.setAccessible(true);
			Object attribute = null;

			attribute = f.get(o);

			if (!lib.basicType(f.getType())) {

				if (attribute != null) {

					if (f.get(o) instanceof List<?>) {

					} else if (f.get(o) instanceof Set<?>) {

					} else if (attribute.getClass().isArray()) {

					} else {
						values.add(im.get(attribute) + "");
						if (i != 0)
							keys.append(" , ");
						keys.append(f.getName());
						keys.append(" =?");
					}

				}

			} else {
				values.add(attribute + "");
				if (i != 0)
					keys.append(" , ");
				keys.append(f.getName());
				keys.append(" =?");
			}
		}

		StringBuilder sqlUpdate = new StringBuilder("UPDATE ");
		sqlUpdate.append(tableName);
		sqlUpdate.append(" SET ");
		sqlUpdate.append(keys);
		sqlUpdate.append(" WHERE ID = ?");

		Connection con;

		con = this.lib.getConnection();
		PreparedStatement pst = con.prepareStatement(sqlUpdate.toString());
		for (int i = 0; i < values.size(); i++) {
			pst.setObject(i + 1, values.get(i));
		}
		pst.setObject(values.size() + 1, im.get(o));
		pst.execute();
		con.close();
	}

	/**
	 * 
	 * @param o
	 * @return
	 * @throws SQLException
	 */
	private String createTable(Object o) throws SQLException {
		String tableName = "";
		String sql = "SELECT nombreclase,nombretabla FROM indicetabla WHERE nombreclase = ?";
		PreparedStatement pst;
		Connection c = this.lib.getConnection();
		pst = c.prepareStatement(sql);
		pst.setString(1, o.getClass().getName());
		ResultSet rs = pst.executeQuery();
		if (rs.next()) {// ya esta creada, se consulta su nombre
			tableName = rs.getString("nombretabla");
		} else {
			tableName = insertTableIndex(o.getClass().getName(), o.getClass().getSimpleName());

			StringBuilder sql1 = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
			sql1.append(tableName);
			sql1.append(" (id INTEGER not NULL AUTO_INCREMENT, PRIMARY KEY ( id ))");

			Connection c1 = this.lib.getConnection();
			PreparedStatement pst1 = c1.prepareStatement(sql1.toString());

			pst1.execute();
			c1.close();
		}
		c.close();
		return tableName;
	}

	/**
	 * 
	 * @param className
	 * @param almostTableName
	 * @return
	 * @throws SQLException
	 */
	private String insertTableIndex(String className, String almostTableName) throws SQLException {
		int id;
		StringBuilder tableName = new StringBuilder();
		Connection c = this.lib.getConnection();
		String sql = "INSERT INTO indicetabla (nombreclase) VALUES (?)";

		PreparedStatement statement = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		statement.setString(1, className);
		int affectedRows = statement.executeUpdate();
		if (affectedRows == 0) {
			throw new SQLException("Creating index failed, no rows affected.");
		}

		ResultSet generatedKeys = statement.getGeneratedKeys();
		if (generatedKeys.next()) {
			id = generatedKeys.getInt(1);
		} else {
			throw new SQLException("Creating index failed, no ID obtained.");
		}
		c.close();

		tableName.append(id);
		tableName.append(almostTableName);

		c = this.lib.getConnection();
		sql = "UPDATE indicetabla SET nombretabla=? where id=?";
		PreparedStatement pst = c.prepareStatement(sql);
		pst.setString(1, tableName.toString());
		pst.setInt(2, id);
		pst.executeUpdate();
		c.close();

		return tableName.toString();
	}

	/**
	 * 
	 * @param tableName
	 * @param o
	 * @return
	 * @throws SQLException
	 */
	private ArrayList<Attribute> alterTable(String tableName, Object o) throws SQLException {
		ArrayList<Attribute> atributos = getAttributes(o);

		String indexTableId = getIndexTableId(tableName);
		for (Attribute a : atributos) {
			if (!existsColumnIndex(a, indexTableId))
				insertColumnIndex(tableName, a, indexTableId);
		}
		return atributos;

	}

	/**
	 * 
	 * @param a
	 * @param indexTableId
	 * @return
	 * @throws SQLException
	 */
	private boolean existsColumnIndex(Attribute a, String indexTableId) throws SQLException {
		String sql = "SELECT id FROM indicecolumna WHERE idtabla = ? and atributo = ?";

		Connection c = this.lib.getConnection();
		PreparedStatement pst = c.prepareStatement(sql);
		pst.setString(1, indexTableId);

		pst.setString(2, a.getName());

		ResultSet rs = pst.executeQuery();

		boolean ret = rs.next();
		c.close();
		return ret;
	}

	/**
	 * 
	 * @param o
	 * @return
	 * @throws SQLException
	 */
	private ArrayList<Attribute> getAttributes(Object o) throws SQLException {
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		for (Field f : o.getClass().getDeclaredFields()) {
			boolean nullObject = false;
			boolean basic = true;
			boolean multivalued = false;
			String name = f.getName();
			StringBuilder type=new StringBuilder();
			String constructorClass = "";
			if (f.getType().getCanonicalName().equalsIgnoreCase("Java.lang.String"))
				type = new StringBuilder("VARCHAR(255)");
			else if (f.getType().getCanonicalName().equalsIgnoreCase("Int"))
				type = new StringBuilder("INTEGER");
			else {
				f.setAccessible(true);
				Object ob = null;
				try {
					ob = f.get(o);
				} catch (IllegalArgumentException | IllegalAccessException e1) {

				}
				try {
					if (ob != null && (ob instanceof Set<?> || ob instanceof List<?> || ob.getClass().isArray())) {

						Class<?> componentType = null;
						if (ob.getClass().isArray()) {
							componentType = ob.getClass().getComponentType();
						} else {
							Type friendsGenericType = f.getGenericType();
							ParameterizedType friendsParameterizedType = (ParameterizedType) friendsGenericType;
							Type[] friendsType = friendsParameterizedType.getActualTypeArguments();
							componentType = (Class<?>) friendsType[0];
						}

						String objectTableName = lib.getTableName(o.getClass().getName());
						String instanceType = "";
						if (ob instanceof List<?>)
							instanceType = "List";
						else if (ob instanceof Set<?>)
							instanceType = "Set";
						else
							instanceType = "Array";

						if (componentType.getName().equalsIgnoreCase("Java.lang.String")) {
							createMultivaluedTable(objectTableName, f.getName(), "VARCHAR(255)", instanceType);
						} else if (componentType.getName().equalsIgnoreCase("Java.lang.Integer")
								|| componentType.getName().equalsIgnoreCase("int")) {
							createMultivaluedTable(objectTableName, f.getName(), "INTEGER", instanceType);
						} else {
							Object param = componentType.newInstance();
							String nombreTablaParametro = createTable(param);
							createMiddleTable(objectTableName, nombreTablaParametro, f.getName(), instanceType);
						}
						name = f.getName();
						type = new StringBuilder("VARCHAR(255)");								
						basic = false;
						multivalued = true;
						if (instanceType.equalsIgnoreCase("Array"))
							constructorClass = "Array";
						else
							constructorClass = ob.getClass().getName();
					} else {

						if (ob == null) {
							nullObject = true;
						} else {
							String referencedTableName = createTable(ob);
							basic = false;
							name = f.getName();
							constructorClass = ob.getClass().getName();
							type = new StringBuilder("INTEGER, ADD FOREIGN KEY (");
							type.append(f.getName());
							type.append(") REFERENCES ");							
							type.append(referencedTableName);
							type.append("(id) ON DELETE SET NULL");
						}
					}
				} catch (IllegalArgumentException | IllegalAccessException | SecurityException
						| InstantiationException e) {
					e.printStackTrace();
				}
			}
			if (!nullObject) {
				Attribute a = new Attribute(name, type.toString(), basic, multivalued);
				if (!basic)
					a.setConstructorClass(constructorClass);
				attributes.add(a);
			}
		}

		return attributes;		
	}

	/**
	 * 
	 * @param nto
	 * @param fieldName
	 * @param type
	 * @param instanceType
	 * @throws SQLException
	 */
	private void createMultivaluedTable(String nto, String fieldName, String type, String instanceType)
			throws SQLException {
		StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
		sql.append(nto);
		sql.append("_");
		sql.append(fieldName);
		sql.append(" (id INTEGER not NULL AUTO_INCREMENT,");
		sql.append("id1_");
		sql.append(nto);
		sql.append(" INTEGER, ");
		sql.append(fieldName);
		sql.append(" " );
		sql.append(type);
		sql.append(", " );

		if (instanceType.equalsIgnoreCase("List") || instanceType.equalsIgnoreCase("Array"))
			sql.append(" posicion INTEGER, ");
			
		sql.append(" PRIMARY KEY ( id ), CONSTRAINT fk_");
		sql.append(nto);
		sql.append("_");
		sql.append(fieldName);
		sql.append("_");
		sql.append(fieldName);
		sql.append(" FOREIGN KEY (id1_");
		sql.append(nto);
		sql.append(") REFERENCES ");
		sql.append(nto);
		sql.append("(id) )");

		Connection c = this.lib.getConnection();
		PreparedStatement pst = c.prepareStatement(sql.toString());
		pst.execute();
		c.close();
	}

	/**
	 * Creates a middle table class.
	 * 
	 * @param nto
	 * @param ntp
	 * @param paramName
	 * @param instanceType
	 * @throws SQLException
	 */
	private void createMiddleTable(String nto, String ntp, String paramName, String instanceType) throws SQLException {
		StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
		sql.append(nto);
		sql.append("_");
		sql.append(paramName);
		sql.append(" (id INTEGER not NULL AUTO_INCREMENT, id1_");
		sql.append(nto);
		sql.append(" INTEGER, id2_");
		sql.append(ntp);
		sql.append(" INTEGER, ");

		if (instanceType.equalsIgnoreCase("List") || instanceType.equalsIgnoreCase("Array"))
			sql.append(" posicion INTEGER, ");

		sql.append(" PRIMARY KEY ( id ), CONSTRAINT fk1_");
		sql.append(nto);
		sql.append("_");
		sql.append(paramName);
		sql.append(" FOREIGN KEY (id1_");
		sql.append(nto);
		sql.append(") REFERENCES ");
		sql.append(nto);
		sql.append("(id), CONSTRAINT fk2_");
		sql.append(nto);
		sql.append("_");
		sql.append(paramName);
		sql.append(" FOREIGN KEY (id2_");
		sql.append(ntp);
		sql.append(") REFERENCES ");
		sql.append(ntp);
		sql.append("(id) )");

		Connection c = this.lib.getConnection();
		PreparedStatement pst = c.prepareStatement(sql.toString());
		pst.execute();
		c.close();
	}

	/**
	 * Returns the index of a given table name.
	 * 
	 * @param tableName
	 * @return
	 * @throws SQLException
	 */
	private String getIndexTableId(String tableName) throws SQLException {
		String id = "";
		String sql = "SELECT id FROM indicetabla WHERE nombretabla = ? ";
		PreparedStatement pst;
		Connection c = this.lib.getConnection();
		pst = c.prepareStatement(sql);
		pst.setString(1, tableName);
		ResultSet rs = pst.executeQuery();
		if (rs.next()) {
			id = rs.getString("id");
		}
		c.close();

		return id;
	}

	/**
	 * Inserts an attribute in the column index.
	 * 
	 * @param tableName
	 * @param a
	 * @param indexTableId
	 * @throws SQLException
	 */
	private void insertColumnIndex(String tableName, Attribute a, String indexTableId) throws SQLException {

		StringBuilder sql1;
		StringBuilder alter;

		if (a.isBasic()) {
			sql1 = new StringBuilder("INSERT INTO indicecolumna (idtabla,atributo,columna) VALUES ( \"");
			sql1.append(indexTableId);
			sql1.append("\" , \"");
			sql1.append(a.getName());
			sql1.append("\" , \"");
			sql1.append( a.getName());
			sql1.append("\"  )");
			
			alter=new StringBuilder("ALTER TABLE ");
			alter.append(tableName);
			alter.append(" ADD ");
			alter.append(a.getName());
			alter.append(" ");			
			alter.append(a.getType());
		} else {
			if (a.isMultivalued()) {
				sql1 = new StringBuilder("INSERT INTO indicecolumna (idtabla,atributo,nombrecolumnatipo) VALUES ( \"");
				sql1.append(indexTableId);
				sql1.append("\" , \"");
				sql1.append( a.getName());
				sql1.append( "\" , \"2_");
				sql1.append(a.getName());
				sql1.append("\" )");
				
				alter=new StringBuilder("ALTER TABLE ");
				alter.append(tableName);
				alter.append(" ADD  2_");
				alter.append(a.getName());
				alter.append(" VARCHAR(255)");
			} else {
				sql1 = new StringBuilder("INSERT INTO indicecolumna (idtabla,atributo,nombrecolumnatipo) VALUES ( \"");
				sql1.append(indexTableId);				
				sql1.append("\" , \"");
				sql1.append(a.getName());
				sql1.append("\" , \"");
				sql1.append(a.getName());
				sql1.append("\" , \"2_");				
				sql1.append(a.getName());
				sql1.append("\" )");

				alter=new StringBuilder("ALTER TABLE ");
				alter.append(tableName);
				alter.append(" ADD ");
				alter.append(a.getName());
				alter.append(" ");
				alter.append(a.getType());
				alter.append(", ADD 2_");
				alter.append(a.getName());
				alter.append(" VARCHAR(255)");
			}

		}
		Connection c1 = this.lib.getConnection();
		PreparedStatement pst1 = c1.prepareStatement(sql1.toString());
		pst1.execute();
		c1.close();

		Connection c2 = this.lib.getConnection();
		PreparedStatement pst = c2.prepareStatement(alter.toString());
		pst.execute();

		c2.close();
	}

}
