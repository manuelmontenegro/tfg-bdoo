package oodb.constraints;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class AndConstraint implements Constraint {

	private List<Constraint> constraints;

	public AndConstraint(Constraint... c) {
		this.constraints = new ArrayList<Constraint>();
		for (int i = 0; i < c.length; i++) {
			constraints.add(c[i]);
		}
	}

	public AndConstraint(List<Constraint> l) {
		this.constraints = l;
	}

	public String toSql() {
		if (this.constraints.size() == 0)
			return "(TRUE)";
		List<String> list = new ArrayList<String>();
		for (int i = 0; i < this.constraints.size(); i++) {
			list.add(this.constraints.get(i).toSql());
		}
		String ret = StringUtils.join(list, " AND ");

		return "(" + ret + ")";
	}

	public List<Object> getValues() {
		List<Object> l = new ArrayList<Object>();
		for (Constraint c : constraints)
			l.addAll(c.getValues());
		return l;
	}

	public List<Constraint> getInnerConstraint() {
		return constraints;
	}

	public String getField() {
		return null;
	}
}