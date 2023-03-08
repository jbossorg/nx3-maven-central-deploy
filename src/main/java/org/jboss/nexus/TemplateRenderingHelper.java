package org.jboss.nexus;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.jetbrains.annotations.NotNull;

import javax.inject.Named;
import java.io.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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
	public static final String TASK_NAME = "task";

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

}
