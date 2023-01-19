package szumszum.cases;
import java.io.Serializable;

import szumszum.checkers.IOutputChecker;

public class TestCase implements Serializable {
	private static final long serialVersionUID = 1L;

	public String name;
	public String methodName;
	public IOutputChecker returnChecker;
	public Object[] args;

}
