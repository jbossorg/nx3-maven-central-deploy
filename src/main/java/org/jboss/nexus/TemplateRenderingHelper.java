package org.jboss.nexus;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.jboss.nexus.validation.checks.FailedCheck;
import org.jetbrains.annotations.NotNull;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.DefaultComponent;

import javax.inject.Named;
import java.io.*;
import java.util.*;

/** Class to generate template parameters for Velocity rendering
 *
 */
@Named
public class TemplateRenderingHelper {

	/** Name of the datetime field */
	public static final String DATE = "date";

	/** Name of the run number field */
	public static final String RUN = "run";

	/** Name for the name of the task */
	public static final String TASK_NAME = "name";

//	private final VelocityEngineProvider velocityEngineProvider;

//	@Inject
//	public TemplateRenderingHelper(VelocityEngineProvider velocityEngineProvider) {
//		this.velocityEngineProvider = checkNotNull(velocityEngineProvider);
//	}

	public Map<String, Object>  generateTemplateParameters(@NotNull MavenCentralDeployTaskConfiguration mavenCentralDeployTaskConfiguration) {
		Map<String, Object> result = new HashMap<>();
		result.put(DATE, new Date());
		result.put(RUN, mavenCentralDeployTaskConfiguration.getRunNumber());
		result.put(TASK_NAME, mavenCentralDeployTaskConfiguration.getName());

		final String variablesString;
		if(StringUtils.isNotBlank(variablesString = mavenCentralDeployTaskConfiguration.getVariables())) {
			Properties properties = new Properties();
			try (StringReader reader = new StringReader(variablesString)) {
				properties.load(reader);
				properties.forEach((key, value) -> result.put((String)key, value));
			} catch (IOException e) {
				throw new RuntimeException("Error parsing variables from the task configuration", e);
			}
		}
		return result;
	}

	/** Method renders template using Velocity into a string.
	 *
	 * @param template the template string to be rendered.
	 * @param parameters parameters to be passed for the replacements
	 *
	 * @return processed result
	 */
	public String render(String template, Map<String, Object> parameters) {
		try (StringWriter stringWriter = new StringWriter()) {
			try(StringReader stringReader = new StringReader(template)) {
				render(stringReader, stringWriter, parameters);
				return stringWriter.toString();
			}
		} catch (IOException e) {
			throw new RuntimeException("Error rendering template: "+e.getMessage(), e);
		}
	}

	/** Rendering streams using Velocity. Use for large output, where strings are not efficient
	 *
	 * @param templateStream the stream with a template.
	 * @param outputStream output stream
	 *
	 * @param parameters parameters to be passed to the processing
	 */
	public void render(Reader templateStream, Writer outputStream, Map<String, Object> parameters) {
		final VelocityEngine velocityEngine =  new VelocityEngine(); //   checkNotNull(velocityEngineProvider.get(), "Unable to get velocity engine!");
		VelocityContext velocityContext = new VelocityContext(parameters);
		try {
			velocityEngine.evaluate(velocityContext, outputStream, "render_template", templateStream);
		} catch (Exception e) {
			throw new RuntimeException("Error rendering template!", e);
		}
	}

	private static final List<FailedCheck> failedChecks ;
	static {
		Component component1 = new DefaultComponent().group("org.jboss.failed").name("failed-1").version("1.2.3").format("maven2");
		Component component2 = new DefaultComponent().group("org.jboss.failed").name("failed-2").version("1.2.3").format("maven2");
		Component component3 = new DefaultComponent().group("org.something.failed").name("another").version("3.2.1").format("maven2");

		List<FailedCheck> list = new ArrayList<>();

		list.add(new FailedCheck(component1, "org/jboss/failed/failed-1/1.2.3/failed-1-1.2.3.pom parsing error: ParseError at [row,col]:[1,104]" +
			 "Message: XML document structures must start and end within the same entity." ));

		list.add(new FailedCheck(component1, "org/jboss/failed/failed-1/1.2.3/failed-1-1.2.3.pom does not have the project group specified!" ));

		list.add(new FailedCheck(component2, "org/jboss/failed/failed-2/1.2.3/failed-1-1.2.3.pom does not have the project group specified!" ));

		list.add(new FailedCheck(component3, "org/something/failed/another/3.2.1/another-3.2.1.pom does not have the project name specified!" ));
		list.add(new FailedCheck(component3, "org/something/failed/another/3.2.1/another-3.2.1.pom does not have the project description specified!" ));
		list.add(new FailedCheck(component3, "org/something/failed/another/3.2.1/another-3.2.1.pom does not have the project URL specified!" ));

		failedChecks = Collections.unmodifiableList(list);
	}


	/** Generates a list of fictive test error failures so it is possible to test the templates.
	 *
	 * @return fictive report
	 */
	public static List<FailedCheck> generateFictiveErrors() {
		return failedChecks;
	}
}
