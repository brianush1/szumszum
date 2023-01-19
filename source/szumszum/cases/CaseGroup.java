package szumszum.cases;

import java.io.Serializable;

public class CaseGroup implements Serializable {
	private static final long serialVersionUID = 1L;

	public String name;
	public TestCase[] subCases;
	public double timeLimitPerCase;

}
