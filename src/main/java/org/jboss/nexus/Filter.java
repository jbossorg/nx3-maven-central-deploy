package org.jboss.nexus;

import com.sonatype.nexus.tags.orient.OrientTag;
import com.sonatype.nexus.tags.orient.TagComponent;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sonatype.nexus.repository.storage.Component;

import java.util.Objects;


public class Filter {

	private String group, artifact, version, tag, tagAttr, tagAttrValue;

	private LogicalOperation.Operator versionOperation, tagOperation, tagAttrOperation;

	private String unspecified;

	public static Filter parseFilterString(@Nullable String filterString) throws ParseException {
		Filter result = new Filter();

		if(StringUtils.isNotBlank(filterString)) {
			for(String token : filterString.split("\\s*&\\s*")) {
				LogicalOperation operation = LogicalOperation.parseString(token);
				switch (operation.getAttribute().toLowerCase()) {
					case "group":
						if (operation.getOperator() != LogicalOperation.Operator.EQ) {
							throw new ParseException("Incorrect filter expression: The only allowed logical operator for checking group in the filter field is '='");
						}
						result.group = operation.getValue();
						break;
					case "name":
					case "artifact":
						if (operation.getOperator() != LogicalOperation.Operator.EQ) {
							throw new ParseException("Incorrect filter expression: The only allowed logical operator for checking artifact in the filter field is '='");
						}
						result.artifact = operation.getValue();
						break;
					case "version":
						result.version = operation.getValue();
						result.versionOperation = operation.getOperator();
						break;
					case "tag":
						result.tag = operation.getValue();
						result.tagOperation = operation.getOperator();
						break;
					case "":
						if(result.unspecified != null) {
							throw new ParseException("Incorrect filter expression: free text expression can only be used once: "+filterString);
						}
						result.unspecified = operation.getValue();
						break;
					default:
						if(token.toLowerCase().matches("\\s*tagattr\\s*=.+")) {
							operation = LogicalOperation.parseString(token.substring(token.indexOf('=')+1));

							if (operation.getOperator() == LogicalOperation.Operator.NOOP) {
								throw new ParseException("Incorrect filter expression: Missing operator in tagAttr: " + token);
							}
							result.tagAttrOperation = operation.getOperator();
							result.tagAttr = operation.getAttribute();
							result.tagAttrValue = operation.getValue();
							break;
						} else
							throw new ParseException("Incorrect filter expression: allowed fields are group, artifact, name, version, tag, tagAttr: "+token); // FIXME: 02.02.2023 maybe not the right text
				}
			}
		}

		return result;
	}

	static class LogicalOperation {
		private LogicalOperation() {}

		enum Operator {EQ, NE, LT, GT, LE, GE, NOOP}

		String attribute;
		String value;
		Operator operator;

		/** Parses the string, that looks something like xy=zz. The operator gets identified. Attribute and value are stored in the fields.
		 * 
		 * @param attrExpression expression to be parsed
		 *
		 * @throws RuntimeException when there is an error in the string
		 */
		public static LogicalOperation parseString(@NotNull  String attrExpression) {
			if(StringUtils.isBlank(attrExpression))
				throw new Filter.ParseException("Empty expression defined in a filter");

			LogicalOperation result = new LogicalOperation();

			int index = Math.max(attrExpression.indexOf("!="), attrExpression.indexOf("<>"));
			if(index > 1) {
				// not equal
				result.attribute = attrExpression.substring(0, index).trim();
				result.value = attrExpression.substring(index+2).trim();
				result.operator = Operator.NE;
			} else if ((index = attrExpression.indexOf("<=")) > -1) {
				result.attribute = attrExpression.substring(0, index).trim();
				result.value = attrExpression.substring(index+2).trim();
				result.operator = Operator.LE;
			} else if ((index = attrExpression.indexOf(">=")) > -1) {
				result.attribute = attrExpression.substring(0, index).trim();
				result.value = attrExpression.substring(index+2).trim();
				result.operator = Operator.GE;
			} else if ((index = attrExpression.indexOf('<')) > -1) {
				// smaller than
				result.attribute = attrExpression.substring(0, index).trim();
				result.value = attrExpression.substring(index+1).trim();
				result.operator = Operator.LT;
			} else if ((index = attrExpression.indexOf('>')) > -1) {
				// bigger than
				result.attribute = attrExpression.substring(0, index).trim();
				result.value = attrExpression.substring(index+1).trim();
				result.operator = Operator.GT;
			} else if ((index = attrExpression.indexOf('=')) > -1) {
				// equals
				result.attribute = attrExpression.substring(0, index).trim();
				result.value = attrExpression.substring(index+1).trim();
				result.operator = Operator.EQ;
			} else {
				// failed to locate any logical operation
				result.attribute = "";
				result.value = attrExpression;
				result.operator = Operator.NOOP;
			}

			if(StringUtils.isBlank(result.attribute) && result.getOperator() != Operator.NOOP )
				throw new Filter.ParseException("Attribute name was missing in the logical expression in filter: "+attrExpression);

			if(StringUtils.isBlank(result.value))
				throw new Filter.ParseException("Missing value in the logical expression in filter: "+attrExpression);

			return result;
		}

		/** Attribute name
		 *
		 * @return attribute name
		 */
		@NotNull
		public String getAttribute() {
			return attribute;
		}

		@NotNull
		public String getValue() {
			return value;
		}

		@NotNull
		public Operator getOperator() {
			return operator;
		}

	}

	/**
	 * Exception thrown during parsing the new object.
	 */
	public static class ParseException extends RuntimeException {
		ParseException(String message) {
			super(message);
		}
	}

	/** Returns group of the artifact to be searched for.
	 *
	 * @return group
	 */
	public String getGroup() {
		return group;
	}

	/** Returns artifact name to be searched for.
	 *
	 * @return artifact name
	 */	public String getArtifact() {
		return artifact;
	}

	/** Returns version of the artifact to be searched for.
	 *
	 * @return version
	 */	public String getVersion() {
		return version;
	}

	/** Returns tag name of the artifact to be searched for.
	 *
	 * @return tag name
	 */	public String getTag() {
		return tag;
	}


	/** Returns tag attribute of the artifact to be searched for.
	 *
	 * @return tag attribute
	 */	public String getTagAttr() {
		return tagAttr;
	}

	/** Returns value of the tag attribute to be checked.
	 *
	 * @return tag attribute value
	 */
	public String getTagAttrValue() {
		return tagAttrValue;
	}


	/** Returns value for search without logical operators.
	 *
	 * @return value for unspecified search
	 */
	public String getUnspecified() {
		return unspecified;
	}

	/** Logical operation to be used for checking on version.
	 *
	 * @return {@link LogicalOperation.Operator#operator} value
	 */
	public LogicalOperation.Operator getVersionOperation() {
		return versionOperation;
	}

	/** Logical operation to be used for checking on tag name.
	 *
	 * @return {@link LogicalOperation.Operator#operator} value
	 */
	public LogicalOperation.Operator getTagOperation() {
		return tagOperation;
	}

	/** Logical operation to be used for checking on tag attribute.
	 *
	 * @return {@link LogicalOperation.Operator#operator} value
	 */
	public LogicalOperation.Operator getTagAttrOperation() {
		return tagAttrOperation;
	}

	/** Returns string, that will be searched for by Nexus based on the filter setting to narrow the selection of artifacts.
	 *
	 * @return null or string, that will be matched against group OR artifact OR version during repository crawling
	 */
	public String getSearchString() {
		if(StringUtils.isNotBlank(getUnspecified()))
			return getUnspecified();

		if(StringUtils.isNotBlank(getArtifact()))
			return getArtifact();

		if(StringUtils.isNotBlank(getVersion()) && getVersionOperation() == LogicalOperation.Operator.EQ )
			return getVersion();

		if(StringUtils.isNotBlank(getGroup()))
			return getGroup();

		return null;
	}

	/** Verifies, that the current component matches the filter conditions.
	 *
	 * @param component component to be checked
	 *
	 * @return true if the component corresponds to the search conditions
	 */
	public boolean checkComponent(Component component) {
		if(StringUtils.isNotEmpty(getGroup()))
			if(!Objects.equals(getGroup(), component.group()))
				return false;

		if(StringUtils.isNotEmpty(getArtifact()))
			if(!Objects.equals(getArtifact(), component.name()))
				return false;

		if(StringUtils.isNotEmpty(getVersion()))
			if(!FunctionMapping.resolve(getVersionOperation(), component.version(), getVersion()))
				return false;

		// NXRM3 Professional tagging
		if(TagComponent.class.isAssignableFrom(component.getClass())) {

			if (StringUtils.isNotEmpty(getTag()))
				if(((TagComponent)component).tags().stream().noneMatch( t -> FunctionMapping.resolve(getTagOperation(), t.name(), getTag())))
					return false;

			if (StringUtils.isNotEmpty(getTagAttr())) {
				boolean found = false;

				for(OrientTag tag : ((TagComponent)component).tags()) {
					Object object = tag.attributes().get(getTagAttr());
					if (object != null &&  String.class.isAssignableFrom(object.getClass())) {
						found = FunctionMapping.resolve(getTagAttrOperation(), (String) object, getTagAttrValue());
						if(found)
							break;
					}
				}

				return found;
			}

		} else {
			// requested filter on tags while tags are not supported
			if(StringUtils.isNotEmpty(getTag()) || StringUtils.isNotEmpty(getTagAttr())) {
				throw new RuntimeException("Filter error: attempt to filter based on a tag or tagAttr. Tags are only supported in NXRM3 Professional.");
			}
		}
		return true;
	}


	private static class FunctionMapping {
		static boolean resolve(LogicalOperation.Operator operator, String s1, String s2) {
			switch (operator) {
				case EQ:
					return EQ(s1, s2);
				case NE:
					return NE(s1, s2);
				case LT:
					return LT(s1, s2);
				case GT:
					return GT(s1, s2);
				case GE:
					return GE(s1, s2);
				case LE:
					return LE(s1, s2);
				default:
					throw new RuntimeException("Filtering: unknown operation - error in programing!");
			}
		}

		private static boolean EQ(String s1, String s2) {
			return Objects.equals(s1, s2);
		}

		private static boolean NE(String s1, String s2) {
			return !EQ(s1, s2);
		}

		private static boolean LT(String s1, String s2) {
			return s1.compareTo(s2) < 0;
		}

		private static boolean GT(String s1, String s2) {
			return s1.compareTo(s2) > 0;
		}

		private static boolean LE(String s1, String s2) {
			return s1.compareTo(s2) <= 0;
		}

		private static boolean GE(String s1, String s2) {
			return s1.compareTo(s2) >= 0;
		}
	}





}
