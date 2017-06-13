package oodb.constraints;

import java.util.ArrayList;
import java.util.List;

public class NotConstraint implements Constraint {

	private Constraint constraint;

	public NotConstraint(Constraint c) {
		this.constraint = c;
	}

	public String toSql() {
		return "((NOT (" + this.constraint.toSql() + "))";
	}

	public List<Object> getValues() {
		List<Object> l = new ArrayList<Object>();
		l.addAll(this.constraint.getValues());
		return l;
	}

	public List<Constraint> getInnerConstraint() {
		List<Constraint> l = new ArrayList<Constraint>();
		l.add(this.constraint);
		return l;
	}

	public String getField() {
		return null;
	}
}