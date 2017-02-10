package tfg.tfg;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.IdentityHashMap;

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
	 */
	void guardarOactualizar(Object o, IdentityHashMap<Object, Integer> im) throws SQLException {
		int id=-1;
		String nombreTabla=crearTabla(o);
		if(!im.containsKey(o) && !this.lib.constainsKeyObjectMap(o) ){
			alterarTabla(nombreTabla, o);
			id=insertarFilaVacia(nombreTabla);
		}
		else{//esta en alguno de los dos
			 if(im.containsKey(o)){// obtener id de la fila con de alguno de los dos mapas
				 id=im.get(o);
			 }
			 else{
				 id=this.lib.getObjectMap(o);
			 }
		}
		im.put(o, id);//guardar en im <obj, id>	
		
		for (Field f : o.getClass().getDeclaredFields()) {//para cada atributo no basico{
			
			f.setAccessible(true);
			Object atributo=null;
			try {
				atributo=f.get(o);
			} catch (IllegalArgumentException | IllegalAccessException e1) {
				e1.printStackTrace();
			}
			if(!esBasico(f)){
				if(atributo!=null){					
					if(!im.containsKey(atributo)){//    if(!esta en im)
						guardarOactualizar(atributo, im);
					}
				}
			}
		}	
		update(nombreTabla, o, im);
	}
	/**
	 * Inserta una fila vacia en la tabla pasada como argumento
	 * @param nombreTabla
	 * @return
	 * @throws SQLException
	 */
	private int insertarFilaVacia(String nombreTabla) throws SQLException{
		int id = 0;
		String sql = "INSERT INTO "+nombreTabla+" () VALUES ()";
		System.out.println(sql);
		Connection c = this.lib.getConnection();
		PreparedStatement pst = c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);
		pst.executeUpdate();// insertar fila pero todo vacio
		ResultSet rs = pst.getGeneratedKeys();
		if (rs.next()) 
		    id = rs.getInt(1);
		// obtener id de la fila con getgeneratedkeys()
		c.close();
		return id;
	}
	
	private void update(String nombreTabla, Object o, IdentityHashMap<Object, Integer> im) throws SQLException{
		String claves = "";
		ArrayList<String> valores = new ArrayList<String>();	
		
		Field[] fields=o.getClass().getDeclaredFields();
		int tam=o.getClass().getDeclaredFields().length;
		for (int i=0; i<tam ;i++) {
			Field f=fields[i];
			f.setAccessible(true);
			Object atributo=null;
			try {
				atributo=f.get(o);
			} catch (IllegalArgumentException | IllegalAccessException e1) {
				e1.printStackTrace();
			}

			if(!esBasico(f)){
				if(atributo!=null){					
					valores.add(im.get(atributo)+"");
					if (i != 0)
						claves += " , ";
					claves+=f.getName()+" =?";
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
		Connection con = this.lib.getConnection();
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
	 * inserta una fia en el indice de tablas
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

		String sql = "CREATE TABLE IF NOT EXISTS "+nombreTabla+" (id INTEGER not NULL AUTO_INCREMENT, PRIMARY KEY ( id ))";
		Connection c = this.lib.getConnection();
		PreparedStatement pst = c.prepareStatement(sql);
		
		pst.execute();
		c.close();
	}
	/**
	 * Altera la tabla para que coincida en columnas con los atributos del objeto
	 * primero para cada atributo le intenta insertar en indice columnas si no estaba ya
	 * y despues altera la tabla para que tenga la comuna corespondiente para guardar ese atributo
	 * segundo elimina las columnas que corresponde
	 * y despues elimina su entada en indice columnas
	 * @param nombreTabla
	 * @param o
	 * @throws SQLException
	 */
	private void alterarTabla(String nombreTabla, Object o) throws SQLException {
		ArrayList<Atributo> atributos = sacarAtributos(o);
		for (Atributo a : atributos) {
			insertarIndiceColumna(nombreTabla, a);
			
		}
		//borrarIndiceColumna(nombreTabla, atributos);
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
			String tipo = "";
			//System.out.println(f.getName()+" es :"+f.getType().getCanonicalName());
			if (f.getType().getCanonicalName().equalsIgnoreCase("Java.lang.String"))
				tipo = "VARCHAR(255)";
			else if (f.getType().getCanonicalName().equalsIgnoreCase("Int"))
				tipo = "INTEGER";
			else{ //Si no es ningun tipo primitivo quiere decir que es una referencia a otro objeto
				Object ob = null;
				f.setAccessible(true); 
		
					try {
						ob = f.get(o);
					} catch (IllegalArgumentException | IllegalAccessException e) {
						e.printStackTrace();
					} // Cargar el objeto en ob
				
				if(ob==null){
					objetoNulo=true; 
					//System.out.println("el objeto contiene un objeto null");
					tipo = "INTEGER";
				}
				else{
					String nombreTablaReferenciada=crearTabla(ob);
					tipo = "INTEGER, ADD FOREIGN KEY ("+f.getName()+") REFERENCES "+nombreTablaReferenciada+"(id) ON DELETE SET NULL"; // El tipo ya va a ser una foreign key	
				}
			}
			if(!objetoNulo){
				Atributo a = new Atributo(f.getName(), tipo);
				atributos.add(a);
			}
		}
		return atributos;
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
	private void insertarIndiceColumna(String nombreTabla,  Atributo a) throws SQLException {
		String idIndiceTabla=getIDIndiceTabla(nombreTabla);
		String sql = "SELECT id FROM indicecolumna WHERE idtabla = ? and atributo = ?";

		Connection c = this.lib.getConnection();
		PreparedStatement pst = c.prepareStatement(sql);
		pst.setString(1, idIndiceTabla);
		pst.setString(2, a.getNombre());
		ResultSet rs = pst.executeQuery();

		if (!rs.next()) {//hay que insertar en indece columna y alterar su tabla
			String sql1 = "INSERT INTO indicecolumna (idtabla,atributo,columna) " + " VALUES ( \"" + idIndiceTabla + "\" , \""
					+ a.getNombre() + "\" , \"" + a.getNombre() + "\"  )";
			//POR AHORA EL NOMBRE DEL ATRIBUTO DE LA CLASE Y EL NOMBRE DE LA CALUMNA DONDE SE VA A GUARDAR ES EL MISMO
			//YA QUE NO PUEDE HABER DOS ATRIBUTOS DE UNA CLASE CON EL MISNO NOMBRE
			//System.out.println(sql);
			Connection c1 = this.lib.getConnection();
			PreparedStatement pst1 = c1.prepareStatement(sql1);
			pst1.execute();
			c1.close();

			String anyadir = "ALTER TABLE " + nombreTabla + " ADD " + a.getNombre() + " " + a.getTipo();
			Connection c2 = this.lib.getConnection();
			pst = c2.prepareStatement(anyadir);
			//System.out.println(anyadir);
			pst.execute();
			c2.close();

		}
		//else ya esta insertado ese atributo en indececolumna
		c.close();
		
	}
	/**
	 * Metodo para eliminar las culumnas que no tengoa el objeto pero que todavia tenga la tabla usada para guardar el objeto
	 * ademas elimina su entrad en indice columnas
	 * @param nombreTabla
	 * @param atributos
	 * @throws SQLException
	 */
	private void borrarIndiceColumna(String nombreTabla, ArrayList<Atributo> atributos) throws SQLException {
		String idIndiceTabla=getIDIndiceTabla(nombreTabla);
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
