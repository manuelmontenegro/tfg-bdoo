package oobd.library;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.mysql.jdbc.Statement;

import pruebaList.Direccion;
import pruebaList.Usuario;


public class Saver {
	
	private OOBDLibrary library;
	
	Saver (OOBDLibrary lib){
		this.library=lib;
	}

	void save(Object o, IdentityHashMap<Object, Integer> im)throws SQLException, IllegalArgumentException, IllegalAccessException {
		int id = -1;
		String tableName;

		tableName = createTable(o);

		if (!im.containsKey(o) && !this.library.constainsKeyObjectMap(o)) {
			ArrayList<Atribute> atributes = alterarTabla(tableName, o);
			id = insertEmptyRow(tableName,atributes);
		} else {
			if (im.containsKey(o)) {
				id = im.get(o);
			} else {
				id = this.library.getObjectMap(o);
			}
		}
		im.put(o, id);
		Identificator key = new Identificator(id, o.getClass().getName());
		this.library.putIdMap(key, o);

		for (Field f : o.getClass().getDeclaredFields()) {

			f.setAccessible(true);
			Object atribute = null;

			atribute = f.get(o);

			if (!isBasic(f)) {
				if (atribute != null) {

					if (atribute instanceof List<?> || atribute instanceof Set<?>) {
						emptyMiddleTableById(tableName, f.getName(), id);

						Type friendsGenericType = f.getGenericType();
						ParameterizedType friendsParameterizedType = (ParameterizedType) friendsGenericType;
						Type[] friendsType = friendsParameterizedType.getActualTypeArguments();
						Class<?> userClass = (Class<?>) friendsType[0];

						String instanceType = "";
						if (atribute instanceof List<?>)
							instanceType = "List";
						else
							instanceType = "Set";

						int i = 0;

						if (userClass.getName().equalsIgnoreCase("Java.lang.String")
								|| userClass.getName().equalsIgnoreCase("Java.lang.Integer")) {
							if (instanceType.equalsIgnoreCase("List")) {
								for (Object obj : ((List<?>) atribute)) {
									insertMultivalued(tableName, f.getName(), id, obj, i, instanceType);
									i++;
								}
							} else {
								for (Object obj : ((Set<?>) atribute)) {
									insertMultivalued(tableName, f.getName(), id, obj, i, instanceType);
									i++;
								}
							}
						} else {
							if (instanceType.equalsIgnoreCase("List")) {
								for (Object param : ((List<?>) atribute)) {
									if (!im.containsKey(param)) {
										save(param, im);
									}
									insertObjectList(tableName, id, library.getTableName(param.getClass().getName()), f.getName(),im.get(param), i, instanceType);
									i++;
								}
							} else {
								for (Object param : ((Set<?>) atribute)) {
									if (!im.containsKey(param)) {
										save(param, im);
									}
									insertObjectList(tableName, id, library.getTableName(param.getClass().getName()), f.getName(),
											im.get(param), i, instanceType);
									i++;
								}
							}
						}

					} else {
						if (!im.containsKey(atribute)) {
							save(atribute, im);
						}
					}

				}
			}
		}
		update(tableName, o, im);
	}
	private void emptyMiddleTableById(String tableName, String fieldName, int id) throws SQLException {
		String sql = "DELETE FROM "+ tableName +"_"+fieldName + " WHERE id1_"+tableName + " = ?" ;
		PreparedStatement pst;
		Connection c = this.library.getConnection();
		pst = c.prepareStatement(sql);
		pst.setInt(1, id);
		pst.execute();
		c.close();
	}

	private void insertObjectList(String parentTableName, int idPadre, String paramTableName, String paramName, Integer idParametro, int position, String instanceType) throws SQLException {
		Connection c = this.library.getConnection();

		String sqlInsert = "";
		if (instanceType.equalsIgnoreCase("List"))
			sqlInsert = "INSERT INTO " + parentTableName + "_" + paramName + " ( id1_" + parentTableName
					+ ", id2_" + paramTableName + ", posicion) VALUES (?, ?, ?)";
		else
			sqlInsert = "INSERT INTO " + parentTableName + "_" + paramName + " ( id1_" + parentTableName
					+ ", id2_" + paramTableName + ") VALUES (?, ?)";
		
		PreparedStatement pstI;
		pstI = c.prepareStatement(sqlInsert);
		pstI.setInt(1, idPadre);
		pstI.setObject(2, idParametro);
		if (instanceType.equalsIgnoreCase("List"))
			pstI.setInt(3, position);
		pstI.execute();

		c.close();
	}

	private void insertMultivalued(String parentTableName, String listName, int parentId, Object obj, int position, String instanceType) throws SQLException {
		
		Connection c = this.library.getConnection();

		String sqlInsert = "";
		if(instanceType.equalsIgnoreCase("List"))
			sqlInsert="INSERT INTO "+parentTableName+"_"+listName+" ( id1_"+parentTableName+", "+listName+", posicion) VALUES (?, ?, ?)";
		else
			sqlInsert="INSERT INTO "+parentTableName+"_"+listName+" ( id1_"+parentTableName+", "+listName+") VALUES (?, ?)";

		PreparedStatement pstI;
		pstI = c.prepareStatement(sqlInsert);
		pstI.setInt(1, parentId);
		pstI.setObject(2, obj);
		if(instanceType.equalsIgnoreCase("List"))
			pstI.setInt(3, position);
		pstI.execute();
		
		c.close();
	}

	private int insertEmptyRow(String tableName, ArrayList<Atribute> atributes) throws SQLException{
		int id = 0;
		ArrayList<String> values = new ArrayList<String>();
		String names = "";
		String keys = "";
		boolean primero=true;
		for(int i=0; i<atributes.size();i++){
			if(!atributes.get(i).isBasic()){
				
				if(!primero){
					keys+=" , ";
					names+=" , ";
				}
				values.add(atributes.get(i).getConstructorClass());	
				keys+=" ? ";
				names+="2_"+atributes.get(i).getName();
				primero=false;
			}
		}
		String sql = "INSERT INTO "+tableName+" ("+names+") VALUES ("+keys+")";
		Connection c = this.library.getConnection();
		PreparedStatement pst = c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);
		int i = 1;
		for(String value : values){
			pst.setString(i,value);
			i++;
		}
		pst.executeUpdate();
		ResultSet rs = pst.getGeneratedKeys();
		if (rs.next()) 
		    id = rs.getInt(1);
		c.close();
		return id;
	}
	
	private void update(String tableName, Object o, IdentityHashMap<Object, Integer> im) throws SQLException, IllegalArgumentException, IllegalAccessException{
		String keys = "";
		ArrayList<String> values = new ArrayList<String>();	
		
		Field[] fields=o.getClass().getDeclaredFields();
		int size=o.getClass().getDeclaredFields().length;
		for (int i=0; i<size ;i++) {
			Field f=fields[i];
			f.setAccessible(true);
			Object field=null;
			
			field=f.get(o);

			if(!isBasic(f)){
				if(field!=null){	
					
					if( f.get(o) instanceof List<?> ){
						
					}
					else if (f.get(o) instanceof Set<?>){
						
					}
					else{
						values.add(im.get(field)+"");
						if (i != 0)
							keys += " , ";
						keys+=f.getName()+" =?";
					}
					
				}
			}
			else {
				values.add(field+"");
				if (i != 0)
					keys += " , ";
				keys+=f.getName()+" =?";
			}			
		}	
		
		String sqlUpdate="UPDATE " + tableName +
				  " SET " + keys +
				  " WHERE ID = ?";
		Connection con;
		
		con = this.library.getConnection();
		PreparedStatement pst = con.prepareStatement(sqlUpdate);
		for (int i = 0; i < values.size(); i++) { 
			pst.setObject(i+1, values.get(i));
		}
		pst.setObject(values.size()+1, im.get(o));
		pst.execute();
		con.close();
		
		
	}

	private boolean isBasic(Field f) {
		boolean ret=false;
		if (library.atributoBasico(f.getType()))
			ret=true;
		return ret;
	}

	private String createTable(Object o) throws SQLException {
		String tableName="";
		String sql = "SELECT nombreclase,nombretabla FROM indicetabla WHERE nombreclase = ?";
		PreparedStatement pst;
		Connection c = this.library.getConnection();
		pst = c.prepareStatement(sql);
		pst.setString(1, o.getClass().getName());
		ResultSet rs = pst.executeQuery();
		if (rs.next()) {//ya esta creada, se consulta su nombre
			tableName=rs.getString("nombretabla");
		}
		else{//hay que crearla
			tableName=insertarIndiceTabla(o.getClass().getName(), o.getClass().getSimpleName());
			
			String sql1 = "CREATE TABLE IF NOT EXISTS "+tableName+" (id INTEGER not NULL AUTO_INCREMENT, PRIMARY KEY ( id ))";
			Connection c1 = this.library.getConnection();
			PreparedStatement pst1 = c1.prepareStatement(sql1);
			
			pst1.execute();
			c1.close();
		}
		c.close();
		return tableName;
	}
	
	/**
	 * inserta una fila en el indice de tablas
	 * primero solo introduce el nombre de completo de la clase y deja que se elija solo el id
	 * despues con ese id el nombre simple de la clase los concatena para crear el nombre de la tabla
	 * finalmente actualiza esa fila con el nombre de tabla 
	 * @param nombreClase nombre completo da la clase
	 * @param casiNombreTabla nombre de la clase sin todo lo anterior
	 * @return nombre de la tabla que se ha elegido
	 * @throws SQLException
	 */
	private String insertarIndiceTabla(String nombreClase, String casiNombreTabla) throws SQLException {
		int id;
		String nombreTabla;
		Connection c = this.library.getConnection();
		String sql = "INSERT INTO indicetabla (nombreclase) VALUES (?)";
		
		PreparedStatement statement = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		statement.setString(1, nombreClase);
		int affectedRows = statement.executeUpdate();//insertar una fila con solo nombre da la clase
		if (affectedRows == 0) {
			throw new SQLException("Creating index failed, no rows affected.");
		}
		
		ResultSet generatedKeys = statement.getGeneratedKeys();
	    if (generatedKeys.next()) {
	        id=generatedKeys.getInt(1);
	    }
	    else {
	        throw new SQLException("Creating index failed, no ID obtained.");
	    }
	    c.close();
	    
	    nombreTabla=id+casiNombreTabla;
	    
	    c = this.library.getConnection();
	    sql = "UPDATE indicetabla SET nombretabla=? where id=?";
	    PreparedStatement pst = c.prepareStatement(sql);
	    pst.setString(1, nombreTabla);
	    pst.setInt(2, id);
	    pst.executeUpdate();//actualizar la misma fila pero ya con su nombre de tabla
		c.close();
	    
		return nombreTabla;		
	}
	

	/**
	 * Altera la tabla para que coincida en columnas con los atributos del objeto
	 * primero para cada atributo del objeto si no esta en indie columna le inserta en indice columna
	 * y despues altera la tabla para que tenga la comuna corespondiente para guardar ese atributo
	 * segundo elimina las columnas que corresponde
	 * y despues elimina su entada en indice columnas
	 * @param nombreTabla
	 * @param o
	 * @throws SQLException
	 */
	private ArrayList<Atribute> alterarTabla(String nombreTabla, Object o) throws SQLException {
		ArrayList<Atribute> atributos = sacarAtributos(o);
		
		String idIndiceTabla=getIDIndiceTabla(nombreTabla);
		for (Atribute a : atributos) {
			if(!estaIndiceColumna(a, idIndiceTabla))
				insertarIndiceColumna(nombreTabla, a, idIndiceTabla);
		}
		return atributos;
		
		//borrarIndiceColumna(nombreTabla, atributos, idIndiceTabla);
	}
	private boolean estaIndiceColumna(Atribute a, String idIndiceTabla) throws SQLException {
		String sql = "SELECT id FROM indicecolumna WHERE idtabla = ? and atributo = ?";

		Connection c = this.library.getConnection();
		PreparedStatement pst = c.prepareStatement(sql);
		pst.setString(1, idIndiceTabla);
	
		pst.setString(2, a.getName());

		ResultSet rs = pst.executeQuery();
		
		boolean ret=rs.next();
		c.close();
		return ret;
	}

	/**
	 * Metodo para sacar el nombre y el tipo de los atributos de un objeto
	 * sirve para saber de que tipo tiene que ser su respactiva columna
	 * @param o
	 * @return lista de atributos del objeto nombre y tipo sql
	 * @throws SQLException
	 */
	private ArrayList<Atribute> sacarAtributos(Object o) throws SQLException {
		ArrayList<Atribute> atributos = new ArrayList<Atribute>();
		for (Field f : o.getClass().getDeclaredFields()) {
			boolean objetoNulo=false;
			boolean basico = true;
			boolean multi = false;
			String nombre = f.getName();
			String tipo = "";
			String claseConstructora = "";
			//System.out.println(f.getName()+" es :"+f.getType().getCanonicalName());
			if (f.getType().getCanonicalName().equalsIgnoreCase("Java.lang.String"))
				tipo = "VARCHAR(255)";
			else if (f.getType().getCanonicalName().equalsIgnoreCase("Int"))
				tipo = "INTEGER";
			else{
				f.setAccessible(true); 
				Object ob = null;
				try {
					ob = f.get(o);
				} catch (IllegalArgumentException | IllegalAccessException e1) {
					
				}
				try {
					if(ob instanceof Set<?> || ob instanceof List<?>){
						//Aqui sacamos el parametro de la lista
						Type friendsGenericType = f.getGenericType();
						ParameterizedType friendsParameterizedType = (ParameterizedType) friendsGenericType;
						Type[] friendsType = friendsParameterizedType.getActualTypeArguments();
						Class<?> userClass = (Class<?>) friendsType[0];
						String nombreTablaObjeto = library.getTableName(o.getClass().getName());
						String tipoInstancia = "";
						if( ob instanceof List<?>)
							tipoInstancia = "List";
						else
							tipoInstancia = "Set";
						
						//Si son basicos creamos una tabla de multivalorado
						if(userClass.getName().equalsIgnoreCase("Java.lang.String") ){
							crearTablaMultivalorado(nombreTablaObjeto, f.getName(),"VARCHAR(255)",tipoInstancia);
						}
						else if(userClass.getName().equalsIgnoreCase("Java.lang.Integer")){
							crearTablaMultivalorado(nombreTablaObjeto, f.getName(), "INTEGER",tipoInstancia);
						}
						else{ //Si el parametro es un objeto creamos una tabla intermedia.
							Object param = userClass.newInstance();
							String nombreTablaParametro =createTable(param);
							crearTablaIntermedia(nombreTablaObjeto,nombreTablaParametro,f.getName(),tipoInstancia);
						}
						nombre = f.getName();
						tipo = "VARCHAR(255)";
						basico = false;
						multi = true;
						claseConstructora = ob.getClass().getName();
					}
					else{ //Si no es ningun tipo primitivo quiere decir que es una referencia a otro objeto
						
						if(ob==null){
							objetoNulo=true; 
							//System.out.println("el objeto contiene un objeto null");
						}
						else{
							String nombreTablaReferenciada=createTable(ob);
							basico = false;
							nombre = f.getName();
							claseConstructora = ob.getClass().getName();
							tipo = "INTEGER, ADD FOREIGN KEY ("+f.getName()+") REFERENCES "+nombreTablaReferenciada+"(id) ON DELETE SET NULL"; // El tipo ya va a ser una foreign key	
						}
					}
				} catch (IllegalArgumentException | IllegalAccessException | SecurityException | InstantiationException e) {
					//throw new LibreriaBBDDException(e);
					e.printStackTrace();
				}
			}
			if(!objetoNulo){
				Atribute a = new Atribute(nombre, tipo, basico, multi);
				if(!basico)
					a.setConstructorClass(claseConstructora);
				atributos.add(a);
			}
		}
		
		return atributos;
	}
	
	/**
	 * Creamos tabla multivalorado para listas con atributos basicos.
	 * @param nto
	 * @param nombreCampo
	 * @param tipo
	 * @param tipoInstancia 
	 * @throws SQLException
	 */
	private void crearTablaMultivalorado(String nto, String nombreCampo, String tipo, String tipoInstancia) throws SQLException {
		String sql = "CREATE TABLE IF NOT EXISTS "+nto+"_"+nombreCampo
				+" (id INTEGER not NULL AUTO_INCREMENT,"
				+ "id1_"+nto+" INTEGER, "
				+ nombreCampo+ " "+tipo+", ";
		
		if(tipoInstancia.equalsIgnoreCase("List"))
			sql +=  " posicion INTEGER, ";
				
		sql+= " PRIMARY KEY ( id ),"
		+ " CONSTRAINT fk_"+nto+"_"+nombreCampo+"_"+nombreCampo+" FOREIGN KEY (id1_"+nto+") REFERENCES "+nto+"(id) )";
		
		Connection c = this.library.getConnection();
		PreparedStatement pst = c.prepareStatement(sql);
		pst.execute();
		c.close();
	}

	/**
	 * Creamos una tabla intermedia con los id de las dos tablas mas la posicion.
	 * @param nto
	 * @param ntp
	 * @param tipoInstancia 
	 * @throws SQLException
	 */
	private void crearTablaIntermedia(String nto, String ntp, String nombreParametro,String tipoInstancia) throws SQLException {
		String sql = "CREATE TABLE IF NOT EXISTS "+nto+"_"+nombreParametro
				+" (id INTEGER not NULL AUTO_INCREMENT,"
				+ "id1_"+nto+" INTEGER, "
				+ "id2_"+ntp+" INTEGER, ";
		
		if(tipoInstancia.equalsIgnoreCase("List"))
			sql +=  " posicion INTEGER, ";
				
		sql+= " PRIMARY KEY ( id ),"
		+ " CONSTRAINT fk1_"+nto+"_"+nombreParametro+" FOREIGN KEY (id1_"+nto+") REFERENCES "+nto+"(id),"
		+ " CONSTRAINT fk2_"+nto+"_"+nombreParametro+" FOREIGN KEY (id2_"+ntp+") REFERENCES "+ntp+"(id) )";
		
		Connection c = this.library.getConnection();
		PreparedStatement pst = c.prepareStatement(sql);
		pst.execute();
		c.close();
	}

	/**
	 * dado un nombre de tabla busca en indice tabla su id y lo devuelve
	 * @param nombreTabla
	 * @return
	 * @throws SQLException
	 */
	private String getIDIndiceTabla(String nombreTabla) throws SQLException {
		String id = "";
		String sql = "SELECT id FROM indicetabla WHERE nombretabla = ? ";
		PreparedStatement pst;
		Connection c = this.library.getConnection();
		pst = c.prepareStatement(sql);
		pst.setString(1, nombreTabla);
		ResultSet rs = pst.executeQuery();
		if (rs.next()) {
			id = rs.getString("id");
		}
		c.close();

		return id;
	}

	private void insertarIndiceColumna(String nombreTabla,  Atribute a, String idIndiceTabla) throws SQLException {
	
		String sql1;
		String alter;
		
		if(a.isBasic()){	
			sql1 = "INSERT INTO indicecolumna (idtabla,atributo,columna) " + " VALUES ( \"" + idIndiceTabla + "\" , \""
					+ a.getName() + "\" , \"" + a.getName() + "\"  )";

			alter = "ALTER TABLE " + nombreTabla + " ADD " + a.getName() + " " + a.getType();
		}
		else{
			if(a.isMultivalued()){	
				sql1 = "INSERT INTO indicecolumna (idtabla,atributo,nombrecolumnatipo) " + " VALUES ( \"" + idIndiceTabla + "\" , \""
						+ a.getName() + "\" , \"2_" + a.getName() + "\" )";
				
				alter = "ALTER TABLE " + nombreTabla + " ADD  2_"+a.getName()+" VARCHAR(255)";
			}
			else{
				sql1 = "INSERT INTO indicecolumna (idtabla,atributo,columna,nombrecolumnatipo) " + " VALUES ( \"" + idIndiceTabla + "\" , \""
						+ a.getName() + "\" , \"" + a.getName() + "\" , \"2_" + a.getName() + "\" )";
				
				alter = "ALTER TABLE " + nombreTabla + " ADD " + a.getName() + " " + a.getType()+", ADD 2_"+a.getName()+" VARCHAR(255)";
			}
			
		}
		Connection c1 = this.library.getConnection();
		PreparedStatement pst1 = c1.prepareStatement(sql1);
		pst1.execute();
		c1.close();
		
		Connection c2 = this.library.getConnection();
		PreparedStatement pst = c2.prepareStatement(alter);
		pst.execute();
		
		c2.close();
	}

	private void borrarIndiceColumna(String nombreTabla, ArrayList<Atribute> atributos, String idIndiceTabla) throws SQLException {
		String sql = "SELECT atributo FROM indicecolumna WHERE idtabla = ?";
		PreparedStatement pst;
		Connection c = this.library.getConnection();
		pst = c.prepareStatement(sql);

		pst.setString(1, idIndiceTabla);
		ResultSet rs = pst.executeQuery();
		boolean esta = false;
		while (rs.next()) {//para cada atributo de la tabla indice columna
			for (Atribute a : atributos) {//comprueba que este en los atributos del objeto
				if (rs.getString("atributo").equalsIgnoreCase(a.getName())) {
					esta = true;
				}
			}
			if (!esta) {//si no esta hay que borrar su entarada en indicecolumna y eliminar la columna de su tabla

				String del = "ALTER TABLE " + nombreTabla + " DROP COLUMN " + rs.getString("atributo");
				c = this.library.getConnection();
				pst = c.prepareStatement(del);
				pst.execute();
				c.close();
				System.out.println(del);

				del = "DELETE FROM indicecolumna WHERE idtabla = ? and atributo = ?";
				c = this.library.getConnection();
				pst = c.prepareStatement(del);
				System.out.println(del);

				pst.setString(1, idIndiceTabla);
				pst.setString(2, rs.getString("atributo"));
				pst.execute();
				c.close();
			}
			esta = false;
		}
		c.close();
	}


}
