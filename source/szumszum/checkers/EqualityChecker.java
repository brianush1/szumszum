package szumszum.checkers;

import java.io.Serializable;
import java.util.Arrays;

public class EqualityChecker implements IOutputChecker, Serializable {

	private static final long serialVersionUID = 1L;

	public Object expectedValue;

	public EqualityChecker(Object expectedValue) {
		this.expectedValue = expectedValue;
	}

	public boolean check(Object value) {
		if (expectedValue.getClass().isArray() && value.getClass().isArray()) {
			return Arrays.deepEquals((Object[]) expectedValue, (Object[]) value);
		} else {
			return expectedValue.equals(value);
		}
	}

}
