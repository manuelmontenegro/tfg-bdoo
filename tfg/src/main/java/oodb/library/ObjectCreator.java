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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class ObjectCreator {

	private OODBLibrary library;

	ObjectCreator(OODBLibrary lib) {
		this.library = lib;
	}

	Object createObject(Class<?> c, ResultSet rs, int depth)
			throws InstantiationException, IllegalAccessException, SQLException, ClassNotFoundException {
		String className = c.getName();
		Object o = Class.forName(className).newInstance();
		Identificator idenO = new Identificator((int) rs.getInt("id"), c.getCanonicalName());
		library.putIdMap(idenO, o);
		library.putObjectMap(o, (int) rs.getInt("id"));
		Field[] fields = o.getClass().getDeclaredFields();
		for (Field f : fields) {
			Object field = null;
			if (!f.getType().isAssignableFrom(List.class) && !f.getType().isAssignableFrom(Set.class))
				field = rs.getObject(f.getName());
			f.setAccessible(true);

			if (f.getType().isAssignableFrom(List.class)) {
				List<Object> list = null;
				String listType = rs.getString("2_" + f.getName());
				if (listType != null) {
					Class<?> cl = Class.forName(listType);
					if (listType.contains("ArrayList")) {
						list = (ArrayList<Object>) cl.newInstance();
					} else if (listType.contains("LinkedList")) {
						list = (LinkedList<Object>) cl.newInstance();
					}

					Type friendsGenericType = f.getGenericType();
					ParameterizedType friendsParameterizedType = (ParameterizedType) friendsGenericType;
					Type[] friendsType = friendsParameterizedType.getActualTypeArguments();
					Class<?> userClass = (Class<?>) friendsType[0];

					if (library.basicType(userClass)) {
						String fieldName = f.getName();
						String tableName = library.getTableName(c.getName());
						String multivaluedTableName = tableName + "_" + fieldName;
						String sqlStatement = "SELECT " + fieldName + " FROM " + multivaluedTableName + " WHERE id1_"
								+ tableName + " = " + idenO.getId() + " ORDER BY posicion";
						Connection con = library.getConnection();
						PreparedStatement pst;
						pst = con.prepareStatement(sqlStatement);
						ResultSet rset = pst.executeQuery();
						while (rset.next()) {
							list.add(rset.getObject(fieldName));
						}
						con.close();
					} else {
						String tableName = library.getTableName(c.getName());
						String multivaluedObjectTableName = library.getTableName(userClass.getName());
						String multivaluedTableName = tableName + "_" + f.getName();
						String selectStatement = "id2_" + multivaluedObjectTableName;
						String whereStatement = "id1_" + tableName;

						String sqlStatement = "SELECT " + selectStatement + " FROM " + multivaluedTableName + " WHERE "
								+ whereStatement + " = " + idenO.getId() + " ORDER BY posicion";
						Connection con = library.getConnection();
						PreparedStatement pst;
						pst = con.prepareStatement(sqlStatement);
						ResultSet rset = pst.executeQuery();

						while (rset.next()) {
							String sqlSttmnt = "SELECT * FROM " + multivaluedObjectTableName + " WHERE id = "
									+ rset.getInt(selectStatement);
							Connection connect = library.getConnection();
							PreparedStatement pst2;
							pst2 = connect.prepareStatement(sqlSttmnt);
							ResultSet rs2 = pst2.executeQuery();
							Object obj = userClass.newInstance();
							rs2.next();
							obj = createObject(userClass, rs2, depth - 1);
							list.add(obj);
							connect.close();
						}
						con.close();
					}

				}
				field = list;
			} else if (f.getType().isAssignableFrom(Set.class)) {
				Set<Object> set = null;
				String setType = rs.getString("2_" + f.getName());
				if (setType != null) {
					Class cl = Class.forName(setType);
					if (setType.contains("HashSet")) {
						set = (HashSet<Object>) cl.newInstance();
					} else if (setType.contains("LinkedHashSet")) {
						set = (LinkedHashSet<Object>) cl.newInstance();
					} else if (setType.contains("TreeSet")) {
						set = (TreeSet<Object>) cl.newInstance();
					}

					Type friendsGenericType = f.getGenericType();
					ParameterizedType friendsParameterizedType = (ParameterizedType) friendsGenericType;
					Type[] friendsType = friendsParameterizedType.getActualTypeArguments();
					Class<?> userClass = (Class<?>) friendsType[0];

					if (library.basicType(userClass)) {
						String fieldName = f.getName();
						String tableName = library.getTableName(c.getName());
						String multivaluedTableName = tableName + "_" + fieldName;
						String sqlStatement = "SELECT " + fieldName + " FROM " + multivaluedTableName + " WHERE id1_"
								+ tableName + " = " + idenO.getId();
						Connection con = library.getConnection();
						PreparedStatement pst;
						pst = con.prepareStatement(sqlStatement);
						ResultSet rset = pst.executeQuery();
						while (rset.next()) {
							set.add(rset.getObject(fieldName));
						}
						con.close();
					} else {
						String tableName = library.getTableName(c.getName());
						String multivaluedObjectTableName = library.getTableName(userClass.getName());
						String multivaluedTableName = tableName + "_" + f.getName();
						String selectStatement = "id2_" + multivaluedObjectTableName;
						String whereStatement = "id1_" + tableName;

						String sqlStatement = "SELECT " + selectStatement + " FROM " + multivaluedTableName + " WHERE "
								+ whereStatement + " = " + idenO.getId();
						Connection con = library.getConnection();
						PreparedStatement pst;
						pst = con.prepareStatement(sqlStatement);
						ResultSet rset = pst.executeQuery();

						while (rset.next()) {
							String sqlSttmnt = "SELECT * FROM " + multivaluedObjectTableName + " WHERE id = "
									+ rset.getInt(selectStatement);
							Connection connect = library.getConnection();
							PreparedStatement pst2;
							pst2 = connect.prepareStatement(sqlSttmnt);
							ResultSet rs2 = pst2.executeQuery();
							Object obj = userClass.newInstance();
							rs2.next();
							obj = createObject(userClass, rs2, depth - 1);
							set.add(obj);
							connect.close();
						}
						con.close();
					}

				}
				field = set;
			} else if (!library.basicType(f.getType())) {
				String atrType = rs.getString("2_" + f.getName());
				if (!atrType.equals("Array")) {
					if (depth == 1)
						field = null;
					if (field != null) {
						Identificator iden = new Identificator((int) field, f.getType().getCanonicalName());
						if (library.containsKeyIdMap(iden)) {
							field = library.getIdMap(iden);
						} else {
							String tn = library.getTableName(f.getType().getCanonicalName());
							String sqlStatement = "SELECT * FROM " + tn + " WHERE ID = ?";
							Connection con = library.getConnection();
							PreparedStatement pst;
							pst = con.prepareStatement(sqlStatement);
							pst.setInt(1, (int) field);
							ResultSet rset = pst.executeQuery();
							if (rset.next()) {
								field = createObject(f.getType(), rset, depth - 1);
							} else
								field = null;
							con.close();
						}
					}
				} else {
					if (library.basicType(f.getType().getComponentType())) {
						String fieldName = f.getName();
						String tableName = library.getTableName(c.getName());
						String multivaluedTableName = tableName + "_" + fieldName;
						String sqlStatement = "SELECT " + fieldName + " FROM " + multivaluedTableName + " WHERE id1_"
								+ tableName + " = " + idenO.getId() + " ORDER BY posicion DESC";
						Connection con = library.getConnection();
						PreparedStatement pst;
						pst = con.prepareStatement(sqlStatement);
						ResultSet rset = pst.executeQuery();
						boolean created = false;
						while (rset.next()) {
							if (!created) {
								int size = rset.getInt("posicion");
								field = Array.newInstance(f.getType().getComponentType(), (size + 1));
								created = true;
							}
							Array.set(field, (int) rset.getObject("posicion"), rset.getObject(fieldName));
						}
						con.close();
					} else {
						String tableName = library.getTableName(c.getName());
						String multivaluedObjectTableName = library.getTableName(f.getType().getComponentType().getName());
						String multivaluedTableName = tableName + "_" + f.getName();
						String selectStatement = "id2_" + multivaluedObjectTableName;
						String whereStatement = "id1_" + tableName;

						String sqlStatement = "SELECT " + selectStatement + " FROM " + multivaluedTableName + " WHERE "
								+ whereStatement + " = " + idenO.getId() + " ORDER BY posicion DESC";
						Connection con = library.getConnection();
						PreparedStatement pst;
						pst = con.prepareStatement(sqlStatement);
						ResultSet rset = pst.executeQuery();
						boolean created = false;
						while (rset.next()) {
							if (!created) {
								int size = rset.getInt("posicion");
								field = Array.newInstance(f.getType().getComponentType(), (size + 1));
								created = true;
							}
							String sqlSttmnt = "SELECT * FROM " + multivaluedObjectTableName + " WHERE id = "
									+ rset.getInt(selectStatement);
							Connection connect = library.getConnection();
							PreparedStatement pst2;
							pst2 = connect.prepareStatement(sqlSttmnt);
							ResultSet rs2 = pst2.executeQuery();
							Object obj = f.getType().getComponentType().newInstance();
							rs2.next();
							obj = createObject(f.getType().getComponentType(), rs2, depth - 1);
							Array.set(field, (int) rset.getObject("posicion"), obj);
							connect.close();
						}
						con.close();
					}

				}
			}
			f.set(o, field);
		}
		return o;
	}

}
