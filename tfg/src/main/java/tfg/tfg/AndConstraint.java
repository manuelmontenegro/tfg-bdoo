package tfg.tfg;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase para representar una restricción del tipo Restriccioón1 AND Restricción2
 * @author Carlos
 *
 */
public class AndConstraint implements Constraint{

	private List<Constraint> cons;	//Primera restricción
	
	public AndConstraint(Constraint ...c){
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
			return "true";
		String ret = "(";
		for(int i = 0; i<cons.size(); i++){
			if(i != 0)
				ret += " AND ";
			ret +=this.cons.get(i).toSql();	
		}
		return ret+ ")";
	}
	
}
