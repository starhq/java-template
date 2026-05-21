package com.github.starhq.template.config.json.serializer;

import com.github.starhq.template.config.json.properties.SensitiveFieldProperties;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.PropertyWriter;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;

/**
 * A Jackson {@link SimpleBeanPropertyFilter} that intercepts JSON serialization to mask sensitive data.
 *
 * <p>This filter dynamically inspects the name of the field currently being serialized. If the field name
 * matches a predefined sensitive list (from {@link SensitiveFieldProperties}), it replaces the actual
 * value with a masked placeholder (e.g., "*****"). Otherwise, it delegates to standard serialization.
 *
 * <p><b>Integration Requirement:</b> For this filter to take effect, two steps are required:
 * <ol>
 *   <li>The target DTO class must be annotated with {@code @JsonFilter(SensitivePropertyFilter.FILTER_NAME)}.</li>
 *   <li>The {@link tools.jackson.databind.ObjectMapper} must be configured to register this filter ID
 *       via {@code mapper.setFilterProvider(new SimpleFilterProvider().addFilter(FILTER_NAME, new SensitivePropertyFilter(...)))}.</li>
 * </ol>
 *
 * <p><b>Note:</b> Uses Jackson 3.x artifacts ({@code tools.jackson.*}).
 *
 * @author starhq
 * @see SensitiveFieldProperties
 */
@RequiredArgsConstructor
public class SensitivePropertyFilter extends SimpleBeanPropertyFilter {

    /**
     * The properties component providing the rules for sensitive field detection.
     */
    private final SensitiveFieldProperties sensitiveProperties;

    /**
     * The unique identifier for this filter.
     * <p>This constant must exactly match the value provided in the {@code @JsonFilter} annotation
     * on the DTO classes requiring masking.
     */
    public static final String FILTER_NAME = "sensitiveFilter";

    /**
     * Intercepts the serialization of a single property to apply masking logic if necessary.
     *
     * <p><b>Execution Flow:</b> Jackson calls this method for every getter/field during serialization.
     * If the property is sensitive, we manually write the masked string to the JSON output stream.
     * If not, we fall back to the default {@link PropertyWriter} to serialize the real value.
     *
     * @param pojo     the root object being serialized (usually ignored, as we focus on the specific property)
     * @param g        the {@link JsonGenerator} used to manually write JSON tokens to the output stream
     * @param provider the context providing serialization configurations (usually ignored here)
     * @param writer   the {@link PropertyWriter} representing the specific field/getter currently being processed
     * @throws Exception if an I/O error occurs while writing to the JSON generator
     */
    @Override
    public void serializeAsProperty(Object pojo, JsonGenerator g, SerializationContext provider, PropertyWriter writer)
            throws Exception {
        // Extract the exact JSON key name that will be written (e.g., "password", "accessToken")
        String fieldName = writer.getName();

        if (sensitiveProperties.isSensitive(fieldName)) {
            // Bypass the actual value and manually write the masked placeholder directly to the JSON stream
            g.writeStringProperty(fieldName, sensitiveProperties.getMaskValue());
        } else {
            // Not sensitive: proceed with standard Jackson serialization logic
            writer.serializeAsProperty(pojo, g, provider);
        }
    }
}