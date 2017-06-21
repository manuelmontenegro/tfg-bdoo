package oodb.constraints;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * Implements the Constraint interface to represent the intersection between two or more constraints.
 */
public class AndConstraint implements Constraint {

	private List<Constraint> constraints;

	/**
	 * Class constructor. Admits one or more Constraint objects.
	 * @param c
	 */
	public AndConstraint(Constraint... c) {
		this.constraints = new ArrayList<Constraint>();
		for (int i = 0; i < c.length; i++) {
			constraints.add(c[i]);
		}
	}

	/**
	 * Class constructor. Admits a list of Constraint objects.
	 * @param l
	 */
	public AndConstraint(List<Constraint> l) {
		this.constraints = l;
	}

	/**
	 * Returns the SQL statement that represents the constraint.
	 */
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

	/**
	 * Returns a List of values of each constraint.
	 */
	public List<Object> getValues() {
		List<Object> l = new ArrayList<Object>();
		for (Constraint c : constraints)
			l.addAll(c.getValues());
		return l;
	}

	/**
	 * Returns the List of constraints.
	 */
	public List<Constraint> getInnerConstraint() {
		return constraints;
	}

	/**
	 * Useless for this constraint.
	 */
	public String getField() {
		return null;
	}
}
