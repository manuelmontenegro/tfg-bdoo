package tfg.tfg;

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


public class GuardadorOactualizador {
	
	private LibreriaBBDD lib;
	
	GuardadorOactualizador (LibreriaBBDD lib){
		this.lib=lib;
	}

	/**
	 * Metodo que usa la libreria para guardar un objeto y todos los que tenga a su vez
	 * si el objeto ya esta previamente guardado actualizarara sus valores
	 * Usa un mapa parcial de objetos visitados que a la vuelta vuelca en el general de objetos guardados
	 * @param o objeto que se va a guardar
	 * @param im mapa parcial de objetos guardados para no volver a guardar uno ya guardado
	 * @throws SQLException
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	void guardarOactualizar(Object o, IdentityHashMap<Object, Integer> im)throws SQLException, IllegalArgumentException, IllegalAccessException {
		int id = -1;
		String nombreTabla;

		nombreTabla = crearTabla(o);

		if (!im.containsKey(o) && !this.lib.constainsKeyObjectMap(o)) {
			ArrayList<Atributo> atributos = alterarTabla(nombreTabla, o);
			id = insertarFilaVacia(nombreTabla,o.getClass().getName(),atributos);
		} else {// esta en alguno de los dos
			if (im.containsKey(o)) {// obtener id de la fila con de alguno de
									// los dos mapas
				id = im.get(o);
			} else {
				id = this.lib.getObjectMap(o);
			}
		}
		im.put(o, id);// guardar en im <obj, id>
		Identificador key = new Identificador(id, o.getClass().getName());
		this.lib.putIdMap(key, o);// hay que insertar tambien en idMap para no
									// perder consistencia

		for (Field f : o.getClass().getDeclaredFields()) {// para cada atributo
															// no basico{

			f.setAccessible(true);
			Object atributo = null;

			atributo = f.get(o);

			if (!esBasico(f)) {
				if (atributo != null) {

					if (atributo instanceof List<?> || atributo instanceof Set<?>) {// si es una lista o set hay que tratarlo aparte
						vaciarTablaIntermediaById(nombreTabla, f.getName(), id);

						// Aqui sacamos el tipo del parametro
						Type friendsGenericType = f.getGenericType();
						ParameterizedType friendsParameterizedType = (ParameterizedType) friendsGenericType;
						Type[] friendsType = friendsParameterizedType.getActualTypeArguments();
						Class<?> userClass = (Class<?>) friendsType[0];

						String tipoInstancia = "";
						if (atributo instanceof List<?>)
							tipoInstancia = "List";
						else
							tipoInstancia = "Set";

						int i = 0;
						// Si son basicos insertamos en la tabla de
						// multivalorado
						if (userClass.getName().equalsIgnoreCase("Java.lang.String")
								|| userClass.getName().equalsIgnoreCase("Java.lang.Integer")) {
							if (tipoInstancia.equalsIgnoreCase("List")) {
								for (Object obj : ((List<?>) atributo)) {
									insertarMultivalorado(nombreTabla, f.getName(), id, obj, i, tipoInstancia);
									i++;
								}
							} else {
								for (Object obj : ((Set<?>) atributo)) {
									insertarMultivalorado(nombreTabla, f.getName(), id, obj, i, tipoInstancia);
									i++;
								}
							}
						} else { // Si el parametro es un objeto con el id del usuario,
								//la id de la direccion recien insertada y la posicion
									// que ocupa se inserta la fila en la tabla intermedia
							if (tipoInstancia.equalsIgnoreCase("List")) {
								for (Object parametro : ((List<?>) atributo)) {
									if (!im.containsKey(parametro)) {
										guardarOactualizar(parametro, im); // Te recorres la lista guardando cada objeto
									}
									insertarObjetoLista(nombreTabla, id, lib.getTableName(parametro), f.getName(),im.get(parametro), i, tipoInstancia);
									i++;
								}
							} else {
								for (Object parametro : ((Set<?>) atributo)) {
									if (!im.containsKey(parametro)) {
										guardarOactualizar(parametro, im); // Te recorres el set guardando cada objeto
									}
									insertarObjetoLista(nombreTabla, id, lib.getTableName(parametro), f.getName(),
											im.get(parametro), i, tipoInstancia);
									i++;
								}
							}
						}

					} else {// es un objeto complejo
						if (!im.containsKey(atributo)) {// if(!esta en im)
							guardarOactualizar(atributo, im);
						}
					}

				}
			}
		}
		update(nombreTabla, o, im);
	}
	private void vaciarTablaIntermediaById(String nombreTabla, String nombreAtributo, int id) throws SQLException {
		String sql = "DELETE FROM "+ nombreTabla +"_"+nombreAtributo + " WHERE id1_"+nombreTabla + " = ?" ;
		PreparedStatement pst;
		Connection c = this.lib.getConnection();
		pst = c.prepareStatement(sql);
		pst.setInt(1, id);
		pst.execute();
		c.close();
	}

	private void insertarObjetoLista(String nombreTablaPadre, int idPadre, String nombreTablaParametro, String nombreParametro, Integer idParametro, int posicion, String tipoInstancia) throws SQLException {
		Connection c = this.lib.getConnection();

		String sqlInsert = "";
		if (tipoInstancia.equalsIgnoreCase("List"))
			sqlInsert = "INSERT INTO " + nombreTablaPadre + "_" + nombreParametro + " ( id1_" + nombreTablaPadre
					+ ", id2_" + nombreTablaParametro + ", posicion) VALUES (?, ?, ?)";
		else // Si es un Set
			sqlInsert = "INSERT INTO " + nombreTablaPadre + "_" + nombreParametro + " ( id1_" + nombreTablaPadre
					+ ", id2_" + nombreTablaParametro + ") VALUES (?, ?)";
		
		PreparedStatement pstI;
		System.out.println(sqlInsert+ idPadre);
		pstI = c.prepareStatement(sqlInsert);
		pstI.setInt(1, idPadre);
		pstI.setObject(2, idParametro);
		if (tipoInstancia.equalsIgnoreCase("List"))
			pstI.setInt(3, posicion);
		pstI.execute();

		c.close();
	}

	private void insertarMultivalorado(String nombreTablaPadre, String nombreLista, int idPadre, Object elemento, int posicion, String tipoInstancia) throws SQLException {
		
		Connection c = this.lib.getConnection();

		String sqlInsert = "";
		if(tipoInstancia.equalsIgnoreCase("List"))
			sqlInsert="INSERT INTO "+nombreTablaPadre+"_"+nombreLista+" ( id1_"+nombreTablaPadre+", "+nombreLista+", posicion) VALUES (?, ?, ?)";
		else
			sqlInsert="INSERT INTO "+nombreTablaPadre+"_"+nombreLista+" ( id1_"+nombreTablaPadre+", "+nombreLista+") VALUES (?, ?)";

		PreparedStatement pstI;
		pstI = c.prepareStatement(sqlInsert);
		pstI.setInt(1, idPadre);
		pstI.setObject(2, elemento);
		if(tipoInstancia.equalsIgnoreCase("List"))
			pstI.setInt(3, posicion);
		pstI.execute();
		
		c.close();
	}

	/**
	 * Inserta una fila vacia en la tabla pasada como argumento
	 * @param nombreTabla
	 * @param atributos 
	 * @return
	 * @throws SQLException
	 */
	private int insertarFilaVacia(String nombreTabla, String tipo, ArrayList<Atributo> atributos) throws SQLException{
		int id = 0;
		ArrayList<String> valores = new ArrayList<String>();
		String nombres = "";
		String claves = "";
		for(Atributo a : atributos){
			if(!a.isBasico()){
				valores.add(a.getClaseConstructora());	
				claves+=",? ";
				nombres+=" , "+a.getNombre();
			}
		}
		String sql = "INSERT INTO "+nombreTabla+" (1_tipo"+nombres+") VALUES (? "+claves+")";
		Connection c = this.lib.getConnection();
		PreparedStatement pst = c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);
		pst.setString(1, tipo);
		int i = 2;
		for(String valor : valores){
			pst.setString(i,valor);
			i++;
		}
		System.out.println(pst.unwrap(PreparedStatement.class).toString().split(":")[1]);
		pst.executeUpdate();// insertar fila pero todo vacio
		ResultSet rs = pst.getGeneratedKeys();
		if (rs.next()) 
		    id = rs.getInt(1);
		// obtener id de la fila con getgeneratedkeys()
		c.close();
		return id;
	}
	
	private void update(String nombreTabla, Object o, IdentityHashMap<Object, Integer> im) throws SQLException, IllegalArgumentException, IllegalAccessException{
		String claves = "";
		ArrayList<String> valores = new ArrayList<String>();	
		
		Field[] fields=o.getClass().getDeclaredFields();
		int tam=o.getClass().getDeclaredFields().length;
		for (int i=0; i<tam ;i++) {
			Field f=fields[i];
			f.setAccessible(true);
			Object atributo=null;
			
			atributo=f.get(o);

			if(!esBasico(f)){
				if(atributo!=null){	
					
					if( f.get(o) instanceof List<?> ){//si es una lista actualizar la lista
						
					}
					else if (f.get(o) instanceof Set<?>){
						
					}
					else{//
						valores.add(im.get(atributo)+"");
						if (i != 0)
							claves += " , ";
						claves+=f.getName()+" =?";
					}
					
				}
			}
			else {//es basico
				valores.add(atributo+"");
				if (i != 0)
					claves += " , ";
				claves+=f.getName()+" =?";
			}			
		}	
		
		String sqlUpdate="UPDATE " + nombreTabla +
				  " SET " + claves +
				  " WHERE ID = ?";
		System.out.println(sqlUpdate);
		Connection con;
		
		con = this.lib.getConnection();
		PreparedStatement pst = con.prepareStatement(sqlUpdate);
		for (int i = 0; i < valores.size(); i++) { 
			pst.setObject(i+1, valores.get(i));
		}
		pst.setObject(valores.size()+1, im.get(o));	//Añadir la ID parametrizada
		pst.execute();
		con.close();
		
		
	}

	/**
	 * Metodo para combrar si un campo es de tipo basico o no
	 * @param f
	 * @return cierto si es basico
	 */
	private boolean esBasico(Field f) {
		boolean ret=false;
		if (f.getType().getCanonicalName().equalsIgnoreCase("Java.lang.String") || f.getType().getCanonicalName().equalsIgnoreCase("Int"))
			ret=true;
		return ret;
	}

	/**
	 * Metodo para crear una tabla con el nombre que coresponda segun el objeto pasado
	 * la tabla solo tendra id para que asi otras tablas la puedan referenciar
	 * @param o objeto para el que va crear la tabla
	 * @return nombre de la tabla creada o de la que ya habia
	 * @throws SQLException
	 */
	private String crearTabla(Object o) throws SQLException {
		String nombreTabla="";
		String sql = "SELECT nombreclase,nombretabla FROM indicetabla WHERE nombreclase = ?";
		PreparedStatement pst;
		Connection c = this.lib.getConnection();
		pst = c.prepareStatement(sql);
		pst.setString(1, o.getClass().getName());
		ResultSet rs = pst.executeQuery();
		if (rs.next()) {//ya esta creada, se consulta su nombre
			nombreTabla=rs.getString("nombretabla");
		}
		else{//hay que crearla
			nombreTabla=insertarIndiceTabla(o.getClass().getName(), o.getClass().getSimpleName());
			crearTabla(nombreTabla);//crea la tabla solo con id
		}
		c.close();
		return nombreTabla;
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
		Connection c = this.lib.getConnection();
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
	    
	    nombreTabla=casiNombreTabla+id;
	    
	    c = this.lib.getConnection();
	    sql = "UPDATE indicetabla SET nombretabla=? where id=?";
	    PreparedStatement pst = c.prepareStatement(sql);
	    pst.setString(1, nombreTabla);
	    pst.setInt(2, id);
	    pst.executeUpdate();//actualizar la misma fila pero ya con su nombre de tabla
		c.close();
	    
		return nombreTabla;		
	}
	
	/**
	 * Crea una tablo con el nombre dado
	 * La tabla solo tendra id
	 * @param nombreTabla nombre de la tabla que se va a crear
	 * @throws SQLException
	 */
	private void crearTabla(String nombreTabla) throws SQLException {

		String sql = "CREATE TABLE IF NOT EXISTS "+nombreTabla+" (id INTEGER not NULL AUTO_INCREMENT, 1_tipo VARCHAR(255), PRIMARY KEY ( id ))";
		Connection c = this.lib.getConnection();
		PreparedStatement pst = c.prepareStatement(sql);
		
		pst.execute();
		c.close();
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
	private ArrayList<Atributo> alterarTabla(String nombreTabla, Object o) throws SQLException {
		ArrayList<Atributo> atributos = sacarAtributos(o);
		String idIndiceTabla=getIDIndiceTabla(nombreTabla);
		for (Atributo a : atributos) {
			if(!estaIndiceColumna(a, idIndiceTabla))
				insertarIndiceColumna(nombreTabla, a, idIndiceTabla);
		}
		return atributos;
		
		//borrarIndiceColumna(nombreTabla, atributos, idIndiceTabla);
	}
	private boolean estaIndiceColumna(Atributo a, String idIndiceTabla) throws SQLException {
		String sql = "SELECT id FROM indicecolumna WHERE idtabla = ? and atributo = ?";

		Connection c = this.lib.getConnection();
		PreparedStatement pst = c.prepareStatement(sql);
		pst.setString(1, idIndiceTabla);
		pst.setString(2, a.getNombre());
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
	private ArrayList<Atributo> sacarAtributos(Object o) throws SQLException {
		ArrayList<Atributo> atributos = new ArrayList<Atributo>();
		for (Field f : o.getClass().getDeclaredFields()) {
			boolean objetoNulo=false;
			boolean basico = true;
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
						String nombreTablaObjeto = lib.getTableName(o);
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
							String nombreTablaParametro =crearTabla(param);
							crearTablaIntermedia(nombreTablaObjeto,nombreTablaParametro,f.getName(),tipoInstancia);
						}
						nombre = "2_"+f.getName();
						tipo = "VARCHAR(255)";
						basico = false;
						claseConstructora = ob.getClass().getName();
					}
					else{ //Si no es ningun tipo primitivo quiere decir que es una referencia a otro objeto
						
						if(ob==null){
							objetoNulo=true; 
							//System.out.println("el objeto contiene un objeto null");
						}
						else{
							String nombreTablaReferenciada=crearTabla(ob);
							tipo = "INTEGER, ADD FOREIGN KEY ("+f.getName()+") REFERENCES "+nombreTablaReferenciada+"(id) ON DELETE SET NULL"; // El tipo ya va a ser una foreign key	
						}
					}
				} catch (IllegalArgumentException | IllegalAccessException | SecurityException | InstantiationException e) {
					//throw new LibreriaBBDDException(e);
					e.printStackTrace();
				}
			}
			if(!objetoNulo){
				Atributo a = new Atributo(nombre, tipo,basico);
				if(!basico)
					a.setClaseConstructora(claseConstructora);
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
		
		System.out.println(sql);
		Connection c = this.lib.getConnection();
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
		
		System.out.println(sql);
		Connection c = this.lib.getConnection();
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
		Connection c = this.lib.getConnection();
		pst = c.prepareStatement(sql);
		pst.setString(1, nombreTabla);
		ResultSet rs = pst.executeQuery();
		if (rs.next()) {
			id = rs.getString("id");
		}
		c.close();

		return id;
	}

	/**
	 * Metodo para intentar insertar en indice columna el atributo pasado
	 * si no estaba le inserta y añade una comuna a la tabla para guardarle
	 * @param nombreTabla
	 * @param a
	 * @throws SQLException
	 */
	private void insertarIndiceColumna(String nombreTabla,  Atributo a, String idIndiceTabla) throws SQLException {

		String sql1 = "INSERT INTO indicecolumna (idtabla,atributo,columna) " + " VALUES ( \"" + idIndiceTabla + "\" , \""
				+ a.getNombre() + "\" , \"" + a.getNombre() + "\"  )";
		//POR AHORA EL NOMBRE DEL ATRIBUTO DE LA CLASE Y EL NOMBRE DE LA CALUMNA DONDE SE VA A GUARDAR ES EL MISMO
		//YA QUE NO PUEDE HABER DOS ATRIBUTOS DE UNA CLASE CON EL MISNO NOMBRE
		System.out.println(sql1);
		Connection c1 = this.lib.getConnection();
		PreparedStatement pst1 = c1.prepareStatement(sql1);
		pst1.execute();
		c1.close();

		String anyadir = "ALTER TABLE " + nombreTabla + " ADD " + a.getNombre() + " " + a.getTipo();
		Connection c2 = this.lib.getConnection();
		PreparedStatement pst = c2.prepareStatement(anyadir);
		//System.out.println(anyadir);
		pst.execute();
		c2.close();
		
	}
	/**
	 * Metodo para eliminar las culumnas que no tengoa el objeto pero que todavia tenga la tabla usada para guardar el objeto
	 * ademas elimina su entrad en indice columnas
	 * @param nombreTabla
	 * @param atributos
	 * @throws SQLException
	 */
	private void borrarIndiceColumna(String nombreTabla, ArrayList<Atributo> atributos, String idIndiceTabla) throws SQLException {
		String sql = "SELECT atributo FROM indicecolumna WHERE idtabla = ?";
		PreparedStatement pst;
		Connection c = this.lib.getConnection();
		pst = c.prepareStatement(sql);

		pst.setString(1, idIndiceTabla);
		ResultSet rs = pst.executeQuery();
		boolean esta = false;
		while (rs.next()) {//para cada atributo de la tabla indice columna
			for (Atributo a : atributos) {//comprueba que este en los atributos del objeto
				if (rs.getString("atributo").equalsIgnoreCase(a.getNombre())) {
					esta = true;
				}
			}
			if (!esta) {//si no esta hay que borrar su entarada en indicecolumna y eliminar la columna de su tabla

				String del = "ALTER TABLE " + nombreTabla + " DROP COLUMN " + rs.getString("atributo");
				c = this.lib.getConnection();
				pst = c.prepareStatement(del);
				pst.execute();
				c.close();
				System.out.println(del);

				del = "DELETE FROM indicecolumna WHERE idtabla = ? and atributo = ?";
				c = this.lib.getConnection();
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
