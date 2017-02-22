package constraints;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

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
		String[] campos = StringUtils.split(this.campo,".");
		String classCond = "t" + campos.length + ".";
		if(valor != null)
			return "(" + classCond + campos[campos.length-1] + " " + operando + " ?)";
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

	public String[] getOnConditions(){
		String[] classes = StringUtils.split(this.campo,".");
		String[] ret = new String[classes.length - 1];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = "t" + (i+1) + "." + classes[i] + " = " + "t" + (i+2) + ".id ";
		}
		return ret;
	}

	/**
	 * devuelve un array con atributos no simples de la condiccion
	 * @return array con atributos no simples de la condiccion
	 */
	public String[] getMultiplesAtributos() {
		String[] campos = StringUtils.split(this.campo,".");
		String[] ret = new String[campos.length-1];
		for (int i = 0; i < ret.length; i++){
			ret[i] = campos[i];	
		}
		return ret;
	}

}
