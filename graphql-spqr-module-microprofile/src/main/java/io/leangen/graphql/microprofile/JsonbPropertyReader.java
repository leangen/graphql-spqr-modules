package io.leangen.graphql.microprofile;

import javax.json.bind.JsonbConfig;
import javax.json.bind.JsonbException;
import javax.json.bind.config.PropertyNamingStrategy;
import javax.json.bind.config.PropertyVisibilityStrategy;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;

public class JsonbPropertyReader {

    private final JsonbConfig config;
    private final PropertyNamingStrategy propertyNamingStrategy;
    private final PropertyVisibilityStrategy propertyVisibilityStrategy;

    public JsonbPropertyReader(JsonbConfig config) {
        this.config = config;
        this.propertyNamingStrategy = getPropertyNamingStrategy(config);
        this.propertyVisibilityStrategy = getPropertyVisibilityStrategy(config);
    }

    public JsonbConfig getConfig() {
        return config;
    }

    public PropertyVisibilityStrategy getPropertyVisibilityStrategy() {
        return this.propertyVisibilityStrategy;
    }

    public PropertyNamingStrategy getPropertyNamingStrategy() {
        return this.propertyNamingStrategy;
    }

    private PropertyNamingStrategy getPropertyNamingStrategy(JsonbConfig config) {
        Optional<Object> property = config.getProperty("jsonb.property-naming-strategy");
        if (!property.isPresent()) {
            return name -> name;
        } else {
            Object propertyNamingStrategy = property.get();
            if (propertyNamingStrategy instanceof String) {
                throw new JsonbException("String based property naming strategies not supported: " + propertyNamingStrategy);
            }
            return (PropertyNamingStrategy)property.get();
        }
    }

    private PropertyVisibilityStrategy getPropertyVisibilityStrategy(JsonbConfig config) {
        return config.getProperty("jsonb.property-visibility-strategy")
                .map(prop -> (PropertyVisibilityStrategy) prop)
                .orElseGet(DefaultVisibilityStrategy::new);
    }

    private static class DefaultVisibilityStrategy implements PropertyVisibilityStrategy {

        @Override
        public boolean isVisible(Field field) {
            return Modifier.isPublic(field.getModifiers());
        }

        @Override
        public boolean isVisible(Method method) {
            return Modifier.isPublic(method.getModifiers());
        }
    }
}
