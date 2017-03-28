package tfg.tfg;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ObjectCreator {
	
	private LibreriaBBDD lib;
	
	ObjectCreator(LibreriaBBDD lib){
		this.lib = lib;
	}
	/**
	 * Recibe un ResulSet y devuelve un objeto de la clase 'clase' obteniendo los campos de la BD.
	 * @param rs
	 * @return Object o
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws SQLException
	 */
	Object createObject(Class<?> c, ResultSet rs, int profundidad) throws InstantiationException, IllegalAccessException, SQLException{
		Object o = c.newInstance();
		Identificador idenO=new Identificador((int)rs.getInt("id"), c.getCanonicalName());//identificar del objeto a crear por ahora el objeto esta vacio
		lib.putIdMap(idenO, o);//insertar ese objeto vacio en el mapa
		lib.putObjectMap(o, (int)rs.getInt("id"));//al ser el objeto un puntero se va a ir actualizando con el paso de este metodo
		
		Field[] campos = o.getClass().getDeclaredFields();		//Obtener los campos del objecto
		for(Field f: campos){									//Para cada uno de los campos:
			Object campo = null;
			if(!f.getType().isAssignableFrom(List.class))
				campo = rs.getObject(f.getName());			//Obtener de la BD el valor del campo
			f.setAccessible(true);								//Permitir acceder a campos privados
			
			if(f.getType().isAssignableFrom(List.class)) //si es de tipo lista:
			{	
				//CREAR LISTA AQUI
				//campo = Lista
				Type friendsGenericType = f.getGenericType();
				ParameterizedType friendsParameterizedType = (ParameterizedType) friendsGenericType;
				Type[] friendsType = friendsParameterizedType.getActualTypeArguments();
				Class userClass = (Class) friendsType[0];
				if(userClass.getName().equalsIgnoreCase("Java.lang.String") || userClass.getName().equalsIgnoreCase("Java.lang.Integer"))
				{//Lista de objetos String o int
					String nombreCampo = f.getName();
					String nombreTabla = lib.getTableName(c.getName());
					String nombreTablaMultivalorado = nombreTabla + "_" + nombreCampo;
					String sqlStatement = "SELECT " + nombreCampo + " FROM " + nombreTablaMultivalorado + " WHERE id_" + nombreTabla + " = " + idenO.getIdentificador() + " ORDER BY posicion";
					Connection con = lib.getConnection();
					PreparedStatement pst;
					pst = con.prepareStatement(sqlStatement);
					ResultSet rset = pst.executeQuery();
					while(rset.next()){
						//AÑADIR rset.getObject(nombreCampo) AQUI A LA LISTA
						System.out.println(rset.getObject(nombreCampo));
					}
				}
				else
				{
					String nombreCampo = f.getName();
					String nombreTabla = lib.getTableName(c.getName());
					String nombreTablaObjetoMultivalorado = lib.getTableName(userClass.getName());
					String nombreTablaMultivalorado = nombreTabla + "_" + nombreTablaObjetoMultivalorado;
					String selectStatement = "id_" + nombreTablaObjetoMultivalorado;
					String whereStatement = "id_" + nombreTabla;
					if(nombreTabla.equalsIgnoreCase(nombreTablaObjetoMultivalorado))
					{
						selectStatement = "id2_" + nombreTablaObjetoMultivalorado;
						whereStatement = "id1_" + nombreTabla;
					}
					
					String sqlStatement = "SELECT " + selectStatement + " FROM " + nombreTablaMultivalorado + " WHERE " + whereStatement + " = " + idenO.getIdentificador() + " ORDER BY posicion";
					Connection con = lib.getConnection();
					PreparedStatement pst;
					pst = con.prepareStatement(sqlStatement);
					ResultSet rset = pst.executeQuery();
					while(rset.next()){
						String sqlSttmnt = "SELECT * FROM " + nombreTablaObjetoMultivalorado + " WHERE id = " + rset.getInt(selectStatement);
						Connection connect = lib.getConnection();
						PreparedStatement pst2;
						pst2 = connect.prepareStatement(sqlSttmnt);
						ResultSet rs2 = pst2.executeQuery();
						Object obj = userClass.newInstance();
						obj = createObject(userClass, rs2, profundidad); //CAMBIAR LA PROFUNDIDAD
						//AÑADIR OBJ AQUI A LA LISTA
						connect.close();
						//Seleccionar los objetos de la bd con el id que devuelve el rset y añadir a la lista
					}
					con.close();
				}
				campo = null;
			}
			else if(!f.getType().getCanonicalName().contains("java.lang.String") && !f.getType().getCanonicalName().contains("int")) //Si el campo no es ni int ni string:
			{
				if(profundidad==1)//se recorta aqui la recusividad para evitar hacer una consulta y crear el objeto cuando va ser null
					campo=null;
				if(campo!=null){
					Identificador iden=new Identificador((int)campo, f.getType().getCanonicalName());
					if(lib.constainsKeyIdMap(iden)){ 
						campo=lib.getIdMap(iden);
					}
					else {
						String tn = lib.getTableName(f.getType().getCanonicalName());
						String sqlStatement = "SELECT * FROM " + tn + " WHERE ID = ?";
						Connection con = lib.getConnection();
						PreparedStatement pst;
						pst = con.prepareStatement(sqlStatement); 			// Preparación de la sentencia
						pst.setInt(1, (int) campo);
						ResultSet rset = pst.executeQuery(); 					// Ejecución de la sentencia
						if(rset.next()){
							campo = createObject(f.getType(),rset, profundidad-1);
						}
						else
							campo=null;
						con.close();
					}
				}
			}
			f.set(o, campo);									//campo = valor
		}
		return o;
	}

}
