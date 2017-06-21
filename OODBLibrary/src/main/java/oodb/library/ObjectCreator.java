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

/**
 * Creates an Object using the data retrieved from the database.
 */
class ObjectCreator {

	private OODBLibrary library;

	ObjectCreator(OODBLibrary lib) {
		this.library = lib;
	}

	/**
	 * Creates and return an object using the ResultSet.
	 * @param c
	 * @param rs
	 * @param depth
	 * @return Object
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
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
				StringBuilder sb = new StringBuilder();
				sb.append("2_");
				sb.append(f.getName());
				String listType = rs.getString(sb.toString());
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
						sb = new StringBuilder();
						sb.append("SELECT ");
						sb.append(fieldName);
						sb.append(" FROM ");
						sb.append(multivaluedTableName);
						sb.append(" WHERE id1_");
						sb.append(tableName);
						sb.append(" = ");
						sb.append(idenO.getId());
						sb.append(" ORDER BY posicion");
						Connection con = library.getConnection();
						PreparedStatement pst;
						pst = con.prepareStatement(sb.toString());
						ResultSet rset = pst.executeQuery();
						while (rset.next()) {
							list.add(rset.getObject(fieldName));
						}
						con.close();
					} else {
						String tableName = library.getTableName(c.getName());
						String multivaluedObjectTableName = library.getTableName(userClass.getName());
						String multivaluedTableName = tableName + "_" + f.getName();
						
						StringBuilder selectStatement = new StringBuilder();
						selectStatement.append("id2_");
						selectStatement.append(multivaluedObjectTableName);
						
						StringBuilder whereStatement = new StringBuilder();
						whereStatement.append("id1_");
						whereStatement.append(tableName);
						
						StringBuilder sqlStatement = new StringBuilder();
						sqlStatement.append("SELECT ");
						sqlStatement.append(selectStatement.toString());
						sqlStatement.append("FROM ");
						sqlStatement.append(multivaluedTableName);
						sqlStatement.append(" WHERE ");
						sqlStatement.append(whereStatement.toString());
						sqlStatement.append(" = ");
						sqlStatement.append(idenO.getId());
						sqlStatement.append(" ORDER BY posicion");

						
						Connection con = library.getConnection();
						PreparedStatement pst;
						pst = con.prepareStatement(sqlStatement.toString());
						ResultSet rset = pst.executeQuery();

						while (rset.next()) {
							StringBuilder sqlSttmnt = new StringBuilder();
							sqlSttmnt.append("SELECT * FROM ");
							sqlSttmnt.append(multivaluedObjectTableName);
							sqlSttmnt.append(" WHERE id = ");
							sqlSttmnt.append(rset.getInt(selectStatement.toString()));
							
							
							Connection connect = library.getConnection();
							PreparedStatement pst2;
							pst2 = connect.prepareStatement(sqlSttmnt.toString());
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
				StringBuilder sType = new StringBuilder();
				sType.append("2_");
				sType.append(f.getName());
				String setType = rs.getString(sType.toString());
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
						
						StringBuilder multivaluedTableName = new StringBuilder();
						multivaluedTableName.append(tableName);
						multivaluedTableName.append("_");
						multivaluedTableName.append(fieldName);
						
						StringBuilder sqlStatement = new StringBuilder();
						sqlStatement.append("SELECT ");
						sqlStatement.append(fieldName);
						sqlStatement.append(" FROM ");
						sqlStatement.append(multivaluedTableName.toString());
						sqlStatement.append(" WHERE id1_");
						sqlStatement.append(tableName);
						sqlStatement.append(" = ");
						sqlStatement.append(idenO.getId());
						
						
						Connection con = library.getConnection();
						PreparedStatement pst;
						pst = con.prepareStatement(sqlStatement.toString());
						ResultSet rset = pst.executeQuery();
						while (rset.next()) {
							set.add(rset.getObject(fieldName));
						}
						con.close();
					} else {
						String tableName = library.getTableName(c.getName());
						String multivaluedObjectTableName = library.getTableName(userClass.getName());
						StringBuilder multivaluedTableName = new StringBuilder();
						multivaluedTableName.append(tableName);
						multivaluedTableName.append("_");
						multivaluedTableName.append(f.getName());
						
						StringBuilder selectStatement = new StringBuilder();
						selectStatement.append("id2_");
						selectStatement.append(multivaluedObjectTableName);
						
						StringBuilder whereStatement = new StringBuilder();
						whereStatement.append("id1_");
						whereStatement.append(tableName);
						
						StringBuilder sqlStatement = new StringBuilder();
						sqlStatement.append("SELECT ");
						sqlStatement.append(selectStatement.toString());
						sqlStatement.append(" FROM ");
						sqlStatement.append(multivaluedTableName.toString());
						sqlStatement.append(" WHERE ");
						sqlStatement.append(whereStatement.toString());
						sqlStatement.append(" = ");
						sqlStatement.append(idenO.getId());

						Connection con = library.getConnection();
						PreparedStatement pst;
						pst = con.prepareStatement(sqlStatement.toString());
						ResultSet rset = pst.executeQuery();

						while (rset.next()) {
							StringBuilder sqlSttmnt = new StringBuilder();
							sqlSttmnt.append("SELECT * FROM ");
							sqlSttmnt.append(multivaluedObjectTableName);
							sqlSttmnt.append(" WHERE id = ");
							sqlSttmnt.append(rset.getInt(selectStatement.toString()));
						
							Connection connect = library.getConnection();
							PreparedStatement pst2;
							pst2 = connect.prepareStatement(sqlSttmnt.toString());
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
				StringBuilder atr = new StringBuilder();
				atr.append("2_");
				atr.append(f.getName());
				String atrType = rs.getString(atr.toString());
				if (!atrType.equals("Array")) {
					if (depth == 1)
						field = null;
					if (field != null) {
						Identificator iden = new Identificator((int) field, f.getType().getCanonicalName());
						if (library.containsKeyIdMap(iden)) {
							field = library.getIdMap(iden);
						} else {
							String tn = library.getTableName(f.getType().getCanonicalName());
							StringBuilder sqlStatement = new StringBuilder();
							sqlStatement.append("SELECT * FROM ");
							sqlStatement.append(tn);
							sqlStatement.append(" WHERE ID = ?");
							
							Connection con = library.getConnection();
							PreparedStatement pst;
							pst = con.prepareStatement(sqlStatement.toString());
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
						StringBuilder multivaluedTableName = new StringBuilder();
						multivaluedTableName.append(tableName);
						multivaluedTableName.append("_");
						multivaluedTableName.append(fieldName);
						
						StringBuilder sqlStatement = new StringBuilder();
						sqlStatement.append("SELECT ");
						sqlStatement.append(fieldName);
						sqlStatement.append(" FROM ");
						sqlStatement.append(multivaluedTableName.toString());
						sqlStatement.append(" WHERE id1_");
						sqlStatement.append(tableName);
						sqlStatement.append(" = ");
						sqlStatement.append(idenO.getId());
						sqlStatement.append(" ORDER BY posicion DESC");
						
						
						Connection con = library.getConnection();
						PreparedStatement pst;
						pst = con.prepareStatement(sqlStatement.toString());
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
						
						StringBuilder multivaluedTableName = new StringBuilder();
						multivaluedTableName.append(tableName);
						multivaluedTableName.append("_");
						multivaluedTableName.append(f.getName());
						
						StringBuilder selectStatement = new StringBuilder();
						selectStatement.append("id2_");
						selectStatement.append(multivaluedObjectTableName);
						
						StringBuilder whereStatement = new StringBuilder();
						whereStatement.append("id1_");
						whereStatement.append(tableName);
						
						StringBuilder sqlStatement = new StringBuilder();
						sqlStatement.append("SELECT ");
						sqlStatement.append(selectStatement);
						sqlStatement.append(" FROM ");
						sqlStatement.append(multivaluedTableName.toString());
						sqlStatement.append(" WHERE ");
						sqlStatement.append(whereStatement.toString());
						sqlStatement.append(" = ");
						sqlStatement.append(idenO.getId());
						sqlStatement.append(" ORDER BY posicion DESC");
						
						
						Connection con = library.getConnection();
						PreparedStatement pst;
						pst = con.prepareStatement(sqlStatement.toString());
						ResultSet rset = pst.executeQuery();
						boolean created = false;
						while (rset.next()) {
							if (!created) {
								int size = rset.getInt("posicion");
								field = Array.newInstance(f.getType().getComponentType(), (size + 1));
								created = true;
							}
							
							StringBuilder sqlSttmnt = new StringBuilder();
							sqlSttmnt.append("SELECT * FROM ");
							sqlSttmnt.append(multivaluedObjectTableName);
							sqlSttmnt.append(" WHERE id = ");
							sqlSttmnt.append(rset.getInt(selectStatement.toString()));
							
							Connection connect = library.getConnection();
							PreparedStatement pst2;
							pst2 = connect.prepareStatement(sqlSttmnt.toString());
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
