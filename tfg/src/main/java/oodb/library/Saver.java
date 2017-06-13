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

public class Saver {

	private OODBLibrary lib;

	Saver(OODBLibrary lib) {
		this.lib = lib;
	}

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

			if (!isBasic(f)) {
				if (attribute instanceof List<?> || attribute instanceof Set<?>) // si
																					// es
																					// una
																					// lista
																					// o
																					// set
																					// hay
																					// que
																					// tratarlo
																					// aparte
					emptyMiddleTableById(tableName, f.getName(), id);

				if (attribute != null) {

					if (attribute instanceof List<?> || attribute instanceof Set<?> || attribute.getClass().isArray()) {// si
																														// es
																														// una
																														// lista
																														// o
																														// set
																														// hay
																														// que
																														// tratarlo
																														// aparte

						Class<?> componentType = null;
						if (attribute.getClass().isArray()) {
							componentType = attribute.getClass().getComponentType();
						} else {
							// Aqui sacamos el tipo del parametro
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

	private void emptyTableRow(String tableName, int id) throws SQLException {
		String sql = "UPDATE " + tableName + " SET  WHERE id = ?";
		PreparedStatement pst;
		Connection c = this.lib.getConnection();
		pst = c.prepareStatement(sql);
		pst.setInt(1, id);
		pst.execute();
		c.close();

	}

	private void emptyMiddleTableById(String tableName, String attributeName, int id) throws SQLException {
		String sql = "DELETE FROM " + tableName + "_" + attributeName + " WHERE id1_" + tableName + " = ?";
		PreparedStatement pst;
		Connection c = this.lib.getConnection();
		pst = c.prepareStatement(sql);
		pst.setInt(1, id);
		pst.execute();
		c.close();
	}

	private void insertListObject(String parentTableName, int parentId, String paramTableName, String paramName,
			Integer paramId, int position, String instanceType) throws SQLException {
		Connection c = this.lib.getConnection();

		String sqlInsert = "";
		if (instanceType.equalsIgnoreCase("List") || instanceType.equalsIgnoreCase("Array"))
			sqlInsert = "INSERT INTO " + parentTableName + "_" + paramName + " ( id1_" + parentTableName + ", id2_"
					+ paramTableName + ", posicion) VALUES (?, ?, ?)";
		else
			sqlInsert = "INSERT INTO " + parentTableName + "_" + paramName + " ( id1_" + parentTableName + ", id2_"
					+ paramTableName + ") VALUES (?, ?)";

		PreparedStatement pstI;
		pstI = c.prepareStatement(sqlInsert);
		pstI.setInt(1, parentId);
		pstI.setObject(2, paramId);
		if (instanceType.equalsIgnoreCase("List") || instanceType.equalsIgnoreCase("Array"))
			pstI.setInt(3, position);
		pstI.execute();

		c.close();
	}

	private void insertMultivalued(String parentTableName, String listName, int idPadre, Object element, int position,
			String instanceType) throws SQLException {

		Connection c = this.lib.getConnection();

		String sqlInsert = "";
		if (instanceType.equalsIgnoreCase("List") || instanceType.equalsIgnoreCase("Array"))
			sqlInsert = "INSERT INTO " + parentTableName + "_" + listName + " ( id1_" + parentTableName + ", "
					+ listName + ", posicion) VALUES (?, ?, ?)";
		else
			sqlInsert = "INSERT INTO " + parentTableName + "_" + listName + " ( id1_" + parentTableName + ", "
					+ listName + ") VALUES (?, ?)";

		PreparedStatement pstI;
		pstI = c.prepareStatement(sqlInsert);
		pstI.setInt(1, idPadre);
		pstI.setObject(2, element);
		if (instanceType.equalsIgnoreCase("List") || instanceType.equalsIgnoreCase("Array"))
			pstI.setInt(3, position);
		pstI.execute();

		c.close();
	}

	private int insertEmptyRow(String tableName, ArrayList<Attribute> attributes) throws SQLException {
		int id = 0;
		ArrayList<String> values = new ArrayList<String>();
		String names = "";
		String keys = "";
		boolean first = true;
		for (int i = 0; i < attributes.size(); i++) {
			if (!attributes.get(i).isBasic()) {

				if (!first) {
					keys += " , ";
					names += " , ";
				}
				values.add(attributes.get(i).getConstructorClass());
				keys += " ? ";
				names += "2_" + attributes.get(i).getName();
				first = false;
			}
		}
		String sql = "INSERT INTO " + tableName + " (" + names + ") VALUES (" + keys + ")";
		Connection c = this.lib.getConnection();
		PreparedStatement pst = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
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

	private void update(String tableName, Object o, IdentityHashMap<Object, Integer> im)
			throws SQLException, IllegalArgumentException, IllegalAccessException {
		String keys = "";
		ArrayList<String> values = new ArrayList<String>();

		Field[] fields = o.getClass().getDeclaredFields();
		int tam = o.getClass().getDeclaredFields().length;
		for (int i = 0; i < tam; i++) {
			Field f = fields[i];
			f.setAccessible(true);
			Object attribute = null;

			attribute = f.get(o);

			if (!isBasic(f)) {

				if (attribute != null) {

					if (f.get(o) instanceof List<?>) {

					} else if (f.get(o) instanceof Set<?>) {

					} else if (attribute.getClass().isArray()) {

					} else {
						values.add(im.get(attribute) + "");
						if (i != 0)
							keys += " , ";
						keys += f.getName() + " =?";
					}

				}

			} else {
				values.add(attribute + "");
				if (i != 0)
					keys += " , ";
				keys += f.getName() + " =?";
			}
		}

		String sqlUpdate = "UPDATE " + tableName + " SET " + keys + " WHERE ID = ?";

		Connection con;

		con = this.lib.getConnection();
		PreparedStatement pst = con.prepareStatement(sqlUpdate);
		for (int i = 0; i < values.size(); i++) {
			pst.setObject(i + 1, values.get(i));
		}
		pst.setObject(values.size() + 1, im.get(o));
		pst.execute();
		con.close();
	}

	private boolean isBasic(Field f) {
		boolean ret = false;
		if (f.getType().getCanonicalName().equalsIgnoreCase("Java.lang.String")
				|| f.getType().getCanonicalName().equalsIgnoreCase("Int"))
			ret = true;
		return ret;
	}

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

			String sql1 = "CREATE TABLE IF NOT EXISTS " + tableName
					+ " (id INTEGER not NULL AUTO_INCREMENT, PRIMARY KEY ( id ))";
			Connection c1 = this.lib.getConnection();
			PreparedStatement pst1 = c1.prepareStatement(sql1);

			pst1.execute();
			c1.close();
		}
		c.close();
		return tableName;
	}

	private String insertTableIndex(String className, String almostTableName) throws SQLException {
		int id;
		String tableName;
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

		tableName = id + almostTableName;

		c = this.lib.getConnection();
		sql = "UPDATE indicetabla SET nombretabla=? where id=?";
		PreparedStatement pst = c.prepareStatement(sql);
		pst.setString(1, tableName);
		pst.setInt(2, id);
		pst.executeUpdate();
		c.close();

		return tableName;
	}

	private ArrayList<Attribute> alterTable(String tableName, Object o) throws SQLException {
		ArrayList<Attribute> atributos = getAttributes(o);

		String indexTableId = getIndexTableId(tableName);
		for (Attribute a : atributos) {
			if (!existsColumnIndex(a, indexTableId))
				insertColumnIndex(tableName, a, indexTableId);
		}
		return atributos;

	}

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

	private ArrayList<Attribute> getAttributes(Object o) throws SQLException {
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		for (Field f : o.getClass().getDeclaredFields()) {
			boolean nullObject = false;
			boolean basic = true;
			boolean multivalued = false;
			String name = f.getName();
			String type = "";
			String constructorClass = "";
			if (f.getType().getCanonicalName().equalsIgnoreCase("Java.lang.String"))
				type = "VARCHAR(255)";
			else if (f.getType().getCanonicalName().equalsIgnoreCase("Int"))
				type = "INTEGER";
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
						type = "VARCHAR(255)";
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
							type = "INTEGER, ADD FOREIGN KEY (" + f.getName() + ") REFERENCES " + referencedTableName
									+ "(id) ON DELETE SET NULL"; // El tipo ya
																	// va a ser
																	// una
																	// foreign
																	// key
						}
					}
				} catch (IllegalArgumentException | IllegalAccessException | SecurityException
						| InstantiationException e) {
					e.printStackTrace();
				}
			}
			if (!nullObject) {
				Attribute a = new Attribute(name, type, basic, multivalued);
				if (!basic)
					a.setConstructorClass(constructorClass);
				attributes.add(a);
			}
		}

		return attributes;
	}

	private void createMultivaluedTable(String nto, String fieldName, String type, String instanceType)
			throws SQLException {
		String sql = "CREATE TABLE IF NOT EXISTS " + nto + "_" + fieldName + " (id INTEGER not NULL AUTO_INCREMENT,"
				+ "id1_" + nto + " INTEGER, " + fieldName + " " + type + ", ";

		if (instanceType.equalsIgnoreCase("List") || instanceType.equalsIgnoreCase("Array"))
			sql += " posicion INTEGER, ";

		sql += " PRIMARY KEY ( id )," + " CONSTRAINT fk_" + nto + "_" + fieldName + "_" + fieldName
				+ " FOREIGN KEY (id1_" + nto + ") REFERENCES " + nto + "(id) )";

		Connection c = this.lib.getConnection();
		PreparedStatement pst = c.prepareStatement(sql);
		pst.execute();
		c.close();
	}

	private void createMiddleTable(String nto, String ntp, String paramName, String instanceType) throws SQLException {
		String sql = "CREATE TABLE IF NOT EXISTS " + nto + "_" + paramName + " (id INTEGER not NULL AUTO_INCREMENT,"
				+ "id1_" + nto + " INTEGER, " + "id2_" + ntp + " INTEGER, ";

		if (instanceType.equalsIgnoreCase("List") || instanceType.equalsIgnoreCase("Array"))
			sql += " posicion INTEGER, ";

		sql += " PRIMARY KEY ( id )," + " CONSTRAINT fk1_" + nto + "_" + paramName + " FOREIGN KEY (id1_" + nto
				+ ") REFERENCES " + nto + "(id)," + " CONSTRAINT fk2_" + nto + "_" + paramName + " FOREIGN KEY (id2_"
				+ ntp + ") REFERENCES " + ntp + "(id) )";

		Connection c = this.lib.getConnection();
		PreparedStatement pst = c.prepareStatement(sql);
		pst.execute();
		c.close();
	}

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

	private void insertColumnIndex(String tableName, Attribute a, String indexTableId) throws SQLException {

		String sql1;
		String alter;

		if (a.isBasic()) {
			sql1 = "INSERT INTO indicecolumna (idtabla,atributo,columna) " + " VALUES ( \"" + indexTableId + "\" , \""
					+ a.getName() + "\" , \"" + a.getName() + "\"  )";

			alter = "ALTER TABLE " + tableName + " ADD " + a.getName() + " " + a.getType();
		} else {
			if (a.isMultivalued()) {
				sql1 = "INSERT INTO indicecolumna (idtabla,atributo,nombrecolumnatipo) " + " VALUES ( \"" + indexTableId
						+ "\" , \"" + a.getName() + "\" , \"2_" + a.getName() + "\" )";

				alter = "ALTER TABLE " + tableName + " ADD  2_" + a.getName() + " VARCHAR(255)";
			} else {
				sql1 = "INSERT INTO indicecolumna (idtabla,atributo,columna,nombrecolumnatipo) " + " VALUES ( \""
						+ indexTableId + "\" , \"" + a.getName() + "\" , \"" + a.getName() + "\" , \"2_" + a.getName()
						+ "\" )";

				alter = "ALTER TABLE " + tableName + " ADD " + a.getName() + " " + a.getType() + ", ADD 2_"
						+ a.getName() + " VARCHAR(255)";
			}

		}
		Connection c1 = this.lib.getConnection();
		PreparedStatement pst1 = c1.prepareStatement(sql1);
		pst1.execute();
		c1.close();

		Connection c2 = this.lib.getConnection();
		PreparedStatement pst = c2.prepareStatement(alter);
		pst.execute();

		c2.close();
	}

	private void deleteColumnIndex(String tableName, ArrayList<Attribute> attributes, String indexTableId)
			throws SQLException {
		String sql = "SELECT atributo FROM indicecolumna WHERE idtabla = ?";
		PreparedStatement pst;
		Connection c = this.lib.getConnection();
		pst = c.prepareStatement(sql);

		pst.setString(1, indexTableId);
		ResultSet rs = pst.executeQuery();
		boolean exists = false;
		while (rs.next()) {
			for (Attribute a : attributes) {
				if (rs.getString("atributo").equalsIgnoreCase(a.getName())) {
					exists = true;
				}
			}
			if (!exists) {

				String del = "ALTER TABLE " + tableName + " DROP COLUMN " + rs.getString("atributo");
				c = this.lib.getConnection();
				pst = c.prepareStatement(del);
				pst.execute();
				c.close();

				del = "DELETE FROM indicecolumna WHERE idtabla = ? and atributo = ?";
				c = this.lib.getConnection();
				pst = c.prepareStatement(del);

				pst.setString(1, indexTableId);
				pst.setString(2, rs.getString("atributo"));
				pst.execute();
				c.close();
			}
			exists = false;
		}
		c.close();
	}

}
