package tfg.tfg;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import constraints.AndConstraint;
import constraints.Constraint;

/**
 * Clase que permite ejecutar una sentencia SQL para devolver una lista de objetos de la tabla deseada
 * con las restricciones definidas.
 * @author Carlos
 *
 */
public class Query {
	private LibreriaBBDD lib;
	private Class<?> clase;				//Clase a la que se aplican las constraint
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
	 * Devuelve la sentencia SQL a ejecutar con la constraint creada.
	 * @param con (Conexión con la BD).
	 * @return Sentencia SQL.
	 * @throws SQLException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 * @throws SecurityException 
	 * @throws NoSuchFieldException 
	 */
	protected String toSql(Connection con) throws SQLException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {

		//PARTE SELECT
		String sqlStatement = "SELECT t1.id,";
		Field[] campos = this.clase.getDeclaredFields();
		List<String> selectCampos = new ArrayList<String>();
		for(int i = 0; i < (campos.length); i++){
			campos[i].setAccessible(true);
			if(!campos[i].getType().isAssignableFrom(List.class) && !campos[i].getType().isAssignableFrom(Set.class)){
				selectCampos.add("t1." + campos[i].getName());
			}
			else{
				selectCampos.add("t1.2_" + campos[i].getName());
			}
		}
		sqlStatement+= StringUtils.join(selectCampos,",");
		//FIN PARTE SELECT
		
		sqlStatement += " FROM " + this.lib.getTableName(this.clase.getName()) + " t1";
		List<String> listaTablas = new ArrayList<String>();
		List<String> listaCampos = new ArrayList<String>();
		List<String> listaIndices = new ArrayList<String>();
		int ind = 0;
		String constraintSQL = this.constraintToSql(this.restriccion, listaTablas, listaCampos, listaIndices);
		for(int i = 0; i < listaTablas.size(); i++){
			sqlStatement += " LEFT JOIN " + listaTablas.get(i) 
							+ " t" + (i+2) + " ON " + listaIndices.get(i) + "." + listaCampos.get(i) 
							+ " = " + "t" + (i+2) + ".id";
		}
		
		sqlStatement += " WHERE " + constraintSQL;
				
		return sqlStatement;
	}
	
	private String constraintToSql(Constraint c, List<String> lt, List<String> lc, List<String> li) throws SQLException, NoSuchFieldException, SecurityException{
		
		if(c.getClass().getName().contains("NotConstraint")){
			return "NOT " + constraintToSql(c.getInnerConstraint().get(0),lt,lc,li);
		}
		else if(c.getClass().getName().contains("AndConstraint")){
			List<String> ls = new ArrayList<String>();
			for(Constraint con: c.getInnerConstraint()){
				ls.add(constraintToSql(con, lt,lc,li));
			}
			return StringUtils.join(ls," AND ");
		}
		else if(c.getClass().getName().contains("OrConstraint")){
			List<String> ls = new ArrayList<String>();
			for(Constraint con: c.getInnerConstraint()){
				ls.add(constraintToSql(con, lt,lc,li));
			}
			return StringUtils.join(ls," OR ");	
		}
		else{
			String constraint = "";
			String[] campos = StringUtils.split(c.getCampo(),".");
			Field campoActual = null;
			Class<?> clase = this.clase;
			for(String s: campos){
				campoActual = clase.getDeclaredField(s);
				clase = campoActual.getType();
			}
			if(!campoActual.getType().isAssignableFrom(List.class)){
				li.add("t1");
				campoActual = null;
				clase = this.clase;
				for(String s: campos){
					campoActual = clase.getDeclaredField(s);
					clase = campoActual.getType();
					if(!this.lib.atributoBasico(clase)){
						lt.add(this.lib.getTableName(clase.getName()));
						lc.add(campoActual.getName());
					}
				}
				for(int i = 1; i < campos.length-1; i++)
					li.add("t" + (li.size()+1));
				int index = lt.size()+1;
				if(!this.lib.atributoBasico(campoActual.getType())) index--;
				constraint = "t" + index + "." + campoActual.getName() + " = ?";
			}
			else{
				Type friendsGenericType = campoActual.getGenericType();
				ParameterizedType friendsParameterizedType = (ParameterizedType) friendsGenericType;
				Type[] friendsType = friendsParameterizedType.getActualTypeArguments();
				Class<?> userClass = (Class<?>) friendsType[0];
				
				//SOLO SIRVE PARA CLASES DE TIPO NO BASICO
				constraint += "EXISTS (SELECT ";
				String nombreTablaClase = this.lib.getTableName(this.clase.getName());
				String nombreTablaList = nombreTablaClase +  "_" + campoActual.getName();
				String nombreTablaTipoList = this.lib.getTableName(userClass.getName());
				constraint += "ntl.id1_" + nombreTablaClase;
				constraint += " FROM " + nombreTablaList + " ntl JOIN " + nombreTablaTipoList + " nt ON ntl.id2_" 
							+ nombreTablaTipoList + " = nt.id WHERE " + "nt.id = ? AND " + "ntl.id1_" + nombreTablaClase + " = t1.id";
				
				
				constraint += ")";
			}
			
			return "( " + constraint + " )";
		}
	}
	
	/**
	 * Ejecuta la sentencia SQL con las constraint creadas y devuelve una lista con los resultados.
	 * @param con (Conexión con la BD).
	 * @param idMap 
	 * @return List que contiene los resultados de la consulta.
	 * @throws SQLException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws ClassNotFoundException 
	 * @throws SecurityException 
	 * @throws NoSuchFieldException 
	 * @throws IllegalArgumentException 
	 */
	protected List<Object> executeQuery(Connection con, int profundidad) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, NoSuchFieldException, SecurityException{
		ObjectCreator objCrtr = new ObjectCreator(this.lib);
		String sql = this.toSql(con); 									//Sentencia SQL a ejecutar
		List<Object> lista = new ArrayList<Object>(); 					//Lista en la que se introducirán los objetos
		PreparedStatement pst = con.prepareStatement(sql); 				// Preparación de la sentencia
		List<Object> values = this.restriccion.getValues(); 			// Lista de valores de las restricciones
		for (int i = 1; i <= values.size(); i++) { 						// Para cada valor:
			Object obj = values.get(i-1);
			if(!this.lib.atributoBasico(obj.getClass())){
				if(this.lib.constainsKeyObjectMap(obj))
					obj = this.lib.getId(obj);
				else
					obj = -1;
			}
			pst.setObject(i, obj); 						// Añadir el valor a la sentencia
		}
		System.out.println(pst);
		ResultSet rs = pst.executeQuery(); 								// Ejecución de la sentencia
		Object object; 													// Instancia de la clase 'clase'
		while (rs.next()) { 											// Mientras aún haya resultados de la sentencia SQL ejecutada
			Identificador iden=new Identificador(rs.getInt("id"), this.clase.getName());
			if(this.lib.constainsKeyIdMap(iden)){ 
				object = this.lib.getIdMap(iden);
			}
			else{ 														//Si no esta te creas el objeto y le añades al mapa
				object = objCrtr.createObject(this.clase,rs, profundidad); 								// Crea el objeto de la clase
			}
			lista.add(object); 											// Añadir el objeto a la lista que se devolverá
		}

		return lista;
	}
}
