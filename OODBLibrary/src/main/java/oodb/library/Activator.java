package oodb.library;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import oodb.exception.NonExistentObject;
import oodb.exception.OODBLibraryException;

/**
 * Implements the activation of an object process.
 */
public class Activator {

	private OODBLibrary lib;

	/**
	 * Class constructor.
	 * @param library
	 */
	Activator(OODBLibrary library) {
		this.lib = library;
	}

	/**
	 * Activates Object o with the specific depth.
	 * @param o
	 * @param depth
	 * @throws OODBLibraryException
	 * @throws ClassNotFoundException
	 */
	void activate(Object o, int depth) throws OODBLibraryException, ClassNotFoundException {
		if (!this.lib.containsKeyObjectMap(o))
			throw new OODBLibraryException(new NonExistentObject());
		if (depth > 0) {
			for (Field f : o.getClass().getDeclaredFields()) {
				f.setAccessible(true);
				if (!lib.basicType(f.getType())) {
					Object obj = null;
					try {
						obj = f.get(o);
					} catch (IllegalArgumentException | IllegalAccessException e) {
						throw new OODBLibraryException(e);
					}

					if (obj == null) {
						try {
							f.set(o, retrieve(o, f.getName(), f.getType(), depth - 1));
						} catch (IllegalArgumentException | IllegalAccessException e) {
							throw new OODBLibraryException(e);
						}
					} else {
						activate(obj, depth - 1);
					}

				}
			}
		}
	}

	/**
	 * Retrieves an object from the database.
	 * @param o
	 * @param columnName
	 * @param class1
	 * @param depth
	 * @return Object
	 * @throws ClassNotFoundException
	 */
	private Object retrieve(Object o, String columnName, Class<?> class1, int depth) throws ClassNotFoundException {
		if (depth == 0)
			return null;
		int parentId = this.lib.getObjectMap(o);
		String parentTn = "";
		try {
			parentTn = this.lib.getTableName(o.getClass().getName());

			Connection c = this.lib.getConnection();
			StringBuilder sb = new StringBuilder();
			sb.append("Select * from ");
			sb.append(parentTn);
			sb.append(" where id = ");
			sb.append(parentId);
			PreparedStatement parentPs = c.prepareStatement(sb.toString());
			sb = new StringBuilder();
			ResultSet parentRs = parentPs.executeQuery();
			if (parentRs.next()) {
				Object objeto = null;
				int childId = parentRs.getInt(columnName);
				String childTn = this.lib.getTableName(class1.getName());
				sb.append("Select * from ");
				sb.append(childTn);
				sb.append(" where id = ");
				sb.append(childId);

				PreparedStatement childPs = c.prepareStatement(sb.toString());
				ResultSet childRs = childPs.executeQuery();
				if (childRs.next()) {
					try {
						ObjectCreator objCrtr = new ObjectCreator(this.lib);
						objeto = objCrtr.createObject(class1, childRs, depth);
					} catch (InstantiationException e) {
						throw new OODBLibraryException(e);
					} catch (IllegalAccessException e) {
						throw new OODBLibraryException(e);
					}
				}
				c.close();
				return objeto;
			}
		} catch (SQLException e) {
			throw new OODBLibraryException(e);
		}
		return null;
	}
}
