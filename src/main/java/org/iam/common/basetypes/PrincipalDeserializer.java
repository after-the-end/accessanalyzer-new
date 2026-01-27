package org.iam.common.basetypes;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.iam.common.vars.VarKey;

import java.io.IOException;
import java.util.*;

public class PrincipalDeserializer extends JsonDeserializer<Map<VarKey, Set<String>>> {

    @Override
    public Map<VarKey, Set<String>> deserialize(JsonParser jp, DeserializationContext context)
            throws IOException, JacksonException {
        JsonNode node = jp.getCodec().readTree(jp);
        Map<VarKey, Set<String>> principals = new HashMap<>();

        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            VarKey prpKey = VarKey.fromString(field.getKey());
            Set<String> values = new HashSet<>();

            JsonNode arrayNode = field.getValue();

            if (arrayNode.isArray()) {
                for (JsonNode valueNode : arrayNode) {
                    values.add(valueNode.asText());
                }
            } else {
                values.add(arrayNode.asText());
            }

            principals.put(prpKey, values);
        }
        return principals;
    }
}