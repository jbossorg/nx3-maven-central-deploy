package org.jboss.nexus.validation.reporting;

import java.util.Map;

public class PlainTextTestReportCapabilityConfiguration extends TestReportCapabilityConfigurationParent {
	public static final String FILE_NAME = "file.name";

	public static final String APPEND_FILE = "append.file";

	public static final String REPORT_TEMPLATE = "report.template";

	private final String outputFileName;

	private final boolean appendFile;

	private final String reportTemplate;

	static final String DEFAULT_TEMPLATE =
		 "------------------------------------------------------------\n" +
			  "${name} deployment number ${run} on ${date}\n" +
			  "Processed ${processed} components from ${repository}.\n" +
			  "Problems found: ${errors.size()}\n" +
			  "#set ($previous=\"\")\n" +
			  "#foreach( $error in $errors )\n" +
			  "#set($component=$error.component.toStringExternal())\n" +
			  "#if($component != $previous)\n" +
			  "#set($previous=$component)\n" +
			  "----------------------------\n" +
			  "$component\n" +
			  "#end\n" +
			  "- $error.getProblem()\n" +
			  "#end";

	public PlainTextTestReportCapabilityConfiguration(Map<String, String> properties) {
		outputFileName = properties.get(FILE_NAME);
		appendFile = Boolean.parseBoolean(properties.getOrDefault(APPEND_FILE, "true"));
		reportTemplate = properties.getOrDefault(REPORT_TEMPLATE, DEFAULT_TEMPLATE);
	}

	/** Output file name. It may contain variables as it will be processed through Velocity.
	 *
	 * @return file name
	 */
	public String getOutputFileName() {
		return outputFileName;
	}

	/** If true, the text should be appended to the target file.
	 *
	 * @return if false, the output file will be overwritten
	 */
	public boolean isAppendFile() {
		return appendFile;
	}

	public String getReportTemplate() {
		return reportTemplate;
	}
}
