package tfg.tfg;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase para representar una restricción del tipo Restriccioón1 AND Restricción2
 * @author Victor y Alvaro
 *
 */
public class NotConstraint implements Constraint{

	private Constraint cons;	//Primera restricción
	
	public NotConstraint(Constraint c){
		this.cons =c;
	}
	
	/**
	 * Devuelve las dos restricciones unidas por un AND. 
	 */
	public String toSql() {

		String ret = " Not (";
		ret +=this.cons.toSql();	
		return ret+ ")";
	}
	
}