package test.util.configuration;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import test.util.configuration.RepeatTest;
import test.util.configuration.RepeatableTestStatement;
 
public class RepeatedTestRule implements TestRule
{
	@Override
	public Statement apply(Statement statement, Description description)
	{
		Statement result = statement;
		RepeatTest repeat = description.getAnnotation(RepeatTest.class);
		if(repeat != null)
		{
			int times = repeat.times();
			result = new RepeatableTestStatement(times, statement);
		}
		return result;
	}
}
