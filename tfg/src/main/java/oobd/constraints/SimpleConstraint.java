package oobd.constraints;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class SimpleConstraint implements Constraint {

	private String operator;
	private String field;
	private Object value;

	private SimpleConstraint(String o, String f, Object v) {
		operator = o;
		field = f;
		value = v;
	}

	public static SimpleConstraint equalConstraint(String field, Object value) {
		return new SimpleConstraint("=", field, value);
	}

	public static SimpleConstraint greaterThanConstraint(String field, Object value) {
		return new SimpleConstraint(">", field, value);
	}

	public static SimpleConstraint lessThanConstraint(String field, Object value) {
		return new SimpleConstraint("<", field, value);
	}

	public static SimpleConstraint greaterThanOrEqualsConstraint(String field, Object value) {
		return new SimpleConstraint(">=", field, value);
	}

	public static SimpleConstraint lessThanOrEqualsConstraint(String field, Object value) {
		return new SimpleConstraint("<=", field, value);
	}

	public static SimpleConstraint notEqualConstraint(String field, Object value) {
		return new SimpleConstraint("<>", field, value);
	}

	public String toSql() {
		String[] fields = StringUtils.split(this.field, ".");
		String classCond = "t" + fields.length + ".";
		if (value != null)
			return "(" + classCond + fields[fields.length - 1] + " " + operator + " ?)";
		else
			return "(" + field + " IS NULL)";
	}

	public List<Object> getValues() {
		List<Object> l = new ArrayList<Object>();
		if (this.value != null)
			l.add(this.value);
		return l;
	}

	public List<Constraint> getInnerConstraint() {
		return null;
	}

	public String getField() {
		return this.field;
	}
}
