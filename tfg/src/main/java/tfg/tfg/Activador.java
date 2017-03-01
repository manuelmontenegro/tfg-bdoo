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

	protected void activar(Object o) throws ObjetoInexistente {
		if(!this.lib.constainsKeyObjectMap(o))
			throw new ObjetoInexistente();
		for(Field f: o.getClass().getDeclaredFields()){										
			f.setAccessible(true);								
			if(!f.getType().getCanonicalName().contains("java.lang.String") && !f.getType().getCanonicalName().contains("int")){ //Si el campo no es ni int ni string:
				Object obj=null;
				try {
					obj = f.get(o);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(obj==null){
					try {
						f.set(o,recuperar(o));
					} catch (IllegalArgumentException | IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					
					
				}
				else{
					activar(obj);
				}
			
			}
		}	
	}
	
	private Object recuperar(Object o){
		int id=this.lib.getObjectMap(o);
		String tn="";
		try {
			tn=this.lib.getTableName(o);
		
			Connection c=this.lib.getConnection();
			String sql = "Select * from "+ tn + " where id = " + id;
			System.out.println(sql);
			PreparedStatement ps = c.prepareStatement(sql);
			ResultSet rs = ps.executeQuery();
			if(rs.next()){
				Query q= this.lib.newQuery(o.getClass());
				Object objeto = null;
				try {
					System.out.println(o.getClass());
					objeto = q.createObject(o.getClass(), rs, 3);
				} catch (InstantiationException | IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
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
