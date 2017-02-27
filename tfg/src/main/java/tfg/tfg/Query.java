package tfg.tfg;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
	
	private String getTableName(String className) throws SQLException{
		Connection con=this.lib.getConnection();
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
		//EJEMPLO: restriccion: campo1.campo2.campo3 = X AND campo1.campo2 = Y
		
		//PARTE SELECT...FROM TABLA T1
		String sqlStatement = "SELECT t1.id,";
		Field[] campos = this.clase.getDeclaredFields();
		for(int i = 0; i < (campos.length-1); i++){
			sqlStatement+="t1." + campos[i].getName() + ",";
		}
		sqlStatement+="t1." + campos[campos.length-1].getName();
		sqlStatement += " FROM ";
		String tableName = this.getTableName(con);
		sqlStatement += tableName + " t1 ";
		//FIN PARTE SELECT...FROM TABLA T1
		
		List<String> camposRestriccion = this.restriccion.getCampos(); //Lista que contiene: {campo1.campo2.campo3, campo1.campo2}
		List<String> tablasCampos = new ArrayList<String>();	//Lista para guardar las tablas que corresponden a cada campo, al final de ejecutar queda: {tablacampo1, tablacampo2, tablacampo3, tablacampo4, tablacampo5, tablacampo6}
		List<Integer> indiceTablas = new ArrayList<Integer>(); //Lista para guardar los indices de las tablas anteriores, por ejemplo para la tabla de campo1 (que tiene indice t2) guarda t1 que es el t necesario para hacer t1.campo1 = t2.id
		List<String> camposTablas = new ArrayList<String>(); //Lista para guardar el nombre de los campos que se necesitan para hacer la condición de los LEFT JOIN, por ejemplo campo1 para hacer el t1.campo1 = t2.id
		List<String> whereCondiciones = new ArrayList<String>(); //Lista con las condiciones que van a ir en el where, en el caso del ejemplo guarda: {campo3 = ?, campo2 = ?}
		int index = 0; //indice necesario para saber donde estan los ultimos campos de cada conjunto, se usa en la linea 132
		
		for(String s: camposRestriccion){
			String[] split = StringUtils.split(s,"."); //dividimos los campos por los puntos
			Class<?> c = this.clase; //La clase en la que tenemos que buscar el primer campo es la de la consulta
			Field campoActual = null; //esta variable es para facilitar todo, si no es muy lioso
			int i = 0; //indice para recorrer el array de los campos split
			if(index != 0) index--; //Resta necesaria para ajustar la t que corresponde a cada ultima tabla de cada conjunto. Sin esta resta, en el ejemplo, en where condiciones se guardaria que campo2 del segundo conjunto pertenece a la tabla t6 cuando en realidad es t5. Sirve para ajustar eso
			while(!c.getCanonicalName().equalsIgnoreCase("Java.lang.String") && !c.getCanonicalName().equalsIgnoreCase("Int")){ //Mientras la clase que estamos analizando no sea basica
				for (Field f: c.getDeclaredFields()) { //Para cada campo de esa clase buscamos el que se llame igual que el del indice i del conjunto y lo guardamos en la variable campoActual para usarlo despues
					if(f.getName().equalsIgnoreCase(split[i])){
						campoActual = f;
					}
				}
				c = campoActual.getType(); //Ahora la clase que vamos a analizar es la del campo que estabamos buscando
				if(!c.getCanonicalName().equalsIgnoreCase("Java.lang.String") && !c.getCanonicalName().equalsIgnoreCase("Int")){ //Si esa clase no es basica
					tablasCampos.add(this.getTableName(campoActual.getType().getName())); //Buscamos su tabla y la guardamos en la lista de tablas
					indiceTablas.add(i+1); //guardamos el indice de la tabla de la clase que estabamos analizando antes (para hacer el t1.campo1 = t2.id)
					camposTablas.add(campoActual.getName()); //guardamos el nombre del campo (para hacer el t1.campo1 = t2.id)
				}
				i++; //aumentamos i
				index++; //aumentamos index
			}
			//esta parte se ejecuta cuando hemos llegado al ultimo campo del conjunto
			whereCondiciones.add("t" + index +"."+campoActual.getName()); //guardamos en la lista de where: t + index (que correspondera a la tabla del ultimo campo) . nombre del campo que estabamos analizando. Es decir se guarda t3.campo3
		}
		for(int i=0; i < tablasCampos.size(); i++){ //construir los left join usando todas las listas de antes
			sqlStatement += " LEFT JOIN " + tablasCampos.get(i) + " t" + (i+2) + " ON " + "t" + indiceTablas.get(i) + "." + camposTablas.get(i) + " = " + "t" + (i+2) + ".id "; 
		}
		
		sqlStatement += " WHERE ";
		List<String> clausulasWhere  = new ArrayList<String>();
		
		//construccion de las clausulas where usando la tabla de condiciones
		for(String st: whereCondiciones)
			clausulasWhere.add(st + " = ?"); 
		
		//unimos las condiciones con la union que tenga la restriccion (en el caso de AND/OR se unen condicion1 AND/OR condicion2. Si es simple no se unen mediante nada, solo habria una condicion en la lista)
		sqlStatement += StringUtils.join(clausulasWhere, this.restriccion.getUnion());;
		
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
	private Object createObject(Class<?> c, ResultSet rs) throws InstantiationException, IllegalAccessException, SQLException{
		System.out.println(c.getTypeName());
		Object o = c.newInstance();
		Identificador idenO=new Identificador((int)rs.getInt("id"), c.getCanonicalName());//identificar del objeto a crear por ahora el objeto esta vacio
		this.lib.putIdMap(idenO, o);//insertar ese objeto vacio en el mapa
		this.lib.putObjectMap(o, (int)rs.getInt("id"));//al ser el objeto un puntero se va a ir actualizando con el paso de este metodo
		
		//System.out.println(o.getClass().getName());
		Field[] campos = o.getClass().getDeclaredFields();		//Obtener los campos del objecto
		for(Field f: campos){									//Para cada uno de los campos:
			Object campo = rs.getObject(f.getName());			//Obtener de la BD el valor del campo
			f.setAccessible(true);								//Permitir acceder a campos privados
			
			if(!f.getType().getCanonicalName().contains("java.lang.String") && !f.getType().getCanonicalName().contains("int")) //Si el campo no es ni int ni string:
			{
				if(campo!=null){
					//System.out.println(campo);
					Identificador iden=new Identificador((int)campo, f.getType().getCanonicalName());
					//System.out.println(" IDENTIFICADOR: id:"+campo+" clase:"+f.getType().getCanonicalName());
					if(this.lib.constainsKeyIdMap(iden)){ 
						campo=this.lib.getIdMap(iden);
					}
					else {
						String tn = this.getTableName(f.getType().getCanonicalName());
						String sqlStatement = "SELECT * FROM " + tn + " WHERE ID = ?";
						Connection con = lib.getConnection();
						PreparedStatement pst;
						System.out.println(sqlStatement+" "+campo);
						pst = con.prepareStatement(sqlStatement); 			// Preparación de la sentencia
						pst.setInt(1, (int) campo);
						ResultSet rset = pst.executeQuery(); 					// Ejecución de la sentencia
						if(rset.next()){
							System.out.println(f.getType());
							campo = this.createObject(f.getType(),rset);
						}
						else
							campo=null;
						con.close();
					}
				}
			}
			//System.out.println("añadir a "+o.getClass().getSimpleName()+" "+campo+f.getType().getCanonicalName());
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
			}
			lista.add(object); 											// Añadir el objeto a la lista que se devolverá
		}

		return lista;
	}
}
