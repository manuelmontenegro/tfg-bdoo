package constraints;

import java.util.List;

public interface Constraint {
	public String toSql();
	public List<Object> getValues();
	public String[] getOnConditions();
	public String[] getMultiplesAtributos();
}
