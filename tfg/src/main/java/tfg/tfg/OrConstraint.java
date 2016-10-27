package tfg.tfg;

public class OrConstraint implements Constraint{

	private Constraint cons1;
	private Constraint cons2;
	
	public OrConstraint(Constraint c1, Constraint c2){
		cons1 = c1;
		cons2 = c2;
	}
	public String toSql() {
		return cons1.toSql() + " OR " + cons2.toSql();
	}
	
}
