package tfg.tfg;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase para representar una restricción del tipo Restriccioón1 AND Restricción2
 * @author Victor y Alvaro
 *
 */
public class OrConstraint implements Constraint{

	private List<Constraint> cons;	//Primera restricción
	
	public OrConstraint(Constraint ...c){
		this.cons =new ArrayList<Constraint>();
		for(int i = 0; i < c.length; i++){
			cons.add(c[i]);
		}
	}
	
	/**
	 * Devuelve las dos restricciones unidas por un AND. 
	 */
	public String toSql() {
		if(this.cons.size() == 0)
			return "false";
		String ret = "(";
		for(int i = 0; i<cons.size(); i++){
			if(i != 0)
				ret += " OR ";
			ret +=this.cons.get(i).toSql();	
		}
		return ret+ ")";
	}
	
}