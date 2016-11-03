package tfg.tfg;

/**
 * Clase para representar una restricción del tipo Restriccioón1 AND Restricción2
 * @author Carlos
 *
 */
public class AndConstraint implements Constraint{

	private Constraint cons1;	//Primera restricción
	private Constraint cons2;	//Segunda restricción
	
	public AndConstraint(Constraint c1, Constraint c2){
		cons1 = c1;
		cons2 = c2;
	}
	
	/**
	 * Devuelve las dos restricciones unidas por un AND. 
	 */
	public String toSql() {
		return "(" + cons1.toSql() + " AND " + cons2.toSql() + ")";
	}
	
}
