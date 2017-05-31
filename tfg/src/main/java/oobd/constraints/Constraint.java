package oobd.constraints;

import java.util.List;

public interface Constraint {
	public String toSql();

	public List<Constraint> getInnerConstraint();

	public String getField();

	public List<Object> getValues();
}
