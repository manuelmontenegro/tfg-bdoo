package tfg.tfg;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import prueba.Empleado;

public class Query {
	private Class clase;
	private Constraint restriccion;
	
	public Query(Class cl){
		clase = cl;
	}
	
	public void setConstraint(Constraint c) {
		this.restriccion = c;
	}

	public String toSql() {
		String sqlStatement = "SELECT * FROM ";
		/*
		 * Buscar en la tabla de indices el nombre de la tabla a la que corresponde
		 * el atributo clase. Ahora funciona para el ejemplo de empleado*/
		sqlStatement += "Empleado1 ";
		sqlStatement += "WHERE ";
		sqlStatement += restriccion.toSql();
		return sqlStatement;
	}
	
	//ESTO VALE PARA EL EJEMPLO DE EMPLEADO, HAY QUE CAMBIARLO PARA QUE FUNCIONE PARA TODO
	public List<Object> executeQuery(Connection con){
		String sql = this.toSql();
		System.out.println(sql);
		List<Object> lista = new ArrayList<Object>();
		try {
			PreparedStatement pst = con.prepareStatement(sql);
			ResultSet rs = pst.executeQuery();
			//Object object = this.clase.newInstance();
			Empleado object;
			while (rs.next()) {
				object = new Empleado();
	            object.setContrasenya(rs.getString("contrasenya"));
	            object.setDireccion(rs.getString("direccion"));
	            object.setDNI(rs.getString("dni"));
	            object.setNombre(rs.getString("nombre"));
	            object.setSexo(rs.getString("sexo"));
	            object.setTelefono(rs.getInt("telefono"));
	            object.setTipo(rs.getString("tipo"));
	            
	            lista.add(object);
	        }
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return lista;
	}
}
