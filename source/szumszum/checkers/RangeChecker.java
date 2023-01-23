package szumszum.checkers;

import java.io.Serializable;

public class RangeChecker implements IOutputChecker, Serializable {

	private static final long serialVersionUID = 1L;

	public double minValue;
	public double maxValue;
	public boolean minInclusive;
	public boolean maxInclusive;

	public RangeChecker(double minValue, double maxValue, boolean minInclusive, boolean maxInclusive) {
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.minInclusive = minInclusive;
		this.maxInclusive = maxInclusive;
	}

	public boolean check(Object value) {
		double doubleValue = (double) value;
		return !((minInclusive
				? (doubleValue < minValue)
				: (doubleValue <= minValue))
				|| (maxInclusive
						? (doubleValue > maxValue)
						: (doubleValue >= maxValue)));
	}

	public String toString() {
		String result = "number ";
		if (minValue > Double.NEGATIVE_INFINITY) {
			if (minInclusive) result += ">="; else result += ">";
			result += " " + minValue;
		}
		if (minValue > Double.NEGATIVE_INFINITY && maxValue < Double.POSITIVE_INFINITY) {
			result += ", ";
		}
		if (maxValue < Double.POSITIVE_INFINITY) {
			if (maxInclusive) result += "<="; else result += "<";
			result += " " + maxValue;
		}
		return result;
	}

}
