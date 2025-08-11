package org.jboss.nexus;

import com.sonatype.nexus.tags.Tag;

import org.junit.Test;
import org.junit.platform.commons.util.StringUtils;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonatype.nexus.common.collect.NestedAttributesMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class FilterTest {

	@Test
	public void parseFilterStringAll() {

		Filter tested = Filter.parseFilterString("group=org.jboss&artifact=nexus&version>=2.3.1&tag=TO_DEPLOY&tagAttr=OS<>MacOS");
		assertNull(tested.getUnspecified());
		assertEquals(1, tested.getGroupFilters().size());
		assertEquals("org.jboss", tested.getGroupFilters().get(0).getValue());
		assertEquals(Filter.LogicalOperation.Operator.EQ, tested.getGroupFilters().get(0).getOperator());

		assertEquals(1, tested.getArtifactFilters().size());
		assertEquals("nexus", tested.getArtifactFilters().get(0).getValue());
		assertEquals(Filter.LogicalOperation.Operator.EQ, tested.getArtifactFilters().get(0).getOperator());

		assertEquals(1, tested.getVersionFilters().size());
		assertEquals("2.3.1", tested.getVersionFilters().get(0).getValue());
		assertEquals(Filter.LogicalOperation.Operator.GE, tested.getVersionFilters().get(0).getOperator());

		assertEquals(1, tested.getTagOperations().size());
		assertEquals("TO_DEPLOY", tested.getTagOperations().get(0).getValue());
		assertEquals(Filter.LogicalOperation.Operator.EQ, tested.getTagOperations().get(0).getOperator());

		List<Filter.LogicalOperation> tagExpressions = tested.getTagAttributeOperations();
		assertEquals(1, tagExpressions.size());
		assertEquals("OS", tagExpressions.get(0).getAttribute());
		assertEquals(Filter.LogicalOperation.Operator.NE, tagExpressions.get(0).getOperator());
		assertEquals("MacOS", tagExpressions.get(0).getValue());
	}

	@Test
	public void parseFilterStringMultipleGroups() {
		Filter tested = Filter.parseFilterString("group=org.jboss&group!=org.jboss.unwanted");
		assertNull(tested.getUnspecified());
		assertTrue(tested.getArtifactFilters().isEmpty());
		assertTrue(tested.getVersionFilters().isEmpty());
		assertTrue(tested.getTagOperations().isEmpty());
		assertTrue(tested.getTagAttributeOperations().isEmpty());
		assertNull(tested.getLatestComponentTime());

		assertEquals(2, tested.getGroupFilters().size());
		assertEquals("org.jboss", tested.getDatabaseSearchParameters().get("groupId1"));
		assertEquals("org.jboss.unwanted", tested.getDatabaseSearchParameters().get("groupId2"));

		assertEquals("namespace = #{filterParams.groupId1} AND namespace != #{filterParams.groupId2}", tested.getDatabaseSearchString());
	}

	@Test
	public void parseFilterStringMultipleArtifacts() {
		Filter tested = Filter.parseFilterString("name!=wildfly-dist&artifact!=wildfly-unwanted");
		assertNull(tested.getUnspecified());
		assertTrue(tested.getGroupFilters().isEmpty());
		assertTrue(tested.getVersionFilters().isEmpty());
		assertTrue(tested.getTagOperations().isEmpty());
		assertTrue(tested.getTagAttributeOperations().isEmpty());
		assertNull(tested.getLatestComponentTime());

		assertEquals(2, tested.getArtifactFilters().size());
		assertEquals("wildfly-dist", tested.getDatabaseSearchParameters().get("name1"));
		assertEquals("wildfly-unwanted", tested.getDatabaseSearchParameters().get("name2"));

		assertEquals("name != #{filterParams.name1} AND name != #{filterParams.name2}", tested.getDatabaseSearchString());
	}


	@Test
	public void parseFilterStringMultipleVersion() {
		Filter tested = Filter.parseFilterString("version!=1.2&version<=10&version>0.5.7");
		assertNull(tested.getUnspecified());
		assertTrue(tested.getGroupFilters().isEmpty());
		assertTrue(tested.getArtifactFilters().isEmpty());
		assertTrue(tested.getTagOperations().isEmpty());
		assertTrue(tested.getTagAttributeOperations().isEmpty());
		assertNull(tested.getLatestComponentTime());

		assertEquals(3, tested.getVersionFilters().size());
		assertEquals("1.2", tested.getDatabaseSearchParameters().get("version1"));
		assertEquals("10", tested.getDatabaseSearchParameters().get("version2"));
		assertEquals("0.5.7", tested.getDatabaseSearchParameters().get("version3"));

		assertEquals("version != #{filterParams.version1} AND version <= #{filterParams.version2} AND version > #{filterParams.version3}", tested.getDatabaseSearchString());
	}


	@Test
	public void parseFilterStringEmpty() {

		Filter tested = Filter.parseFilterString("");
		assertNull(tested.getUnspecified());
		assertTrue(tested.getGroupFilters().isEmpty());
		assertTrue(tested.getArtifactFilters().isEmpty());
		assertTrue(tested.getVersionFilters().isEmpty());
		assertTrue(tested.getTagOperations().isEmpty());
		assertTrue(tested.getTagAttributeOperations().isEmpty());

		assertNull(tested.getLatestComponentTime());

		assertTrue(StringUtils.isBlank(tested.getDatabaseSearchString()));
		assertTrue(tested.getDatabaseSearchParameters().isEmpty());

		tested = Filter.parseFilterString("", 1729766001L);
		assertNull(tested.getUnspecified());
		assertTrue(tested.getGroupFilters().isEmpty());
		assertTrue(tested.getArtifactFilters().isEmpty());
		assertTrue(tested.getVersionFilters().isEmpty());
		assertTrue(tested.getTagOperations().isEmpty());
		assertTrue(tested.getTagAttributeOperations().isEmpty());
		assertEquals(Long.valueOf(1729766001L), tested.getLatestComponentTime());
		assertEquals("created > #{filterParams.created}", tested.getDatabaseSearchString());
		assertEquals(1L, tested.getDatabaseSearchParameters().size());
	}

	@Test
	public void parseFilterStringAllWithNameAndSpaces() {

		Filter tested = Filter.parseFilterString("group    =     org.jboss&name = nexus&version>=2.3.1&  tag=       TO_DEPLOY& tagAttr= OS <>    MacOS");
		assertNull(tested.getUnspecified());
		assertEquals(1, tested.getGroupFilters().size());
		assertEquals("org.jboss", tested.getGroupFilters().get(0).getValue());
		assertEquals(Filter.LogicalOperation.Operator.EQ, tested.getGroupFilters().get(0).getOperator());

		assertEquals(1, tested.getArtifactFilters().size());
		assertEquals("nexus", tested.getArtifactFilters().get(0).getValue());
		assertEquals(Filter.LogicalOperation.Operator.EQ, tested.getArtifactFilters().get(0).getOperator());

		assertEquals(1, tested.getVersionFilters().size());
		assertEquals("2.3.1", tested.getVersionFilters().get(0).getValue());
		assertEquals(Filter.LogicalOperation.Operator.GE, tested.getVersionFilters().get(0).getOperator());

		assertEquals(1, tested.getTagOperations().size());
		assertEquals("TO_DEPLOY", tested.getTagOperations().get(0).getValue());
		assertEquals(Filter.LogicalOperation.Operator.EQ, tested.getTagOperations().get(0).getOperator());

		List<Filter.LogicalOperation> tagExpressions = tested.getTagAttributeOperations();
		assertEquals(1, tagExpressions.size());
		assertEquals("OS", tagExpressions.get(0).getAttribute());
		assertEquals(Filter.LogicalOperation.Operator.NE, tagExpressions.get(0).getOperator());
		assertEquals("MacOS", tagExpressions.get(0).getValue());
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

	@SuppressWarnings("SpellCheckingInspection")
	@Test(expected = Filter.ParseException.class)
	public void parseFilterStringTypo() {
		try {
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
		assertTrue(tested.getGroupFilters().isEmpty());
		assertTrue(tested.getArtifactFilters().isEmpty());
		assertTrue(tested.getVersionFilters().isEmpty());
		assertTrue(tested.getTagOperations().isEmpty());
		assertTrue(tested.getTagAttributeOperations().isEmpty());
		assertNull(tested.getLatestComponentTime());

		assertEquals("(version = #{filterParams.unspecified} OR name = #{filterParams.unspecified} OR namespace = #{filterParams.unspecified})", tested.getDatabaseSearchString());

		assertEquals(1, tested.getDatabaseSearchParameters().size());
		assertEquals("kie-releases", tested.getDatabaseSearchParameters().get("unspecified"));
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

	//------------ tag tests Database implementation
	@Test
	public void checkComponentTagDatabase() {
		Filter tested = Filter.parseFilterString("tag=DEPLOYED");

		org.jboss.nexus.content.Component component = new TemplateRenderingHelper.FictiveComponent("org.jboss", "nexus", "1.0");
		assertFalse(tested.checkComponentTag(component));
		
		Tag tag = mock(Tag.class);
		when(tag.name()).thenReturn("DEPLOYED");
		component.tags().add(tag);
		
		assertTrue(tested.checkComponentTag(component));

		tested = Filter.parseFilterString("tag!=FAILED");
		assertTrue(tested.checkComponentTag(component));

		tested = Filter.parseFilterString("tag=FAILED");
		assertFalse(tested.checkComponentTag(component));

		tested = Filter.parseFilterString("tag!=DEPLOYED");
		assertFalse(tested.checkComponentTag(component));

		tag = mock(Tag.class);
		when(tag.name()).thenReturn("ANOTHER-TAG");
		component.tags().add(tag);

		tested = Filter.parseFilterString("tag!=ANOTHER-TAG");
		assertFalse(tested.checkComponentTag(component));

		tested = Filter.parseFilterString("tag=ANOTHER-TAG");
		assertTrue(tested.checkComponentTag(component));

		tested = Filter.parseFilterString("tag!=DEPLOYED");
		assertFalse(tested.checkComponentTag(component));

		tested = Filter.parseFilterString("tag=DEPLOYED");
		assertTrue(tested.checkComponentTag(component));

		tested = Filter.parseFilterString("tag=DEPLOYED&tag=ANOTHER-TAG");
		assertTrue(tested.checkComponentTag(component));

		tested = Filter.parseFilterString("tag=DEPLOYED&tag!=ANOTHER-TAG");
		assertFalse(tested.checkComponentTag(component));

		tested = Filter.parseFilterString("tag!=DEPLOYED&tag=ANOTHER-TAG");
		assertFalse(tested.checkComponentTag(component));

		tested = Filter.parseFilterString("tag!=DEPLOYED&tag!=ANOTHER-TAG");
		assertFalse(tested.checkComponentTag(component));

		tested = Filter.parseFilterString("tag=DEPLOYED&tag!=EXCLUDE-TAG");
		assertTrue(tested.checkComponentTag(component));

		tested = Filter.parseFilterString("tag=ANOTHER-TAG&tag!=EXCLUDE-TAG");
		assertTrue(tested.checkComponentTag(component));

		tested = Filter.parseFilterString("tag=ANOTHER-TAG&tag=EXCLUDE-TAG");
		assertFalse(tested.checkComponentTag(component));

		try {
			tested = Filter.parseFilterString("tag>=DEPLOYED");
			tested.checkComponentTag(component);
			fail("Exception should have been raised");
		} catch (RuntimeException e) {
			assertEquals("Unexpected operator for tag.", e.getMessage());
		}

		try {
			tested = Filter.parseFilterString("tag>DEPLOYED");
			tested.checkComponentTag(component);
			fail("Exception should have been raised");
		} catch (RuntimeException e) {
			assertEquals("Unexpected operator for tag.", e.getMessage());
		}

		try {
			tested = Filter.parseFilterString("tag<=DEPLOYED");
			tested.checkComponentTag(component);
			fail("Exception should have been raised");
		} catch (RuntimeException e) {
			assertEquals("Unexpected operator for tag.", e.getMessage());
		}

		try {
			tested = Filter.parseFilterString("tag<DEPLOYED");
			tested.checkComponentTag(component);
			fail("Exception should have been raised");
		} catch (RuntimeException e) {
			assertEquals("Unexpected operator for tag.", e.getMessage());
		}

	}

	@Test
	public void checkComponentTagAttrDatabase() {
		Filter tested = Filter.parseFilterString("tagAttr=OS=macOS");

		org.jboss.nexus.content.Component component = new TemplateRenderingHelper.FictiveComponent("org.jboss", "nexus", "1.0");
		assertFalse("No tag attributes", tested.checkComponentTag(component));

		Tag tag = mock(Tag.class);
		// when(tag.name()).thenReturn("DEPLOYED_EMPTY");
		when(tag.attributes()).thenReturn(new NestedAttributesMap());
		component.tags().add(tag);		
		

		Map<String, Object> attributes = new HashMap<>();
		attributes.put("OS", "macOS");
		attributes.put("build", "23");
		attributes.put("java-vm", "11");

		tag = mock(Tag.class);
		// when(tag.name()).thenReturn("DEPLOYED_MULTIPLE");
		when(tag.attributes()).thenReturn(new NestedAttributesMap("attributes", attributes));
		component.tags().add(tag);

		// ------------

		tested = Filter.parseFilterString("tagAttr=OS=macOS");
		assertTrue("Equals", tested.checkComponentTag(component));

		tested = Filter.parseFilterString("tagAttr=OS<=macOS");
		assertTrue("less or equal", tested.checkComponentTag(component));

		tested = Filter.parseFilterString("tagAttr=OS>=macOS");
		assertTrue("more or equal", tested.checkComponentTag(component));

		tested = Filter.parseFilterString("tagAttr=OS<macOS");
		assertFalse("lesser", tested.checkComponentTag(component));

		tested = Filter.parseFilterString("tagAttr=OS>macOS");
		assertFalse("", tested.checkComponentTag(component));

		tested = Filter.parseFilterString("tagAttr=build>0");
		assertTrue("greater", tested.checkComponentTag(component));

		tested = Filter.parseFilterString("tagAttr=build<25");
		assertTrue("smaller", tested.checkComponentTag(component));

		tested = Filter.parseFilterString("tagAttr=build<25&tagAttr=OS=macOS");
		assertTrue("Multiple attributes matches", tested.checkComponentTag(component));

		tested = Filter.parseFilterString("tagAttr=build<25&tagAttr=OS=Windows");
		assertFalse("Multiple attributes does not match", tested.checkComponentTag(component));
	}
}
