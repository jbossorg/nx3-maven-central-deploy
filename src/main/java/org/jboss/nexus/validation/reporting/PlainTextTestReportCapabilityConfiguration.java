package org.jboss.nexus.validation.reporting;

import java.util.Map;

public class PlainTextTestReportCapabilityConfiguration extends TestReportCapabilityConfigurationParent {
	public static final String FILE_NAME = "file.name";

	public static final String APPEND_FILE = "append.file";

	private final String outputFileName;

	private final boolean appendFile;

	public PlainTextTestReportCapabilityConfiguration(Map<String, String> properties) {
		outputFileName = properties.get(FILE_NAME);
		appendFile = Boolean.parseBoolean(properties.getOrDefault(APPEND_FILE, "true"));
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
}
