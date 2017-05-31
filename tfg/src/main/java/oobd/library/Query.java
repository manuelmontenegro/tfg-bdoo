package oobd.library;

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

import oobd.constraints.AndConstraint;
import oobd.constraints.Constraint;

public class Query {
	private OOBDLibrary library;
	private Class<?> constraintClass;
	private Constraint constraint;

	Query(Class<?> cl, OOBDLibrary lib) {
		constraintClass = cl;
		this.library = lib;
		this.constraint = new AndConstraint();
	}

	public void setConstraint(Constraint c) {
		this.constraint = c;
	}

	String toSql(Connection con) throws SQLException, IllegalArgumentException, IllegalAccessException,
			NoSuchFieldException, SecurityException {

		String sqlStatement = "SELECT t1.id,";
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
		sqlStatement += StringUtils.join(selectFields, ",");

		sqlStatement += " FROM " + this.library.getTableName(this.constraintClass.getName()) + " t1";
		List<String> tableList = new ArrayList<String>();
		List<String> fieldList = new ArrayList<String>();
		List<String> indexList = new ArrayList<String>();

		String constraintSQL = this.constraintToSql(this.constraint, tableList, fieldList, indexList);
		for (int i = 0; i < tableList.size(); i++) {
			sqlStatement += " LEFT JOIN " + tableList.get(i) + " t" + (i + 2) + " ON " + indexList.get(i) + "."
					+ fieldList.get(i) + " = " + "t" + (i + 2) + ".id";
		}

		sqlStatement += " WHERE " + constraintSQL;

		return sqlStatement;
	}

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
			String constraint = "";
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
					if (!this.library.atributoBasico(currentClass)) {
						tl.add(this.library.getTableName(currentClass.getName()));
						fl.add(currentField.getName());
					}
				}
				for (int i = 1; i < campos.length - 1; i++)
					il.add("t" + (il.size() + 1));
				int index = tl.size() + 1;
				if (!this.library.atributoBasico(currentField.getType()))
					index--;
				constraint = "t" + index + "." + currentField.getName() + " = ?";
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
				constraint += "EXISTS (SELECT ";
				String classTableName = this.library.getTableName(this.constraintClass.getName());
				String listTableName = classTableName + "_" + currentField.getName();
				if (library.atributoBasico(userClass)) {
					constraint += "ntl.id1_" + classTableName;
					constraint += " FROM " + listTableName + " ntl WHERE " + " ntl." + currentField.getName()
							+ " = ? AND ntl.id1_" + classTableName + " = t1.id";
				} else {
					String listComponentTableName = this.library.getTableName(userClass.getName());
					constraint += "ntl.id1_" + classTableName;
					constraint += " FROM " + listTableName + " ntl JOIN " + listComponentTableName + " nt ON ntl.id2_"
							+ listComponentTableName + " = nt.id WHERE " + "nt.id = ? AND " + "ntl.id1_"
							+ classTableName + " = t1.id";
				}
				constraint += ")";
			}

			return "( " + constraint + " )";
		}
	}

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
			if (!this.library.atributoBasico(obj.getClass())) {
				if (this.library.constainsKeyObjectMap(obj))
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
			if (this.library.constainsKeyIdMap(iden)) {
				object = this.library.getIdMap(iden);
			} else {
				object = objCrtr.createObject(this.constraintClass, rs, profundidad);
			}
			list.add(object);
		}

		return list;
	}
}
