package constraints;

import java.util.List;

public interface Constraint {
	public String toSql();
	public List<Constraint> getInnerConstraint();
	public String getCampo();
	public List<Object> getValues();
}
