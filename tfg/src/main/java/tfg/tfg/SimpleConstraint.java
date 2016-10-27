package tfg.tfg;

public class SimpleConstraint implements Constraint{
	
	private String operando;
	private String campo;
	private String valor;
	
	public static SimpleConstraint mayorQueConstraint(String campo, String valor){
		return new SimpleConstraint(">",campo,valor);
	}
	
	public static SimpleConstraint igualQueConstraint(String campo, String valor){
		return new SimpleConstraint("=",campo,valor);
	}
	
	private SimpleConstraint(String op, String ca, String va){
		operando = op;
		campo = ca;
		valor = va;
	}
	
	public String toSql(){
		return campo + " " + operando + " " + valor;
	}

}
