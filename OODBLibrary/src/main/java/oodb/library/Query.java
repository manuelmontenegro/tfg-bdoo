package oodb.library;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import oodb.constraints.AndConstraint;
import oodb.constraints.Constraint;

/**
 * Class that implements the Object load from the database.
 */
public class Query {
	private OODBLibrary library;
	private Class<?> constraintClass;
	private Constraint constraint;

	/**
	 * Class constructor.
	 * @param cl
	 * @param lib
	 */
	Query(Class<?> cl, OODBLibrary lib) {
		constraintClass = cl;
		this.library = lib;
		this.constraint = new AndConstraint();
	}

	/**
	 * Sets the constraint to apply to the Object.
	 * @param c
	 */
	public void setConstraint(Constraint c) {
		this.constraint = c;
	}

	/**
	 * Returns the SQL Statement in String form.
	 * @param con
	 * @return sqlStatement
	 * @throws SQLException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 */
	String toSql(Connection con) throws SQLException, IllegalArgumentException, IllegalAccessException,
			NoSuchFieldException, SecurityException {

		StringBuilder sqlStatement=new StringBuilder("SELECT t1.id,");
		Field[] fields = this.constraintClass.getDeclaredFields();
		List<String> selectFields = new ArrayList<String>();
		for (int i = 0; i < (fields.length); i++) {
			fields[i].setAccessible(true);
			if (!fields[i].getType().isAssignableFrom(List.class) && !fields[i].getType().isAssignableFrom(Set.class)) {
				selectFields.add("t1." + fields[i].getName());
			} else {
				selectFields.add("t1.2_" + fields[i].getName());
			}
		}
		sqlStatement.append(StringUtils.join(selectFields, ","));
		
		sqlStatement.append(" FROM ");
		sqlStatement.append(this.library.getTableName(this.constraintClass.getName()));
		sqlStatement.append(" t1");

		List<String> tableList = new ArrayList<String>();
		List<String> fieldList = new ArrayList<String>();
		List<String> indexList = new ArrayList<String>();

		String constraintSQL = this.constraintToSql(this.constraint, tableList, fieldList, indexList);
		for (int i = 0; i < tableList.size(); i++) {
			sqlStatement.append(" LEFT JOIN ");
			sqlStatement.append(tableList.get(i));
			sqlStatement.append(" t");
			sqlStatement.append((i + 2));
			sqlStatement.append( " ON ");
			sqlStatement.append(indexList.get(i));
			sqlStatement.append(".");
			sqlStatement.append(fieldList.get(i));
			sqlStatement.append(" = t");
			sqlStatement.append((i + 2));
			sqlStatement.append(".id");
		}

		sqlStatement.append(" WHERE ");
		sqlStatement.append(constraintSQL);

		return sqlStatement.toString();
	}

	/**
	 * Returns the SQL form of a given Constraint.
	 * @param c
	 * @param tl
	 * @param fl
	 * @param il
	 * @return String
	 * @throws SQLException
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 */
	private String constraintToSql(Constraint c, List<String> tl, List<String> fl, List<String> il)
			throws SQLException, NoSuchFieldException, SecurityException {


		if (c.getClass().getName().contains("NotConstraint")) {
			return "NOT " + constraintToSql(c.getInnerConstraint().get(0), tl, fl, il);
		} else if (c.getClass().getName().contains("AndConstraint")) {
			List<String> ls = new ArrayList<String>();
			for (Constraint con : c.getInnerConstraint()) {
				ls.add(constraintToSql(con, tl, fl, il));
			}
			return StringUtils.join(ls, " AND ");
		} else if (c.getClass().getName().contains("OrConstraint")) {
			List<String> ls = new ArrayList<String>();
			for (Constraint con : c.getInnerConstraint()) {
				ls.add(constraintToSql(con, tl, fl, il));
			}
			return StringUtils.join(ls, " OR ");
		} else {
			StringBuilder constraint=new StringBuilder();
			String[] campos = StringUtils.split(c.getField(), ".");
			Field currentField = null;
			Class<?> currentClass = this.constraintClass;
			for (String s : campos) {
				currentField = currentClass.getDeclaredField(s);
				currentClass = currentField.getType();
			}
			if (!currentField.getType().isAssignableFrom(List.class)
					&& !currentField.getType().isAssignableFrom(List.class) && !currentField.getType().isArray()) {
				il.add("t1");
				currentField = null;
				currentClass = this.constraintClass;
				for (String s : campos) {
					currentField = currentClass.getDeclaredField(s);
					currentClass = currentField.getType();
					if (!this.library.basicType(currentClass)) {
						tl.add(this.library.getTableName(currentClass.getName()));
						fl.add(currentField.getName());
					}
				}
				for (int i = 1; i < campos.length - 1; i++)
					il.add("t" + (il.size() + 1));
				int index = tl.size() + 1;
				if (!this.library.basicType(currentField.getType()))
					index--;
				constraint=new StringBuilder("t");
				constraint.append(index);
				constraint.append(".");
				constraint.append(currentField.getName());
				constraint.append(" = ?");
			} else {
				Class<?> userClass;
				if (!currentField.getType().isArray()) {
					Type friendsGenericType = currentField.getGenericType();
					ParameterizedType friendsParameterizedType = (ParameterizedType) friendsGenericType;
					Type[] friendsType = friendsParameterizedType.getActualTypeArguments();
					userClass = (Class<?>) friendsType[0];
				} else {
					userClass = currentField.getType().getComponentType();
				}
				constraint.append("EXISTS (SELECT ");
				String classTableName = this.library.getTableName(this.constraintClass.getName());
				StringBuilder listTableName=new StringBuilder();
				listTableName.append(classTableName);
				listTableName.append("_");
				listTableName.append(currentField.getName());
				if (library.basicType(userClass)) {
					constraint.append("ntl.id1_");
					constraint.append(classTableName);
					constraint.append(" FROM ");
					constraint.append(listTableName.toString());
					constraint.append(" ntl WHERE ntl.");
					constraint.append(currentField.getName());
					constraint.append(" = ? AND ntl.id1_");
					constraint.append(classTableName);
					constraint.append(" = t1.id");
				} else {
					String listComponentTableName = this.library.getTableName(userClass.getName());
					constraint.append("ntl.id1_");
					constraint.append(classTableName);
					constraint.append(" FROM ");
					constraint.append(listTableName.toString());
					constraint.append(" ntl JOIN ");
					constraint.append(listComponentTableName);
					constraint.append(" nt ON ntl.id2_");
					constraint.append(listComponentTableName);
					constraint.append(" = nt.id WHERE nt.id = ? AND ntl.id1_");
					constraint.append(classTableName);
					constraint.append(" = t1.id");
				}
				constraint.append(")");
			}

			constraint.append("( ");
			constraint.append(constraint);
			constraint.append(" )");
			return  constraint.toString();
		}
	}
	
	/**
	 * Executes the Query.
	 * @param con
	 * @param profundidad
	 * @return
	 * @throws SQLException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws IllegalArgumentException
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 */
	List<Object> executeQuery(Connection con, int profundidad)
			throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException,
			IllegalArgumentException, NoSuchFieldException, SecurityException {
		ObjectCreator objCrtr = new ObjectCreator(this.library);
		String sql = this.toSql(con);
		List<Object> list = new ArrayList<Object>();
		PreparedStatement pst = con.prepareStatement(sql);
		List<Object> values = this.constraint.getValues();
		for (int i = 1; i <= values.size(); i++) {
			Object obj = values.get(i - 1);
			if (!this.library.basicType(obj.getClass())) {
				if (this.library.containsKeyObjectMap(obj))
					obj = this.library.getId(obj);
				else
					obj = -1;
			}
			pst.setObject(i, obj);
		}

		ResultSet rs = pst.executeQuery();
		Object object;
		while (rs.next()) {
			Identificator iden = new Identificator(rs.getInt("id"), this.constraintClass.getName());
			if (this.library.containsKeyIdMap(iden)) {
				object = this.library.getIdMap(iden);
			} else {
				object = objCrtr.createObject(this.constraintClass, rs, profundidad);
			}
			list.add(object);
		}

		return list;
	}
}
