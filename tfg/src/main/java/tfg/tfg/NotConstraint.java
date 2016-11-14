package tfg.tfg;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase para representar una restricción del tipo NOT Restricción
 * @author Victor y Alvaro
 *
 */
public class NotConstraint implements Constraint{

	private Constraint cons;	//Restricción
	
	public NotConstraint(Constraint c){
		this.cons = c;
	}
	
	/**
	 * Devuelve la restricción con un NOT delante. 
	 */
	public String toSql() {
		return "((NOT (" + this.cons.toSql() + "))";
	}

	/**
	 * Devuelve una lista con el valor de la restricción.
	 */
	public List<Object> getValues() {
		List<Object> l = new ArrayList<Object>();
		l.addAll(this.cons.getValues());
		return l;
	}
	
}