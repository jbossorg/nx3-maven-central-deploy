package org.jboss.nexus;

import com.sonatype.nexus.tags.orient.OrientTag;
import com.sonatype.nexus.tags.orient.TagComponent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.storage.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class FilterTest {

	@Mock
	private Component storageComponent;

	@Mock
	private TagComponent tagComponent;


	@Test
	public void parseFilterString() {
		// TODO: 01.02.2023 all the testing
	}




	@Test
	public void checkComponentNull() {
		Filter tested = Filter.parseFilterString(null);
		assertTrue(tested.checkComponent(storageComponent));
	}


	@Test
	public void checkComponentGAV() {
		Filter tested = Filter.parseFilterString("group=org.jboss&artifact=nexus&version=2.3.1");
		when(storageComponent.group()).thenReturn("org.jboss");
		when(storageComponent.name()).thenReturn("nexus");
		when(storageComponent.version()).thenReturn("2.3.1");
		assertTrue(tested.checkComponent(storageComponent));


		tested = Filter.parseFilterString("artifact=nexus&version=2.3.1");
		assertTrue(tested.checkComponent(storageComponent));

		tested = Filter.parseFilterString("group=org.jboss&version=2.3.1");
		assertTrue(tested.checkComponent(storageComponent));

		tested = Filter.parseFilterString("group=org.jboss&artifact=nexus");
		assertTrue(tested.checkComponent(storageComponent));

		tested = Filter.parseFilterString("group=com.redhat&artifact=nexus&version=2.3.1");
		assertFalse(tested.checkComponent(storageComponent));

		tested = Filter.parseFilterString("group=com.redhat&artifact=nexus&version=2.3.1");
		assertFalse(tested.checkComponent(storageComponent));

		tested = Filter.parseFilterString("group=org.jboss&artifact=eap&version=2.3.1");
		assertFalse(tested.checkComponent(storageComponent));

		tested = Filter.parseFilterString("group=org.jboss&artifact=nexus&version=2.3.2");
		assertFalse(tested.checkComponent(storageComponent));

		tested = Filter.parseFilterString("group=org.jboss&artifact=nexus&version>=2.3.2");
		assertFalse(tested.checkComponent(storageComponent));

		tested = Filter.parseFilterString("group=org.jboss&artifact=nexus&version>=2.3.0");
		assertTrue(tested.checkComponent(storageComponent));

		tested = Filter.parseFilterString("group=org.jboss&artifact=nexus&version>=2.3.1");
		assertTrue(tested.checkComponent(storageComponent));

		tested = Filter.parseFilterString("group=org.jboss&artifact=nexus&version<=2.3.2");
		assertTrue(tested.checkComponent(storageComponent));

		tested = Filter.parseFilterString("group=org.jboss&artifact=nexus&version<=2.3.0");
		assertFalse(tested.checkComponent(storageComponent));

		tested = Filter.parseFilterString("group=org.jboss&artifact=nexus&version<=2.3.1");
		assertTrue(tested.checkComponent(storageComponent));

		tested = Filter.parseFilterString("group=org.jboss&artifact=nexus&version!=2.3.1");
		assertFalse(tested.checkComponent(storageComponent));

		tested = Filter.parseFilterString("group=org.jboss&artifact=nexus&version <> 2.3.1");
		assertFalse(tested.checkComponent(storageComponent));
	}

	@Test(expected = RuntimeException.class)
	public void checkComponentTagsUnsupported() {
		Filter tested = Filter.parseFilterString("tag=DEPLOYED");
		try {
			tested.checkComponent(storageComponent); // mocked class not compatible with TagComponent
		} catch (RuntimeException e) {
			assertEquals("Filter error: attempt to filter based on a tag or tagAttr. Tags are only supported in NXRM3 Professional.", e.getMessage());
			throw e;
		}
	}

	@Test(expected = RuntimeException.class)
	public void checkComponentTagAttrsUnsupported() {
		Filter tested = Filter.parseFilterString("tagAttr=OS=macOS");
		try {
			tested.checkComponent(storageComponent); // mocked class not compatible with TagComponent
		} catch (RuntimeException e) {
			assertEquals("Filter error: attempt to filter based on a tag or tagAttr. Tags are only supported in NXRM3 Professional.", e.getMessage());
			throw e;
		}
	}

	@Test
	public void checkComponentTags() {
		Filter tested = Filter.parseFilterString("tag=DEPLOYED");
		assertFalse(tested.checkComponent(tagComponent));

		HashSet<OrientTag> tags = new HashSet<>();
		OrientTag tag = new OrientTag();
		tag.name("DEPLOYED");
		tags.add(tag);

		when(tagComponent.tags()).thenReturn(tags);
		assertTrue(tested.checkComponent(tagComponent));

		tested = Filter.parseFilterString("tag>=DEPLOYED");
		assertTrue(tested.checkComponent(tagComponent));

		tested = Filter.parseFilterString("tag>DEPLOYED");
		assertFalse(tested.checkComponent(tagComponent));

		tested = Filter.parseFilterString("tag>=Z");
		assertFalse(tested.checkComponent(tagComponent));

		tested = Filter.parseFilterString("tag>=A");
		assertTrue(tested.checkComponent(tagComponent));
	}

	@Test
	public void checkComponentTagAttr() {
		Filter tested = Filter.parseFilterString("tagAttr=OS=macOS");
		assertFalse("No tag attributes", tested.checkComponent(tagComponent));

		HashSet<OrientTag> tags = new HashSet<>();

		OrientTag tag = new OrientTag();
		tag.name("DEPLOYED_EMPTY").attributes(new NestedAttributesMap());
		tags.add(tag);

		Map<String, Object> attributes = new HashMap<>();
		attributes.put("OS", "macOS");
		attributes.put("build", "23");
		attributes.put("java-vm", "11");

		tag = new OrientTag();
		tag.name("DEPLOYED_MULTIPLE").attributes(new NestedAttributesMap("attributes", attributes));
		tags.add(tag);

		when(tagComponent.tags()).thenReturn(tags);

		// ------------

		tested = Filter.parseFilterString("tagAttr=OS=macOS");
		assertTrue("Equals", tested.checkComponent(tagComponent));

		tested = Filter.parseFilterString("tagAttr=OS<=macOS");
		assertTrue("less or equal", tested.checkComponent(tagComponent));

		tested = Filter.parseFilterString("tagAttr=OS>=macOS");
		assertTrue("more or equal", tested.checkComponent(tagComponent));

		tested = Filter.parseFilterString("tagAttr=OS<macOS");
		assertFalse("lesser", tested.checkComponent(tagComponent));

		tested = Filter.parseFilterString("tagAttr=OS>macOS");
		assertFalse("", tested.checkComponent(tagComponent));


		tested = Filter.parseFilterString("tagAttr=build>0");
		assertTrue("greater", tested.checkComponent(tagComponent));

		tested = Filter.parseFilterString("tagAttr=build<25");
		assertTrue("smaller", tested.checkComponent(tagComponent));

		tested = Filter.parseFilterString("tagAttr=build<25&tagAttr=OS=macOS");
		assertTrue("Multiple attributes matches", tested.checkComponent(tagComponent));

		tested = Filter.parseFilterString("tagAttr=build<25&tagAttr=OS=Windows");
		assertFalse("Multiple attributes does not match", tested.checkComponent(tagComponent));
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


		List<Filter.TagAttributeExpression> tagExpressions = tested.getTagAttributeOperations();
		assertEquals(1, tagExpressions.size());
		assertEquals("OS", tagExpressions.get(0).getTagAttr());
		assertEquals(Filter.LogicalOperation.Operator.NE, tagExpressions.get(0).getTagAttrOperation());
		assertEquals("MacOS", tagExpressions.get(0).getTagAttrValue());
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

		List<Filter.TagAttributeExpression> tagExpressions = tested.getTagAttributeOperations();
		assertEquals(1, tagExpressions.size());
		assertEquals("OS", tagExpressions.get(0).getTagAttr());
		assertEquals(Filter.LogicalOperation.Operator.NE, tagExpressions.get(0).getTagAttrOperation());
		assertEquals("MacOS", tagExpressions.get(0).getTagAttrValue());
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
		assertTrue(tested.getTagAttributeOperations().isEmpty());
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
