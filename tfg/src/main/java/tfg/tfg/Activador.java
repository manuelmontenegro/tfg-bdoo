package tfg.tfg;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import constraints.Constraint;
import constraints.SimpleConstraint;
import excepciones.ObjetoInexistente;

public class Activador {

	private LibreriaBBDD lib;
	
	public Activador(LibreriaBBDD libreriaBBDD) {
		this.lib=libreriaBBDD;
	}
	
/**
 * 
 * @param o
 * @throws ObjetoInexistente
 */

	protected void activar(Object o) throws ObjetoInexistente {
		if(!this.lib.constainsKeyObjectMap(o))
			throw new ObjetoInexistente();
		for(Field f: o.getClass().getDeclaredFields()){										
			f.setAccessible(true);	
			//Si el objeto no es simple
			if(!f.getType().getCanonicalName().contains("java.lang.String") && !f.getType().getCanonicalName().contains("int")){ //Si el campo no es ni int ni string:
				Object obj=null;
				try {
					obj = f.get(o); 
				} catch (IllegalArgumentException | IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(obj==null){ //Si el objeto es nulo hay que recuperarle de la base de datos
					try {
						f.set(o,recuperar(o, f.getName(), f.getType()));
					} catch (IllegalArgumentException | IllegalAccessException  e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					
					
				}
				else{ //Si el objeto no es nulo llamaremos activar con este objeto para recuperar sus objetos hijos
					activar(obj);
				}
			
			}
		}	
	}
	/**
	 * Dado un objeto recupera de la BBDD el objeto hijo que es nulo  
	 * @param o
	 * @param nombreCol
	 * @param class1
	 * @return
	 */
	private Object recuperar(Object o, String nombreCol, Class<?> class1){
		int idPadre=this.lib.getObjectMap(o); 
		String tnPadre="";
		try {
			tnPadre=this.lib.getTableName(o);
		
			Connection c=this.lib.getConnection();
			String sql = "Select * from "+ tnPadre + " where id = " + idPadre; //Aqui cogeremos el id en la base de datos del objeto hijo
			PreparedStatement psPadre = c.prepareStatement(sql);
			ResultSet rsPadre = psPadre.executeQuery();
			if(rsPadre.next()){
				Query q= this.lib.newQuery(class1); //Te creas una query para llamar al createObject
				Object objeto = null;
				int idHijo=rsPadre.getInt(nombreCol);
				String tnHijo=this.lib.getTableName(class1.getName());
				sql = "Select * from "+ tnHijo + " where id = " + idHijo; // Seleccionas todos los campos del objetoHijo

				PreparedStatement psHijo = c.prepareStatement(sql);
				ResultSet rsHijo = psHijo.executeQuery();
				if(rsHijo.next()){
					try {
						objeto = q.createObject(class1, rsHijo, 3); //Te creas el objeto hijo que es el que tienes que devolver
					} catch (InstantiationException | IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				c.close();
				return objeto;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}
