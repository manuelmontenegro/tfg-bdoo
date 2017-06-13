package oodb.library;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import oodb.exception.NonExistentObject;
import oodb.exception.OOBDLibraryException;

public class Activator {

	private OODBLibrary lib;

	public Activator(OODBLibrary library) {
		this.lib = library;
	}

	void activate(Object o, int depth) throws OOBDLibraryException, ClassNotFoundException {
		if (!this.lib.containsKeyObjectMap(o))
			throw new OOBDLibraryException(new NonExistentObject());
		if (depth > 0) {
			for (Field f : o.getClass().getDeclaredFields()) {
				f.setAccessible(true);
				if (!lib.basicType(f.getType())) {
					Object obj = null;
					try {
						obj = f.get(o);
					} catch (IllegalArgumentException | IllegalAccessException e) {
						throw new OOBDLibraryException(e);
					}

					if (obj == null) {
						try {
							f.set(o, retrieve(o, f.getName(), f.getType(), depth - 1));
						} catch (IllegalArgumentException | IllegalAccessException e) {
							throw new OOBDLibraryException(e);
						}
					} else {
						activate(obj, depth - 1);
					}

				}
			}
		}
	}

	private Object retrieve(Object o, String columnName, Class<?> class1, int depth) throws ClassNotFoundException {
		if (depth == 0)
			return null;
		int parentId = this.lib.getObjectMap(o);
		String parentTn = "";
		try {
			parentTn = this.lib.getTableName(o.getClass().getName());

			Connection c = this.lib.getConnection();
			String sql = "Select * from " + parentTn + " where id = " + parentId;
			PreparedStatement parentPs = c.prepareStatement(sql);
			ResultSet parentRs = parentPs.executeQuery();
			if (parentRs.next()) {
				Object objeto = null;
				int childId = parentRs.getInt(columnName);
				String childTn = this.lib.getTableName(class1.getName());
				sql = "Select * from " + childTn + " where id = " + childId;

				PreparedStatement childPs = c.prepareStatement(sql);
				ResultSet childRs = childPs.executeQuery();
				if (childRs.next()) {
					try {
						ObjectCreator objCrtr = new ObjectCreator(this.lib);
						objeto = objCrtr.createObject(class1, childRs, depth);
					} catch (InstantiationException e) {
						throw new OOBDLibraryException(e);
					} catch (IllegalAccessException e) {
						throw new OOBDLibraryException(e);
					}
				}
				c.close();
				return objeto;
			}
		} catch (SQLException e) {
			throw new OOBDLibraryException(e);
		}
		return null;
	}
}
