package tfg.tfg;

import java.util.ArrayList;
import java.util.List;

/***
 * Clase que representa una restricción simple del tipo "Campo - Operando - Valor".
 * @author Carlos
 *
 */
public class SimpleConstraint implements Constraint{
	
	private String operando;	//Operando de relación entre el campo y el valor
	private String campo;		//Campo al que se quiere aplicar la restricción
	private Object valor;		//Valor de la restricción
	
	/***
	 * Constructor de la clase.
	 * @param op
	 * @param ca
	 * @param va
	 */
	private SimpleConstraint(String op, String ca, Object va){
		operando = op;
		campo = ca;
		valor = va;
	}
	
	public static SimpleConstraint mayorQueConstraint(String campo, Object valor){
		return new SimpleConstraint(">",campo,valor);
	}
	
	public static SimpleConstraint igualQueConstraint(String campo, Object valor){
		return new SimpleConstraint("=",campo,valor);
	}
	
	public static SimpleConstraint menorQueConstraint(String campo, Object valor){
		return new SimpleConstraint("<",campo,valor);
	}
	
	public static SimpleConstraint mayorIgualQueConstraint(String campo, Object valor){
		return new SimpleConstraint(">=",campo,valor);
	}
	
	public static SimpleConstraint menorIgualQueConstraint(String campo, Object valor){
		return new SimpleConstraint("<=",campo,valor);
	}
	
	public static SimpleConstraint distintoQueConstraint(String campo, Object valor){
		return new SimpleConstraint("<>",campo,valor);
	}
	
	/***
	 * Devuelve la restricción en forma de sentencia SQL a incluir en la cláusula WHERE.
	 */
	public String toSql(){
		if(valor != null)
			return "(" + campo + " " + operando + " ?)";
		else
			return "(" + campo + " IS NULL)";
	}

	/**
	 * Devuelve una lista con el valor de la restricción.
	 */
	public List<Object> getValues() {
		List<Object> l = new ArrayList<Object>();
		if(this.valor != null)
			l.add(this.valor);
		return l;
	}

}
