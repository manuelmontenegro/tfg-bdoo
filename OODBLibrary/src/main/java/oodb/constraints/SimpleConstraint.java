package oodb.constraints;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * Basic constraint class that represents a simple constraint with a field, operator and value.
 */
public class SimpleConstraint implements Constraint {

	private String operator;
	private String field;
	private Object value;

	private SimpleConstraint(String o, String f, Object v) {
		operator = o;
		field = f;
		value = v;
	}

	/**
	 * Returns a simple constraint: field = value.
	 * @param field
	 * @param value
	 * @return
	 */
	public static SimpleConstraint newEqualConstraint(String field, Object value) {
		return new SimpleConstraint("=", field, value);
	}

	/**
	 * Returns a simple constraint: field > value.
	 * @param field
	 * @param value
	 * @return
	 */
	public static SimpleConstraint newGreaterThanConstraint(String field, Object value) {
		return new SimpleConstraint(">", field, value);
	}

	/**
	 * Returns a simple constraint: field < value.
	 * @param field
	 * @param value
	 * @return
	 */
	public static SimpleConstraint newLessThanConstraint(String field, Object value) {
		return new SimpleConstraint("<", field, value);
	}

	/**
	 * Returns a simple constraint: field >= value.
	 * @param field
	 * @param value
	 * @return
	 */
	public static SimpleConstraint newGreaterThanOrEqualsConstraint(String field, Object value) {
		return new SimpleConstraint(">=", field, value);
	}

	/**
	 * Returns a simple constraint: field <= value.
	 * @param field
	 * @param value
	 * @return
	 */
	public static SimpleConstraint newLessThanOrEqualsConstraint(String field, Object value) {
		return new SimpleConstraint("<=", field, value);
	}

	/**
	 * Returns a simple constraint: field != value.
	 * @param field
	 * @param value
	 * @return
	 */
	public static SimpleConstraint newNotEqualConstraint(String field, Object value) {
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
