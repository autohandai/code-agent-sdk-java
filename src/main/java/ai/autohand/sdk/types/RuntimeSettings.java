package ai.autohand.sdk.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;

/** Private run-only settings applied before the CLI constructs a provider. */
final class RuntimeSettings {
 private static final ObjectMapper JSON = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
 private RuntimeSettings() {}
 static ObjectNode build(JsonNode raw, FeatureFlagSettings features, String provider, String key, String url, String model, String plan) {
  if(raw!=null && !raw.isObject()) throw new IllegalArgumentException("Run settings must be a JSON object");
  ObjectNode settings=raw==null?JSON.createObjectNode():((ObjectNode)raw).deepCopy();
  validate(settings,0);
  if(features!=null) merge(settings,JSON.createObjectNode().set("features",JSON.valueToTree(features)));
  if(provider==null) {
   if(key!=null||url!=null||plan!=null) throw new IllegalArgumentException("A provider is required for provider credentials and settings");
   return settings;
  }
  ObjectNode section=JSON.createObjectNode();
  if(model!=null) section.put("model",model);
  if(key!=null) {
   section.put(provider.equals("vertexai")?"authToken":"apiKey",key);
   switch(provider) {
    case "autohandai","openai","xai" -> section.put("authMode","api-key");
    case "azure" -> section.put("authMethod","api-key");
    case "bedrock" -> section.put("authMode","bedrock-api-key");
    default -> { }
   }
  }
  if(url!=null) section.put(provider.equals("vertexai")||provider.equals("bedrock")?"endpoint":"baseUrl",provider.equals("vertexai")?vertexEndpoint(url):url);
  if(provider.equals("autohandai")) {
   if(plan!=null) section.put("plan",plan);
   else if((key!=null||url!=null) && !settings.path(provider).has("plan")) section.put("plan","cloud");
  }
  if(!section.isEmpty()) {
   ObjectNode overlay=JSON.createObjectNode();
   if(provider.startsWith("custom:")) overlay.set("customProviders",JSON.createObjectNode().set(provider.substring(7),section));
   else if(provider.startsWith("extension:")) overlay.set("extensionProviders",JSON.createObjectNode().set(provider,section));
   else overlay.set(provider,section);
   merge(settings,overlay);
  }
  return settings;
 }
 private static String vertexEndpoint(String value) {
  try {
   URI uri=URI.create(value.contains("://")?value:"https://"+value);
   if(!"https".equals(uri.getScheme()) || uri.getHost()==null || uri.getUserInfo()!=null || uri.getPort()!=-1
       || (uri.getPath()!=null&&!uri.getPath().isEmpty()&&!uri.getPath().equals("/")) || uri.getQuery()!=null || uri.getFragment()!=null)
    throw new IllegalArgumentException();
   return uri.getHost();
  } catch(IllegalArgumentException e) { throw new IllegalArgumentException("Vertex endpoint must be an HTTPS origin or hostname"); }
 }
 private static void validate(JsonNode node,int depth) {
  if(depth>256) throw new IllegalArgumentException("Run settings exceed maximum nesting depth");
  if(node.isPojo() || node.isMissingNode() || (node.isFloatingPointNumber()&&!Double.isFinite(node.doubleValue())))
   throw new IllegalArgumentException("Run settings must contain JSON values");
  if(node.isObject()) {
   var fields=node.fields();
   while(fields.hasNext()) { var entry=fields.next();
    if(entry.getKey().equals("__proto__")||entry.getKey().equals("constructor")||entry.getKey().equals("prototype"))
     throw new IllegalArgumentException("Run settings contain an unsafe setting path");
    validate(entry.getValue(),depth+1);
   }
  } else if(node.isArray()) for(JsonNode child:node) validate(child,depth+1);
 }
 private static void merge(ObjectNode target,ObjectNode source) {
  var fields=source.fields();
  while(fields.hasNext()) { var entry=fields.next();
   if(entry.getValue().isObject() && target.path(entry.getKey()).isObject()) merge((ObjectNode)target.get(entry.getKey()),(ObjectNode)entry.getValue());
   else target.set(entry.getKey(),entry.getValue().deepCopy());
  }
 }
}
