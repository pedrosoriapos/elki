package de.lmu.ifi.dbs.utilities.optionhandling.constraints;

import de.lmu.ifi.dbs.logging.AbstractLoggable;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.utilities.optionhandling.NumberParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

/**
 * Represents a Less-Than global parameter constraint. The value of the first
 * number parameter ({@link NumberParameter}) given has to be less than the value of the second 
 * number parameter ({@link NumberParameter}) given.
 * 
 * @author Steffi Wanka
 * 
 */
public class LessGlobalConstraint<T extends Number> extends AbstractLoggable implements GlobalParameterConstraint {

	/**
	 * First number parameter.
	 */
	private NumberParameter<T,?> first;

	/**
	 * Second number parameter.
	 */
	private NumberParameter<T,?> second;

	/**
	 * Creates a Less-Than global parameter constraint.
	 * That is the value of the
	 * first number parameter given has to be less than the value of the second
	 * number parameter given.
	 * 
	 * @param first
	 *            first number parameter
	 * @param second
	 *            second number parameter
	 */
	public LessGlobalConstraint(NumberParameter<T,?> first, NumberParameter<T,?> second) {
        super(LoggingConfiguration.DEBUG);
		this.first = first;
		this.second = second;
	}

	/**
	 * Checks if the value of the first number parameter is less than the value
	 * of the second number parameter. If not, a parameter exception is thrown.
	 * 
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.constraints.GlobalParameterConstraint#test()
	 */
	public void test() throws ParameterException {

		if (first.isSet() && second.isSet()) {

			if (first.getNumberValue().doubleValue() >= second.getNumberValue().doubleValue()) {

				throw new WrongParameterValueException("Global Parameter Constraint Error: \n" + "The value of parameter \""
						+ first.getName() + "\" has to be less than the" + "value of parameter \"" + second.getName() + "\"" +
								"(Current values: "+first.getName()+": "+first.getNumberValue().doubleValue() +", "+second.getName() +": "+second.getNumberValue().doubleValue()+ ")\n");
			}
		}

	}

}
