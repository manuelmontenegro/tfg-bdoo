package tfg.tfg;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * Clase para representar una restricción del tipo Restriccioón1 AND Restricción2
 * @author Carlos
 *
 */
public class AndConstraint implements Constraint{

	private List<Constraint> cons;	//Primera restricción
	
	public AndConstraint(Constraint ...c){
		this.cons = new ArrayList<Constraint>();		//Inicialización de la lista de restricciones
		for (int i = 0; i < c.length; i++) {			//Para cada restricción recibida:
			cons.add(c[i]);								//Añadir restricción a la lista
		}
	}
	
	/**
	 * Devuelve las restricciones unidas por un AND. 
	 */
	public String toSql() {
		List<String> list = new ArrayList<String>();			//Lista con el SQL de cada una de las restricciones
		for (int i = 0; i < this.cons.size(); i++) {			//Para cada una de las restricciones de la lista:
			list.add(this.cons.get(i).toSql());					//Añadir a la lista de String cada SQL de la lista de restricciones
		}
		String ret = StringUtils.join(list, " AND ");			//Unir las restricciones con un AND
		
		return "(" + ret + ")";
	}

	/**
	 * Devuelve una lista con los valores de las restricciones de la lista.
	 */
	public List<Object> getValues() {
		List<Object> l = new ArrayList<Object>();
		for(Constraint c: cons)
			l.addAll(c.getValues());
		return l;
	}
	
}
