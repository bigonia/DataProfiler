package com.dataprofiler.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom deserializer for sampleRows field to handle backward compatibility
 * Supports both old format (object with headers/rows) and new format (List<List<Object>>)
 */
public class SampleRowsDeserializer extends JsonDeserializer<List<List<Object>>> {

    @Override
    public List<List<Object>> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonToken token = parser.getCurrentToken();
        
        if (token == JsonToken.VALUE_NULL) {
            return null;
        }
        
        if (token == JsonToken.START_ARRAY) {
            // New format: direct array of arrays
            ObjectMapper mapper = (ObjectMapper) parser.getCodec();
            return mapper.readValue(parser, mapper.getTypeFactory().constructCollectionType(
                    List.class, 
                    mapper.getTypeFactory().constructCollectionType(List.class, Object.class)
            ));
        }
        
        if (token == JsonToken.START_OBJECT) {
            // Old format: object with headers and rows
            JsonNode node = parser.getCodec().readTree(parser);
            
            if (node.has("rows") && node.get("rows").isArray()) {
                ObjectMapper mapper = (ObjectMapper) parser.getCodec();
                return mapper.convertValue(
                    node.get("rows"), 
                    mapper.getTypeFactory().constructCollectionType(
                        List.class, 
                        mapper.getTypeFactory().constructCollectionType(List.class, Object.class)
                    )
                );
            }
        }
        
        // Fallback: return empty list
        return new ArrayList<>();
    }
}