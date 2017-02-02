package tfg.tfg;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import excepciones.InsertarDuplicado;

public class Guardador {
	
	private LibreriaBBDD lib;
	private String nombreTabla;
	
	Guardador(LibreriaBBDD lib){
		this.lib=lib;
		this.nombreTabla = "";
	}
	
	void guardar(Object o) throws SQLException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, InsertarDuplicado{
		ArrayList<Atributo> atributos = sacarAtributos(o);
		this.nombreTabla = o.getClass().getSimpleName();
		String nombreClase = o.getClass().getName();
		crearTabla(atributos, nombreClase);
		if(!this.lib.constainsKeyObjectMap(o)) {
			int id = insertarObjeto(o, this.lib.sacarAtributosNoNulos(o));
			this.lib.putObjectMap(o, id);
			Identificador iden=new Identificador(id, o.getClass().getName());
			this.lib.putIdMap(iden, o);
		} else {
			throw new InsertarDuplicado();
		}
	}
	
	/**
	 * Este metodo es para crear la base de datos cogemos todos los atributos de
	 * la clase que le pasemos
	 */
	private ArrayList<Atributo> sacarAtributos(Object o) {
		ArrayList<Atributo> atributos = new ArrayList<Atributo>();
		for (Field n : o.getClass().getDeclaredFields()) {
			String tipo = "";
			if (n.getType().getCanonicalName().equalsIgnoreCase("Java.lang.String"))
				tipo = "VARCHAR(255)";
			else if (n.getType().getCanonicalName().equalsIgnoreCase("Int"))
				tipo = "INTEGER";
			else{ //Si no es ningun tipo primitivo quiere decir que es una referencia a otro objeto
				Object ob = null;
				n.setAccessible(true); 
				try {
					ob = n.get(o); // Cargar el objeto en ob
				} catch (IllegalArgumentException | IllegalAccessException e1) {
					e1.printStackTrace();
				}
				try {
					guardar(ob); 
				} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException
						| SQLException | InsertarDuplicado e1) {
					e1.printStackTrace();
				}
				tipo = "INTEGER, ADD FOREIGN KEY ("+n.getName()+") REFERENCES "+this.nombreTabla+"(id)"; // El tipo ya va a ser una foreign key	
			}
			Atributo a = new Atributo(n.getName(), tipo);
			atributos.add(a);
		}
		return atributos;
	}
	
	/**
	 * Metodo para crear la base de datos Te crea la base de datos con el
	 * nombreclase que le pasas Si existe no la crea
	 * @throws SQLException 
	 */
	private void crearTabla(ArrayList<Atributo> atributos, String nombreClase) throws SQLException {

		insertarTablaIndice(nombreClase);

		String sql = "CREATE TABLE IF NOT EXISTS " + this.nombreTabla + "(id INTEGER not NULL AUTO_INCREMENT, "
				+ " PRIMARY KEY ( id ))";
		//System.out.println(sql);
		PreparedStatement pst;
		Connection c = this.lib.getConnection();
		pst = c.prepareStatement(sql);
		pst.execute();
		c.close();
	

		for (Atributo a : atributos) {
			insertarColumnaIndice(setIDtabla(nombreClase), a.getNombre(), a.getNombre(), a);
		}

		borrarAtributo(setIDtabla(nombreClase), atributos);
	}
	/**
	 * Metodo para insertar filas en la tabla indicetabla Primero se mira si
	 * existe el nombre de la clase en la tabla si existe no se hace nada pero
	 * si no existe se inserta el nombre de la clase y el nombre de la tabla
	 */
	private void insertarTablaIndice(String nombreClase) throws SQLException {
		String sql = "SELECT nombreclase,nombretabla FROM indicetabla WHERE nombreclase = ?";
		PreparedStatement pst;
		Connection c = this.lib.getConnection();
		pst = c.prepareStatement(sql);
		pst.setString(1, nombreClase);
		ResultSet rs = pst.executeQuery();
		if (!rs.next()) {
			String id = "";
			int idsuma = 0;
			sql = "SELECT max(id) FROM indicetabla";
			c = this.lib.getConnection();
			pst = c.prepareStatement(sql);
			rs = pst.executeQuery();
			if (rs.next()) {

				idsuma = rs.getInt("max(id)") + 1;
				id = idsuma + "";
			} else
				id = "1";

			this.nombreTabla = this.nombreTabla + id;

			sql = "INSERT INTO indicetabla (nombreclase,nombretabla) " + " VALUES ( \"" + nombreClase + "\" , \""
					+ this.nombreTabla + "\" )";
			// System.out.println(sql);
			c = this.lib.getConnection();
			pst = c.prepareStatement(sql);
			pst.execute();
			c.close();

		} else {
			this.nombreTabla = rs.getString("nombretabla");
		}
		c.close();
	}
	
	/**
	 * Metodo para insertar filas en la tabla indicecolumna Primero se mira si
	 * existe el idtabla y el atributo en la tabla si existe no se hace nada
	 * pero si no existe se inserta el idtabla,atributo y columna
	 */
	private void insertarColumnaIndice(String idtabla, String atributo, String columna, Atributo a) throws SQLException {
		String sql = "SELECT id FROM indicecolumna WHERE idtabla = ? and atributo = ?";
		PreparedStatement pst;
 
		Connection c = this.lib.getConnection();
		pst = c.prepareStatement(sql);
		pst.setString(1, idtabla);
		pst.setString(2, atributo);
		ResultSet rs = pst.executeQuery();

		if (!rs.next()) {
			sql = "INSERT INTO indicecolumna (idtabla,atributo,columna) " + " VALUES ( \"" + idtabla + "\" , \""
					+ atributo + "\" , \"" + columna + "\"  )";
			System.out.println(sql);
			Connection c1 = this.lib.getConnection();
			pst = c1.prepareStatement(sql);
			pst.execute();
			c1.close();

			String anyadir = "ALTER TABLE " + this.nombreTabla + " ADD " + atributo + " " + a.getTipo();
			Connection c2 = this.lib.getConnection();
			pst = c2.prepareStatement(anyadir);
			System.out.println(anyadir);
			pst.execute();
			c2.close();

		}
		c.close();
	}

	/**
	 * Metodo para recibir el id de la tabla indicetabla y asi insertarle en la
	 * tabla indicecolumna
	 */
	private String setIDtabla(String nombreClase) throws SQLException {
		String id = "";
		String sql = "SELECT id FROM indicetabla WHERE nombreclase = ? and nombretabla = ?";
		PreparedStatement pst;
		Connection c = this.lib.getConnection();
		pst = c.prepareStatement(sql);
		pst.setString(1, nombreClase);
		pst.setString(2, this.nombreTabla);
		ResultSet rs = pst.executeQuery();
		if (rs.next()) {
			id = rs.getString("id");
		}
		c.close();

		return id;
	}
	
	/**
	 * Borra el atributo de la base de datos en la columna indicecolumna que no
	 * este en el objeto y aparte borra la columna de la clase en cuestion
	 */
	private void borrarAtributo(String idtabla, ArrayList<Atributo> atributos) throws SQLException {
		String sql = "SELECT atributo FROM indicecolumna WHERE idtabla = ?";
		PreparedStatement pst;
		Connection c = this.lib.getConnection();
		pst = c.prepareStatement(sql);

		pst.setString(1, idtabla);
		ResultSet rs = pst.executeQuery();
		boolean esta = false;
		while (rs.next()) {
			for (Atributo a : atributos) {
				if (rs.getString("atributo").equalsIgnoreCase(a.getNombre())) {
					esta = true;
				}
			}
			if (!esta) {

				String del = "ALTER TABLE " + this.nombreTabla + " DROP COLUMN " + rs.getString("atributo");
				c = this.lib.getConnection();
				pst = c.prepareStatement(del);
				pst.execute();
				c.close();
				System.out.println(del);

				del = "DELETE FROM indicecolumna WHERE idtabla = ? and atributo = ?";
				c = this.lib.getConnection();
				pst = c.prepareStatement(del);
				System.out.println(del);

				pst.setString(1, idtabla);
				pst.setString(2, rs.getString("atributo"));
				pst.execute();
				c.close();
			}
			esta = false;
		}
		c.close();
	}	
	


	
	/**
	 * Metodo para insertar en la base de datos Inserta en la base de datos
	 * nombreclase Inserta solo los atributos que le pases en el ArrayList
	 */
	private int insertarObjeto(Object o, ArrayList<Atributo> atributos) throws SQLException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		String claves = "";
		for (int i = 0; i < atributos.size(); i++) {
			if (i != 0)
				claves += " , ";
			Atributo a = atributos.get(i);
			claves += a.getNombre();
		}

		String valores = "";
		for (int i = 0; i < atributos.size(); i++) {
			Atributo a = atributos.get(i);
			if (i != 0)
				valores += " , ";
				Field val = o.getClass().getDeclaredField(a.getNombre());
				val.setAccessible(true);
				if(a.getTipo().equalsIgnoreCase(""))//Si es "" quiere decir que es un objeto referenciado
					valores += "\"" + this.lib.getObjectMap(val.get(o)) + "\""; // Como ya esta guardado recuperas su id del objectMap
				else
					valores += "\"" + val.get(o) + "\"";
				
				
		
		}

		String sql = "INSERT INTO " + this.nombreTabla + " (" + claves + ") " + " VALUES " + "(" + valores + ")";

		System.out.println(sql);
		PreparedStatement pst;
		Connection c = this.lib.getConnection();
		pst = c.prepareStatement(sql);
		pst.execute();
		
		
		sql = "SELECT MAX(id) FROM " + this.nombreTabla ;
		pst = c.prepareStatement(sql);
		ResultSet rs = pst.executeQuery();

		int ret;
		if (rs.next()) {
			ret =rs.getInt("MAX(id)");
		}
		else
			ret= -1;
		c.close();
		return ret;
	}



}
