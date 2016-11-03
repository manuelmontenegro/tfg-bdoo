package tfg.tfg;

/**
 * Clase para representar una restricción del tipo Restriccion1 OR Restriccion2.
 * @author Carlos
 *
 */
public class OrConstraint implements Constraint{

	private Constraint cons1;	//Primera restricción
	private Constraint cons2;	//Segunda restricción
	
	/**
	 * Constructor de la clase.
	 * @param c1
	 * @param c2
	 */
	public OrConstraint(Constraint c1, Constraint c2){
		cons1 = c1;
		cons2 = c2;
	}
	
	/**
	 * Devuelve las dos restricciones unidas por un OR.
	 */
	public String toSql() {
		return "(" + cons1.toSql() + " OR " + cons2.toSql() + ")";
	}
	
}
