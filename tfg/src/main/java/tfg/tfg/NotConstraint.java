package tfg.tfg;


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
	
}