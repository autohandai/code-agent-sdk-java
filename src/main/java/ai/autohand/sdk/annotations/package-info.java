/**
 * Annotation-based API for declarative agent configuration.
 *
 * <p>Provides annotations that can be applied to classes to configure an
 * {@link ai.autohand.sdk.sdk.Agent} without imperative code. The
 * {@link ai.autohand.sdk.annotations.AnnotatedAgentFactory} scans these
 * annotations and wires everything together.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * @AutohandAgent(model = "fantail2", instructions = "Be concise")
 * @Skills({"typescript", "testing"})
 * @Permission(PermissionMode.INTERACTIVE)
 * @EnableTools({Tool.READ_FILE, Tool.RUN_COMMAND})
 * public class MyAgent {}
 *
 * Agent agent = AnnotatedAgentFactory.create(MyAgent.class);
 * }</pre>
 *
 * @see ai.autohand.sdk.annotations.AnnotatedAgentFactory
 * @see ai.autohand.sdk.annotations.AutohandAgent
 */
package ai.autohand.sdk.annotations;
