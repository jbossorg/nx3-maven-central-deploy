package org.jboss.nexus;

import com.sonatype.nexus.tags.Tag;
import org.apache.commons.lang3.StringUtils;

import org.jboss.nexus.content.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;


public class Filter {

	private Long latestComponentTime;

	private final List<LogicalOperation> groupFilters = new ArrayList<>(),
			artifactFilters = new ArrayList<>(),
			versionFilters = new ArrayList<>();

	private final List<LogicalOperation> tagAttributeOperations = new ArrayList<>();

	private final List<LogicalOperation> tagOperations = new ArrayList<>();

	private String unspecified;

	public static Filter parseFilterString(@Nullable String filterString) throws ParseException {
		return parseFilterString(filterString, null);
	}

	/** Parses the filter expression string and prepares the new filter object for use to filter objects.
	 *
	 * @param filterString the filter string
	 * @param latestDeploymentComponentTime null or the
	 * @return configured filter object
	 * @throws ParseException if a parsing error appears
	 */
	public static Filter parseFilterString(@Nullable String filterString, @Nullable Long latestDeploymentComponentTime) throws ParseException {
		Filter result = new Filter();

		if(latestDeploymentComponentTime != null && latestDeploymentComponentTime > 0) {
			result.latestComponentTime = latestDeploymentComponentTime;
		}

		if(StringUtils.isNotBlank(filterString)) {
			for(String token : filterString.split("\\s*&\\s*")) {
				LogicalOperation operation = LogicalOperation.parseString(token);
				switch (operation.getAttribute().toLowerCase()) {
					case "group":
						if (operation.getOperator() != LogicalOperation.Operator.EQ && operation.getOperator() != LogicalOperation.Operator.NE) {
							throw new ParseException("Incorrect filter expression: The only allowed logical operator for checking group in the filter field is '=' or '!='");
						}
						result.groupFilters.add(operation);
						break;
					case "name":
					case "artifact":
						if (operation.getOperator() != LogicalOperation.Operator.EQ && operation.getOperator() != LogicalOperation.Operator.NE) {
							throw new ParseException("Incorrect filter expression: The only allowed logical operator for checking artifact in the filter field is '=' or '!='");
						}
						result.artifactFilters.add(operation);
						break;
					case "version":
						result.versionFilters.add(operation);
						break;
					case "tag":
						result.tagOperations.add(operation );
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
							result.tagAttributeOperations.add(operation);
							break;
						} else
							throw new ParseException("Incorrect filter expression: allowed fields are group, artifact, name, version, tag, tagAttr: "+token);
				}
			}
		}

		return result;
	}

	static class LogicalOperation {
		private LogicalOperation() {}

		enum Operator {
			EQ("="),
			NE("!="),
			LT("<"),
			GT(">"),
			LE("<="),
			GE(">="),
			NOOP("");

			Operator(String sqlOperator) {
				this.sqlOperator = sqlOperator;
			}
			private final String sqlOperator;

			public String getSqlOperator() {
				return sqlOperator;
			}
		}

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

	/** Returns group filters of the artifact to be searched for.
	 *
	 * @return group operations
	 */
	 List<LogicalOperation> getGroupFilters() {
		return groupFilters;
	}

	/** Returns artifact name to be searched for.
	 *
	 * @return artifact name
	 */
	 List<LogicalOperation> getArtifactFilters() {
		return artifactFilters;
	}

	/** Returns version filters of the artifacts to be searched for.
	 *
	 * @return version
	 */
	 List<LogicalOperation> getVersionFilters() {
		return versionFilters;
	}


	/** Returns value for search without logical operators.
	 *
	 * @return value for unspecified search
	 */
	public String getUnspecified() {
		return unspecified;
	}

	/** Returns possible tag checks defined by parser
	 *
	 * @return list of tag operations
	 */
	List<LogicalOperation> getTagOperations() {
		 return tagOperations;
	}

	/** Returns possible tag attribute expressions
	 *
	 * @return list of attribute expressions to be validated
	 */
	 List<LogicalOperation> getTagAttributeOperations() {
		return tagAttributeOperations;
	}


	/** Returns the timestamp of latest component, that was successfully deployed to Maven Central.
	 *
	 * @return epoch time in seconds
	 */
	public Long getLatestComponentTime() {
		return latestComponentTime;
	}

	/** Search string when <br>
	 * <i>nexus.datastore.enabled=true</i>
	 *
	 * @return search string for {@link org.sonatype.nexus.repository.content.fluent.FluentComponents#byFilter(String, Map)} or an empty string
	 */
	public String getDatabaseSearchString() {
		StringBuilder result = new StringBuilder();

		int counter = 1;
		for(LogicalOperation operation : getGroupFilters()) {
			result
				.append(" AND namespace ")
			  	.append(operation.getOperator().getSqlOperator())
				.append(" #{filterParams.groupId")
				.append(counter++)
				.append('}');

		}

//		if (!getGroupFilters().isEmpty()) {
//			result.append(" AND namespace = #{filterParams.groupId}");
//		}


		counter = 1;
		for(LogicalOperation operation : getArtifactFilters()) {
			result
					.append(" AND name ")
					.append(operation.getOperator().getSqlOperator())
					.append(" #{filterParams.name")
					.append(counter++)
					.append('}');

		}

		counter = 1;
		for(LogicalOperation operation : getVersionFilters()) {
			result
					.append(" AND version ")
					.append(operation.getOperator().getSqlOperator())
					.append(" #{filterParams.version")
					.append(counter++)
					.append('}');

		}

		if(getLatestComponentTime() != null) {
			result.append(" AND created > #{filterParams.created}");
		}

		if(StringUtils.isNotBlank(getUnspecified())) {
			result.append(" AND (version = #{filterParams.unspecified} OR name = #{filterParams.unspecified} OR namespace = #{filterParams.unspecified})");
		}

		if(result.length() > 4)
			return result.substring(5); // cut leading " AND "

		return ""; // no GAV filtering
	}


	/** <p>Search parameters when <br>
	 * <i>nexus.datastore.enabled=true</i></p>
	 *
	 *
	 * @return search parameters for {@link org.sonatype.nexus.repository.content.fluent.FluentComponents#byFilter(String, Map)}
	 */
	@NotNull
	public Map<String, Object> getDatabaseSearchParameters() {
		final HashMap<String, Object>  result = new HashMap<>();
		int counter = 1;
		for(LogicalOperation operation : getGroupFilters()) {
			result.put("groupId"+counter++ , operation.getValue());
		}

		counter = 1;
		for(LogicalOperation operation :getArtifactFilters()) {
			result.put("name"+counter++, operation.getValue());
		}

		counter = 1;
		for(LogicalOperation operation : getVersionFilters()) {
			result.put("version"+counter++, operation.getValue());
		}

		if (StringUtils.isNotBlank(getUnspecified()))
			result.put("unspecified", getUnspecified());

		if(getLatestComponentTime() != null) {
			result.put("created", OffsetDateTime.ofInstant(Instant.ofEpochSecond(getLatestComponentTime()), ZoneId.systemDefault()));
		}

		return Collections.unmodifiableMap(result);
	}

	/** Checks, if the component corresponds to the
	 *
	 * @param component component to be investigated
	 *
	 * @return true if the component tags correspond to the filter requirements
	 */
	public boolean checkComponentTag(Component component) {

		// EQ - at least one tag must fulfill
		// NE - no tag is allowed to fulfill
        for (LogicalOperation filterExpression : getTagOperations()) {
            switch (filterExpression.getOperator()) {
                case EQ:
                    if (component.tags().stream().noneMatch(t -> FunctionMapping.resolve(filterExpression.getOperator(), t.name(), filterExpression.getValue())))
                        return false;
					break;
                case NE:
                    if (!component.tags().stream().allMatch(t -> FunctionMapping.resolve(filterExpression.getOperator(), t.name(), filterExpression.getValue())))
                        return false;
					break;
                default:
                    throw new RuntimeException("Unexpected operator for tag.");
            }
        }


		if (!tagAttributeOperations.isEmpty() ) {
			for (LogicalOperation tagAttributeExpression : tagAttributeOperations) {
				boolean found = false;
				for (Tag tag : component.tags()) {
					Object object = tag.attributes().get(tagAttributeExpression.getAttribute());
					if (object != null && String.class.isAssignableFrom(object.getClass())) {
						found = FunctionMapping.resolve(tagAttributeExpression.getOperator(), (String) object, tagAttributeExpression.getValue());
						if (found)
							break;
					}
				}
				if (!found)
					return false;
			}
		}

		return true;
	}

	private static class FunctionMapping {
		static boolean resolve(LogicalOperation.Operator operator, String s1, String s2) {
            return switch (operator) {
                case EQ -> EQ(s1, s2);
                case NE -> NE(s1, s2);
                case LT -> LT(s1, s2);
                case GT -> GT(s1, s2);
                case GE -> GE(s1, s2);
                case LE -> LE(s1, s2);
                default -> throw new RuntimeException("Filtering: unknown operation - error in programing!");
            };
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
