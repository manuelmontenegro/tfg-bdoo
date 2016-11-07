package tfg.tfg;

import java.util.ArrayList;
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
		if (this.cons.size() == 0) 						//Si no hay restricciones
			return "TRUE";
		
		List<String> list = new ArrayList<String>();			//Lista con el SQL de cada una de las restricciones
		for (int i = 0; i < this.cons.size(); i++) {			//Para cada una de las restricciones de la lista:
			list.set(i, this.cons.get(i).toSql());				//Añadir a la lista de String cada SQL de la lista de restricciones
		}
		String ret = StringUtils.join(list, " AND ");			//Unir las restricciones con un AND
		
		return "(" + ret + ")";
	}
	
}
