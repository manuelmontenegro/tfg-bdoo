package tfg.tfg;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import prueba.Empleado;

/**
 * Clase que permite ejecutar una sentencia SQL para devolver una lista de objetos de la tabla deseada
 * con las restricciones definidas.
 * @author Carlos
 *
 */
public class Query {
	private Class clase;				//Clase a la que se aplican las constraint
	private Constraint restriccion;		//Constraint a aplicar
	
	/**
	 * Constructor de la clase.
	 * @param cl
	 */
	public Query(Class cl){
		clase = cl;
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
	 */
	private String getTableName(Connection con){
		String str = "";
		String sqlStatement = "SELECT nombretabla "
				+ "FROM INDICETABLA "
				+ "WHERE nombreclase = \'" 
				+ this.clase.getName() + "\'"; 			//Sentencia sql para obtener el nombre de la tabla asociado a la clase 'clase'
		
		PreparedStatement pst;
		try {
			pst = con.prepareStatement(sqlStatement); 	//Preparación de la sentencia
			ResultSet rs = pst.executeQuery(); 			//Ejecución de la sentencia
			rs.next();
			str = rs.getString("nombretabla"); 			//str = nombre de la tabla
		} catch (SQLException e) {e.printStackTrace();}
		
		return str;
	}
	
	/**
	 * Devuelve la sentencia SQL a ejecutar con la constraint creada.
	 * @param con (Conexión con la BD).
	 * @return Sentencia SQL.
	 */
	public String toSql(Connection con) {
		String sqlStatement = "SELECT * FROM ";
		String tableName = this.getTableName(con); 		//Nombre de la tabla a la que se aplican las constraint
		sqlStatement += tableName;
		sqlStatement += " WHERE ";
		sqlStatement += restriccion.toSql(); 			//Constraint que se aplicarán en la cláusula WHERE
		return sqlStatement;
	}
	
	/**
	 * Ejecuta la sentencia SQL con las constraint creadas y devuelve una lista con los resultados.
	 * @param con (Conexión con la BD).
	 * @return List que contiene los resultados de la consulta.
	 */
	public List<Object> executeQuery(Connection con){
		String sql = this.toSql(con); 								//Sentencia SQL a ejecutar
		List<Object> lista = new ArrayList<Object>(); 				//Lista en la que se introducirán los objetos
		try {
			PreparedStatement pst = con.prepareStatement(sql);		//Preparación de la sentencia
			ResultSet rs = pst.executeQuery();						//Ejecución de la sentencia
			//Object object = this.clase.newInstance();				//Instancia de la clase 'clase'
			Empleado object; //-> CAMBIAR PARA QUE FUNCIONE PARA TODOS.
			while (rs.next()) {										//Mientras aún haya resultados de la sentencia SQL ejecutada
				object = new Empleado();
	            object.setContrasenya(rs.getString("contrasenya"));
	            object.setDireccion(rs.getString("direccion"));
	            object.setDNI(rs.getString("dni"));
	            object.setNombre(rs.getString("nombre"));
	            object.setSexo(rs.getString("sexo"));
	            object.setTelefono(rs.getInt("telefono"));
	            object.setTipo(rs.getString("tipo"));
	            
	            lista.add(object);									//Añadir el objeto a la lista que se devolverá
	        }
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return lista;
	}
}
