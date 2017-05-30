package tfg.tfg;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
	 * @throws ClassNotFoundException 
	 */
	Object createObject(Class<?> c, ResultSet rs, int profundidad) throws InstantiationException, IllegalAccessException, SQLException, ClassNotFoundException{
		String nombreClase = c.getName();
		Object o = Class.forName(nombreClase).newInstance();
		Identificador idenO = new Identificador((int)rs.getInt("id"), c.getCanonicalName());//identificar del objeto a crear por ahora el objeto esta vacio
		lib.putIdMap(idenO, o);//insertar ese objeto vacio en el mapa
		lib.putObjectMap(o, (int)rs.getInt("id"));//al ser el objeto un puntero se va a ir actualizando con el paso de este metodo
		Field[] campos = o.getClass().getDeclaredFields();		//Obtener los campos del objecto
		for(Field f: campos){									//Para cada uno de los campos:
			Object campo = null;
			if(!f.getType().isAssignableFrom(List.class) && !f.getType().isAssignableFrom(Set.class))
				campo = rs.getObject(f.getName());			//Obtener de la BD el valor del campo
			f.setAccessible(true);								//Permitir acceder a campos privados
			
			if(f.getType().isAssignableFrom(List.class)) //si es de tipo lista:
			{
				List<Object> list = null;
				String tipoLista = rs.getString("2_" + f.getName());
				if(tipoLista != null){
					Class cl = Class.forName(tipoLista);
					if(tipoLista.contains("ArrayList")){
						list = (ArrayList<Object>) cl.newInstance();
					}
					else if(tipoLista.contains("LinkedList")){
						list = (LinkedList<Object>) cl.newInstance();
					}
					
					Type friendsGenericType = f.getGenericType();
					ParameterizedType friendsParameterizedType = (ParameterizedType) friendsGenericType;
					Type[] friendsType = friendsParameterizedType.getActualTypeArguments();
					Class<?> userClass = (Class<?>) friendsType[0];
					if(lib.atributoBasico(userClass))
					{//Lista de objetos String o int
						String nombreCampo = f.getName();
						String nombreTabla = lib.getTableName(c.getName());
						String nombreTablaMultivalorado = nombreTabla + "_" + nombreCampo;
						String sqlStatement = "SELECT " + nombreCampo + " FROM " + nombreTablaMultivalorado + " WHERE id1_" + nombreTabla + " = " + idenO.getIdentificador() + " ORDER BY posicion";
						Connection con = lib.getConnection();
						PreparedStatement pst;
						pst = con.prepareStatement(sqlStatement);
						ResultSet rset = pst.executeQuery();
						while(rset.next()){
							list.add(rset.getObject(nombreCampo));
						}
						con.close();
					}
					else
					{
						String nombreTabla = lib.getTableName(c.getName());
						String nombreTablaObjetoMultivalorado = lib.getTableName(userClass.getName());
						String nombreTablaMultivalorado = nombreTabla + "_" + f.getName();
						String selectStatement = "id2_" + nombreTablaObjetoMultivalorado;
						String whereStatement = "id1_" + nombreTabla;
						
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
							rs2.next();
							obj = createObject(userClass, rs2, profundidad-1);
							list.add(obj);
							connect.close();
						}
						con.close();
					}
				
				}
				campo = list;
			}
			else if(f.getType().isAssignableFrom(Set.class)) //Si es de tipo set
			{
				Set<Object> set = null;
				String tipoSet = rs.getString("2_" + f.getName());
				if(tipoSet != null){
					Class cl = Class.forName(tipoSet);
					if(tipoSet.contains("HashSet")){
						set = (HashSet<Object>) cl.newInstance();
					}
					else if(tipoSet.contains("LinkedHashSet")){
						set = (LinkedHashSet<Object>) cl.newInstance();
					}
					else if(tipoSet.contains("TreeSet")){
						set = (TreeSet<Object>) cl.newInstance();
					}
					
					Type friendsGenericType = f.getGenericType();
					ParameterizedType friendsParameterizedType = (ParameterizedType) friendsGenericType;
					Type[] friendsType = friendsParameterizedType.getActualTypeArguments();
					Class<?> userClass = (Class<?>) friendsType[0];
					if(userClass.getName().equalsIgnoreCase("Java.lang.String") || userClass.getName().equalsIgnoreCase("Java.lang.Integer"))
					{//Set de objetos String o int
						String nombreCampo = f.getName();
						String nombreTabla = lib.getTableName(c.getName());
						String nombreTablaMultivalorado = nombreTabla + "_" + nombreCampo;
						String sqlStatement = "SELECT " + nombreCampo + " FROM " + nombreTablaMultivalorado + " WHERE id1_" + nombreTabla + " = " + idenO.getIdentificador();
						Connection con = lib.getConnection();
						PreparedStatement pst;
						pst = con.prepareStatement(sqlStatement);
						ResultSet rset = pst.executeQuery();
						while(rset.next()){
							set.add(rset.getObject(nombreCampo));
						}
						con.close();
					}
					else
					{
						String nombreTabla = lib.getTableName(c.getName());
						String nombreTablaObjetoMultivalorado = lib.getTableName(userClass.getName());
						String nombreTablaMultivalorado = nombreTabla + "_" + f.getName();
						String selectStatement = "id2_" + nombreTablaObjetoMultivalorado;
						String whereStatement = "id1_" + nombreTabla;
						
						String sqlStatement = "SELECT " + selectStatement + " FROM " + nombreTablaMultivalorado + " WHERE " + whereStatement + " = " + idenO.getIdentificador();
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
							rs2.next();
							obj = createObject(userClass, rs2, profundidad-1);
							set.add(obj);
							connect.close();
						}
						con.close();
					}
					
				}
				campo = set;
			}
			else if(!lib.atributoBasico(f.getType()))//no basico:
			{
				String tipoAtr = rs.getString("2_" + f.getName());
				if(!tipoAtr.equals("Array")){
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
				else{//ARRAYS
					if(lib.atributoBasico(f.getType().getComponentType())){
						String nombreCampo = f.getName();
						String nombreTabla = lib.getTableName(c.getName());
						String nombreTablaMultivalorado = nombreTabla + "_" + nombreCampo;
						String sqlStatement = "SELECT " + nombreCampo + " FROM " + nombreTablaMultivalorado + " WHERE id1_" + nombreTabla + " = " + idenO.getIdentificador() + " ORDER BY posicion DESC";
						Connection con = lib.getConnection();
						PreparedStatement pst;
						pst = con.prepareStatement(sqlStatement);
						ResultSet rset = pst.executeQuery();
						boolean created = false;
						while(rset.next()){
							if(!created){
								int size = rset.getInt("posicion");
								campo = Array.newInstance(f.getType().getComponentType(), (size+1));
								created = true;
							}
							Array.set(campo, (int) rset.getObject("posicion"), rset.getObject(nombreCampo));
						}
						con.close();
					}
					else
					{
						String nombreTabla = lib.getTableName(c.getName());
						String nombreTablaObjetoMultivalorado = lib.getTableName(f.getType().getComponentType().getName());
						String nombreTablaMultivalorado = nombreTabla + "_" + f.getName();
						String selectStatement = "id2_" + nombreTablaObjetoMultivalorado;
						String whereStatement = "id1_" + nombreTabla;
						
						String sqlStatement = "SELECT " + selectStatement + " FROM " + nombreTablaMultivalorado + " WHERE " + whereStatement + " = " + idenO.getIdentificador() + " ORDER BY posicion DESC";
						Connection con = lib.getConnection();
						PreparedStatement pst;
						pst = con.prepareStatement(sqlStatement);
						ResultSet rset = pst.executeQuery();
						boolean created = false;
						while(rset.next()){
							if(!created){
								int size = rset.getInt("posicion");
								campo = Array.newInstance(f.getType().getComponentType(), (size+1));
								created = true;
							}
							String sqlSttmnt = "SELECT * FROM " + nombreTablaObjetoMultivalorado + " WHERE id = " + rset.getInt(selectStatement);
							Connection connect = lib.getConnection();
							PreparedStatement pst2;
							pst2 = connect.prepareStatement(sqlSttmnt);
							ResultSet rs2 = pst2.executeQuery();
							Object obj = f.getType().getComponentType().newInstance();
							rs2.next();
							obj = createObject(f.getType().getComponentType(), rs2, profundidad-1);
							Array.set(campo, (int) rset.getObject("posicion"), obj);
							connect.close();
						}
						con.close();
					}
					
				}
			}
			f.set(o, campo);									//campo = valor
		}
		return o;
	}

}
