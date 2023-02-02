package org.jboss.nexus;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonatype.nexus.repository.storage.Component;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class FilterTest {

	@Mock
	private Component component;

	@Test
	public void parseFilterString() {
		// TODO: 01.02.2023 all the testing
	}




	@Test
	public void checkComponentNull() {
		Filter tested = Filter.parseFilterString(null);
		assertTrue(tested.checkComponent(component));
	}


	@Test
	public void checkComponentGAV() {
		Filter tested = Filter.parseFilterString("group=org.jboss&artifact=nexus&version=2.3.1");
		when(component.group()).thenReturn("org.jboss");
		when(component.name()).thenReturn("nexus");
		when(component.version()).thenReturn("2.3.1");
		assertTrue(tested.checkComponent(component));

		tested = Filter.parseFilterString("group=com.redhat&artifact=nexus&version=2.3.1");
		assertFalse(tested.checkComponent(component));

		tested = Filter.parseFilterString("group=org.jboss&artifact=eap&version=2.3.1");
		assertFalse(tested.checkComponent(component));

		tested = Filter.parseFilterString("group=org.jboss&artifact=nexus&version=2.3.2");
		assertFalse(tested.checkComponent(component));

		tested = Filter.parseFilterString("group=org.jboss&artifact=nexus&version>=2.3.2");
		assertFalse(tested.checkComponent(component));

		tested = Filter.parseFilterString("group=org.jboss&artifact=nexus&version>=2.3.0");
		assertTrue(tested.checkComponent(component));

		tested = Filter.parseFilterString("group=org.jboss&artifact=nexus&version>=2.3.1");
		assertTrue(tested.checkComponent(component));

		tested = Filter.parseFilterString("group=org.jboss&artifact=nexus&version<=2.3.2");
		assertTrue(tested.checkComponent(component));

		tested = Filter.parseFilterString("group=org.jboss&artifact=nexus&version<=2.3.0");
		assertFalse(tested.checkComponent(component));

		tested = Filter.parseFilterString("group=org.jboss&artifact=nexus&version<=2.3.1");
		assertTrue(tested.checkComponent(component));

		tested = Filter.parseFilterString("group=org.jboss&artifact=nexus&version!=2.3.1");
		assertFalse(tested.checkComponent(component));

		tested = Filter.parseFilterString("group=org.jboss&artifact=nexus&version <> 2.3.1");
		assertFalse(tested.checkComponent(component));

	}




	
	@Test
	public void parseFilterStringAll() {
		Filter tested = Filter.parseFilterString("group=org.jboss&artifact=nexus&version>=2.3.1&tag=TO_DEPLOY&tagAttr=OS<>MacOS");
		assertNull(tested.getUnspecified());
		assertEquals("org.jboss", tested.getGroup());
		assertEquals("nexus", tested.getArtifact());
		assertEquals("2.3.1", tested.getVersion());
		assertEquals(Filter.LogicalOperation.Operator.GE, tested.getVersionOperation());
		assertEquals("TO_DEPLOY", tested.getTag());
		assertEquals(Filter.LogicalOperation.Operator.EQ, tested.getTagOperation());
		assertEquals("OS", tested.getTagAttr());
		assertEquals(Filter.LogicalOperation.Operator.NE, tested.getTagAttrOperation());
		assertEquals("MacOS", tested.getTagAttrValue());
	}

	@Test
	public void parseFilterStringAllWithNameAndSpaces() {
		Filter tested = Filter.parseFilterString("group    =     org.jboss&name = nexus&version>=2.3.1&  tag=       TO_DEPLOY& tagAttr= OS <>    MacOS");
		assertNull(tested.getUnspecified());
		assertEquals("org.jboss", tested.getGroup());
		assertEquals("nexus", tested.getArtifact());
		assertEquals("2.3.1", tested.getVersion());
		assertEquals(Filter.LogicalOperation.Operator.GE, tested.getVersionOperation());
		assertEquals("TO_DEPLOY", tested.getTag());
		assertEquals(Filter.LogicalOperation.Operator.EQ, tested.getTagOperation());
		assertEquals("OS", tested.getTagAttr());
		assertEquals(Filter.LogicalOperation.Operator.NE, tested.getTagAttrOperation());
		assertEquals("MacOS", tested.getTagAttrValue());
	}

	@Test(expected = Filter.ParseException.class)
	public void parseFilterStringTagAttrWrong() {
		try {
			Filter.parseFilterString("tagAttr = no operand");
		} catch (Filter.ParseException e ) {
			assertEquals("Incorrect filter expression: Missing operator in tagAttr: tagAttr = no operand", e.getMessage());
			throw e;
		}
	}

	@Test(expected = Filter.ParseException.class)
	public void parseFilterStringTypo() {
		try {
			//noinspection SpellCheckingInspection
			Filter.parseFilterString("goup=org.jboss&artifact=nexus&version>=2.3.1&tag=TO_DEPLOY&tagAttr=OS<>MacOS"); // misspelled group
		} catch (Filter.ParseException e ) {
			assertEquals("Incorrect filter expression: allowed fields are group, artifact, name, version, tag, tagAttr: goup=org.jboss", e.getMessage());
			throw e;
		}

	}


	@Test
	public void parseFilterStringUnspecified() {
		Filter tested = Filter.parseFilterString("kie-releases");

		assertEquals("kie-releases", tested.getUnspecified());
		assertNull(tested.getGroup());
		assertNull(tested.getArtifact());
		assertNull(tested.getVersion());
		assertNull(tested.getVersionOperation());
		assertNull(tested.getTag());
		assertNull(tested.getTagOperation());
		assertNull(tested.getTagAttr());
		assertNull(tested.getTagAttrOperation());
		assertNull(tested.getTagAttrValue());
	}

	@Test(expected = Filter.ParseException.class)
	public void parseFilterStringUnspecifiedTwice() {
		try {
			Filter.parseFilterString("kie-releases&yet-another");
		} catch (Filter.ParseException e ) {
			assertEquals("Incorrect filter expression: free text expression can only be used once: kie-releases&yet-another", e.getMessage());
			throw e;
		}
	}

	@Test
	public void logicalOperationParseString() {
		String[] operatorStrings = {"=", "!=", "<>", "<", ">", "<=", ">="};
		Filter.LogicalOperation.Operator[] operators = {Filter.LogicalOperation.Operator.EQ, Filter.LogicalOperation.Operator.NE, Filter.LogicalOperation.Operator.NE, Filter.LogicalOperation.Operator.LT, Filter.LogicalOperation.Operator.GT, Filter.LogicalOperation.Operator.LE, Filter.LogicalOperation.Operator.GE };

		for (int i = 0; i < operatorStrings.length; i++) {
			Filter.LogicalOperation logicalOperation = Filter.LogicalOperation.parseString("   xy "+operatorStrings[i]+"    ZYX    ");
			assertEquals(operators[i] ,logicalOperation.getOperator());
			assertEquals("xy" ,logicalOperation.getAttribute());
			assertEquals("ZYX" ,logicalOperation.getValue());

			logicalOperation = Filter.LogicalOperation.parseString("xy"+operatorStrings[i]+"ZYX");
			assertEquals(operators[i] ,logicalOperation.getOperator());
			assertEquals("xy" ,logicalOperation.getAttribute());
			assertEquals("ZYX" ,logicalOperation.getValue());
		}
	}

	@Test(expected = Filter.ParseException.class)
	public void logicalOperationParseStringMissingAttributeError() {
		try {
			Filter.LogicalOperation.parseString("<= j");
		} catch (Filter.ParseException e) {
			assertEquals("Attribute name was missing in the logical expression in filter: <= j", e.getMessage());
			throw e;
		}
	}

	@Test(expected = Filter.ParseException.class)
	public void logicalOperationParseStringEmptyStringError() {
		try {
			Filter.LogicalOperation.parseString("   ");
		} catch (Filter.ParseException e) {
			assertEquals("Empty expression defined in a filter", e.getMessage());
			throw e;
		}
	}

	@Test(expected = Filter.ParseException.class)
	public void logicalOperationParseStringMissingValueError() {
		try {
			Filter.LogicalOperation.parseString("   xy =");
		} catch (Filter.ParseException e) {
			assertEquals("Missing value in the logical expression in filter:    xy =", e.getMessage());
			throw e;
		}
	}

	@Test()
	public void logicalOperationParseStringNoOperandError() {
			Filter.LogicalOperation logicalOperation = Filter.LogicalOperation.parseString("   xy    ");
			assertEquals(Filter.LogicalOperation.Operator.NOOP, logicalOperation.getOperator());
			assertEquals("   xy    ", logicalOperation.getValue());
			assertEquals("", logicalOperation.getAttribute());
	}

}
