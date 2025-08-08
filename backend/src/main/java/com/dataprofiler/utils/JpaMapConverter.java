package com.dataprofiler.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.HashMap;
import java.util.Map;

/**
 * JPA Converter for Map<String, String> to JSON string conversion
 * Used for storing dynamic properties in DataSourceConfig entity
 */
@Converter
public class JpaMapConverter implements AttributeConverter<Map<String, String>, String> {

    private static final Logger logger = LoggerFactory.getLogger(JpaMapConverter.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "{}";
        }
        
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (Exception ex) {
            logger.error("Error converting map to JSON string", ex);
            return "{}";
        }
    }

    @Override
    public Map<String, String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            return objectMapper.readValue(dbData, new TypeReference<Map<String, String>>() {});
        } catch (Exception ex) {
            logger.error("Error converting JSON string to map: {}", dbData, ex);
            return new HashMap<>();
        }
    }
}