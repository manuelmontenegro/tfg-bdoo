package tfg.tfg;

/***
 * Clase que representa una restricción simple del tipo "Campo - Operando - Valor".
 * @author Carlos
 *
 */
public class SimpleConstraint implements Constraint{
	
	private String operando;	//Operando de relación entre el campo y el valor
	private String campo;		//Campo al que se quiere aplicar la restricción
	private String valor;		//Valor de la restricción
	
	/***
	 * Constructor de la clase.
	 * @param op
	 * @param ca
	 * @param va
	 */
	private SimpleConstraint(String op, String ca, String va){
		operando = op;
		campo = ca;
		valor = va;
	}
	
	public static SimpleConstraint mayorQueConstraint(String campo, String valor){
		return new SimpleConstraint(">",campo,valor);
	}
	
	public static SimpleConstraint igualQueConstraint(String campo, String valor){
		return new SimpleConstraint("=",campo,valor);
	}
	
	public static SimpleConstraint menorQueConstraint(String campo, String valor){
		return new SimpleConstraint("<",campo,valor);
	}
	
	public static SimpleConstraint mayorIgualQueConstraint(String campo, String valor){
		return new SimpleConstraint(">=",campo,valor);
	}
	
	public static SimpleConstraint menorIgualQueConstraint(String campo, String valor){
		return new SimpleConstraint("<=",campo,valor);
	}
	
	public static SimpleConstraint distintoQueConstraint(String campo, String valor){
		return new SimpleConstraint("<>",campo,valor);
	}
	
	/***
	 * Devuelve la restricción en forma de sentencia SQL a incluir en la cláusula WHERE.
	 */
	public String toSql(){
		return "(" + campo + " " + operando + " " + valor + ")";
	}

}
