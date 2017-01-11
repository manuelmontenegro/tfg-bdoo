package tfg.tfg;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase que permite ejecutar una sentencia SQL para devolver una lista de objetos de la tabla deseada
 * con las restricciones definidas.
 * @author Carlos
 *
 */
public class Query {
	private LibreriaBBDD lib;
	private Class clase;				//Clase a la que se aplican las constraint
	private Constraint restriccion;		//Constraint a aplicar
	
	/**
	 * Constructor de la clase.
	 * @param cl
	 */
	protected Query(Class<?> cl,LibreriaBBDD lib){
		clase = cl;
		this.lib=lib;
		this.restriccion = new AndConstraint();
	}
	
	/**
	 * Aplicar las constraint creadas.
	 * @param c
	 */
	public void setConstraint(Constraint c) {
		this.restriccion = c;
	}
	
	/**
	 * Devuelve el nombre de la tabla correspondiente al atributo clase.
	 * @param con (Conexión a la BD).
	 * @return Nombre de la tabla.
	 * @throws SQLException 
	 */
	private String getTableName(Connection con) throws SQLException{
		String str = "";
		String sqlStatement = "SELECT nombretabla "
				+ "FROM INDICETABLA "
				+ "WHERE nombreclase = \'" 
				+ this.clase.getName() + "\'"; 			//Sentencia sql para obtener el nombre de la tabla asociado a la clase 'clase'
		
		PreparedStatement pst;
		pst = con.prepareStatement(sqlStatement); 		// Preparación de la sentencia
		ResultSet rs = pst.executeQuery(); 				// Ejecución de la sentencia
		rs.next();
		str = rs.getString("nombretabla"); 				// str = nombre de la tabla
		return str;
	}
	
	private String getTableName(Connection con, String className) throws SQLException{
		String str = "";
		String sqlStatement = "SELECT nombretabla "
				+ "FROM INDICETABLA "
				+ "WHERE nombreclase = \'" 
				+ className + "\'"; 			//Sentencia sql para obtener el nombre de la tabla asociado a la clase 'clase'
		PreparedStatement pst;
		pst = con.prepareStatement(sqlStatement); 		// Preparación de la sentencia
		ResultSet rs = pst.executeQuery(); 				// Ejecución de la sentencia
		rs.next();
		str = rs.getString("nombretabla"); 				// str = nombre de la tabla
		con.close();
		return str;
	}
	
	/**
	 * Devuelve la sentencia SQL a ejecutar con la constraint creada.
	 * @param con (Conexión con la BD).
	 * @return Sentencia SQL.
	 * @throws SQLException 
	 */
	public String toSql(Connection con) throws SQLException {
		String sqlStatement = "SELECT * FROM ";
		String tableName = this.getTableName(con); 		//Nombre de la tabla a la que se aplican las constraint
		sqlStatement += tableName;
		sqlStatement += " WHERE ";
		sqlStatement += restriccion.toSql(); 			//Constraint que se aplicarán en la cláusula WHERE
		return sqlStatement;
	}
	
	/**
	 * Recibe un ResulSet y devuelve un objeto de la clase 'clase' obteniendo los campos de la BD.
	 * @param rs
	 * @return Object o
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws SQLException
	 */
	private Object createObject(Class c, ResultSet rs) throws InstantiationException, IllegalAccessException, SQLException{
		Object o = c.newInstance();
		Field[] campos = o.getClass().getDeclaredFields();		//Obtener los campos del objecto
		for(Field f: campos){									//Para cada uno de los campos:
			Object campo = rs.getObject(f.getName());			//Obtener de la BD el valor del campo
			f.setAccessible(true);								//Permitir acceder a campos privados
			
			if(!f.getType().getCanonicalName().contains("java.lang.String") && !f.getType().getCanonicalName().contains("int")) //Si el campo no es ni int ni string:
			{
				Identificador iden=new Identificador(rs.getInt("id"), this.clase.getName());
				if(this.lib.constainsKeyIdMap(iden)){ 
					campo=this.lib.getIdMap(iden);
				}
				else {
					String tn = this.getTableName(lib.getConnection(),f.getType().getCanonicalName());
					String sqlStatement = "SELECT * FROM " + tn + " WHERE ID = ?";
					Connection con = lib.getConnection();
					PreparedStatement pst;
					pst = con.prepareStatement(sqlStatement); 			// Preparación de la sentencia
					pst.setInt(1, (int) campo);
					ResultSet rset = pst.executeQuery(); 					// Ejecución de la sentencia
					if(rset.next()){
						campo = this.createObject(f.getType(),rset);
						this.lib.putIdMap(iden, campo);
						this.lib.putObjectMap(campo, rset.getInt("id"));
					}
					else
						campo=null;
					con.close();
				}
			}
			
			f.set(o, campo);									//campo = valor
		}
		return o;
	}
	
	/**
	 * Ejecuta la sentencia SQL con las constraint creadas y devuelve una lista con los resultados.
	 * @param con (Conexión con la BD).
	 * @param idMap 
	 * @return List que contiene los resultados de la consulta.
	 * @throws SQLException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	protected List<Object> executeQuery(Connection con) throws SQLException, InstantiationException, IllegalAccessException{
		String sql = this.toSql(con); 									//Sentencia SQL a ejecutar
		List<Object> lista = new ArrayList<Object>(); 					//Lista en la que se introducirán los objetos
		PreparedStatement pst = con.prepareStatement(sql); 				// Preparación de la sentencia
		List<Object> values = this.restriccion.getValues(); 			// Lista de valores de las restricciones
		for (int i = 1; i <= values.size(); i++) { 						// Para cada valor:
			pst.setObject(i, values.get(i - 1)); 						// Añadir el valor a la sentencia
		}
		
		ResultSet rs = pst.executeQuery(); 								// Ejecución de la sentencia
		Object object; 													// Instancia de la clase 'clase'
		while (rs.next()) { 											// Mientras aún haya resultados de la sentencia SQL ejecutada
			Identificador iden=new Identificador(rs.getInt("id"), this.clase.getName());
			if(this.lib.constainsKeyIdMap(iden)){ 
				object = this.lib.getIdMap(iden);
			}
			else{ 														//Si no esta te creas el objeto y le añades al mapa
				object = createObject(this.clase,rs); 								// Crea el objeto de la clase
				this.lib.putIdMap(iden, object);
				this.lib.putObjectMap(object, rs.getInt("id"));
			}
			lista.add(object); 											// Añadir el objeto a la lista que se devolverá
		}

		return lista;
	}
}


/*
Cosas que faltan:
 * En el método createObject, dentro del if (en el caso que no sea int o String) mirar si el objeto que queremos esta en el mapa, 
   ahora lo recupera de la BD sin mirar antes.(HECHO)¸

 * Actualizar con profundidad (si uno de los campos del objeto no es int o String llamar recursivamente a actualizar)
   Para controlar si queremos o no actualizar con profundidad el profesor dio 2 alternativas: añadir un bool al método (si está
   a true se llamará recursivamente y si está a false hará lo que hace el método que tenemos ahora) o hacer otro método distinto 
   que haga la función de la primera alternativa con el bool a true (updateRecursivo o como queramos llamarlo) y el método 
   update que tenemos hecho dejarlo tal cual está(HECHO)
   
   
 * Borrar creo que no tenemos que tocar nada - on delete set null linea 150

 * Recuperación con límite: al método createObject añadirle un entero que vaya aumentando a medida que se llama a la función 
   recursivamente, cuando llegue a un límite devuelve NULL en vez de recuperar el objeto que toque en la base de datos.
   El límite creo que el profesor dijo que lo pusiese el usuario, podemos añadir un int lim_recuperacion a la LibreriaBBDD 
   y un método set público para que el programador lo cambie. En el método createObject compara el int que aumenta recursivamente
   con lib.getLim_Recuperacion para saber cuando parar. (num < lim.getLim_Recuperación)
   

   
 */
