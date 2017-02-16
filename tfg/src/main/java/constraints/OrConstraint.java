package constraints;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * Clase para representar una restricción del tipo Restriccioón1 AND Restricción2
 * @author Carlos, Victor y Alvaro
 *
 */
public class OrConstraint implements Constraint{

	private List<Constraint> cons;	//Lista de restricciones
	
	public OrConstraint(Constraint ...c){
		this.cons = new ArrayList<Constraint>();		//Inicialización de la lista de restriccioes
		for(int i = 0; i < c.length; i++){				//Para cada restricción recibida:
			cons.add(c[i]);								//Añadir restricción a la lista
		}
	}
	
	/**
	 * Devuelve las restricciones unidas por un OR. 
	 */
	public String toSql() {
		if(this.cons.size() ==0 )								//Si no hay constraint
			return "(FALSE)";
		List<String> list = new ArrayList<String>();			//Lista con el SQL de cada una de las restricciones
		for (int i = 0; i < this.cons.size(); i++) {			//Para cada una de las restricciones de la lista:
			list.add(this.cons.get(i).toSql());					//Añadir a la lista de String cada SQL de la lista de restricciones
		}
		String ret = StringUtils.join(list, " OR ");			//Unir las restricciones con un AND
		
		return "(" + ret + ")";
	}

	/**
	 * Devuelve una lista con los valores de las restricciones de la lista.
	 */
	public List<Object> getValues() {
		List<Object> l = new ArrayList<Object>();	 	//Lista a devolver
		for(Constraint c: cons)							//Para cada constraint:
			l.addAll(c.getValues());					//Añadir los valores de la constraint a la lista
		return l;
	}

	@Override
	public String[] getOnConditions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getMultiplesAtributos() {
		// TODO Auto-generated method stub
		return null;
	}
	
}