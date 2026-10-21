package ai.autohand.sdk;

import ai.autohand.sdk.types.SDKConfig;
import ai.autohand.sdk.types.FeatureFlagSettings;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RuntimeSettingsTest {
 private static final ObjectMapper JSON = new ObjectMapper();
 @Test void explicitCredentialsReachNativeFieldsBeforeSpawn() throws Exception {
  for (String provider : new String[]{"anthropic","openai","xai","azure","bedrock","vertexai","custom:my.provider","extension:team.provider:model"}) {
   var config=SDKConfig.builder().provider(provider).apiKey("secret").baseUrl("https://fixture.test").build();
   var settings=JSON.readTree(config.environment().get("AUTOHAND_RUN_SETTINGS"));
   var section=provider.startsWith("custom:")?settings.path("customProviders").path("my.provider"):
    provider.startsWith("extension:")?settings.path("extensionProviders").path(provider):settings.path(provider);
   assertEquals("secret",section.path(provider.equals("vertexai")?"authToken":"apiKey").asText());
   assertEquals(provider.equals("vertexai")?"fixture.test":"https://fixture.test",section.path(provider.equals("vertexai")||provider.equals("bedrock")?"endpoint":"baseUrl").asText());
   assertTrue(config.cliArgs().contains("--sdk-settings-version"));
   assertFalse(config.cliArgs().toString().contains("secret"));
   if(provider.equals("openai")||provider.equals("xai")) assertEquals("api-key",section.path("authMode").asText());
   if(provider.equals("azure")) assertEquals("api-key",section.path("authMethod").asText());
   if(provider.equals("bedrock")) assertEquals("bedrock-api-key",section.path("authMode").asText());
  }
 }
 @Test void preservesFalseNullArraysAndOmittedDefaults() throws Exception {
  var original=JSON.readTree("{\"features\":{\"slashGoal\":true},\"customProviders\":{\"fixture\":{\"models\":[null,\"one\"]}}}");
  var config=SDKConfig.builder().provider("autohandai").runSettings(original).features(FeatureFlagSettings.builder().slashGoal(false).build()).build();
  var settings=JSON.readTree(config.environment().get("AUTOHAND_RUN_SETTINGS"));
  assertFalse(settings.has("autohandai")); assertFalse(settings.path("features").path("slashGoal").asBoolean());
  assertTrue(original.path("features").path("slashGoal").asBoolean());
  assertFalse(config.environment().containsKey("AUTOHAND_AI_PLAN"));
  assertEquals(settings,JSON.readTree(config.toBuilder().build().environment().get("AUTOHAND_RUN_SETTINGS")));
 }
 @Test void rejectsUnsafeOrAmbiguousConfiguration() throws Exception {
  assertThrows(IllegalArgumentException.class,()->SDKConfig.builder().apiKey("secret").build());
  assertThrows(IllegalArgumentException.class,()->SDKConfig.builder().provider("vertexai").baseUrl("https://user:secret@fixture.test/v1").build());
  var unsafe=JSON.readTree("{\"features\":{\"constructor\":true}}");
  assertThrows(IllegalArgumentException.class,()->SDKConfig.builder().runSettings(unsafe).build());
 }
 @Test void translatesSessionReferencesToCurrentRpcStartup() {
  assertEquals(java.util.List.of("--mode","rpc","--sdk-session-reference","saved"),SDKConfig.builder().sessionId("saved").persistSession(true).build().cliArgs());
  assertEquals(java.util.List.of("--mode","rpc","--sdk-resume-last"),SDKConfig.builder().resume(true).build().cliArgs());
  assertEquals(java.util.List.of("--mode","rpc","--fork","saved"),SDKConfig.builder().sessionId("saved").fork("saved").build().cliArgs());
 }

}
