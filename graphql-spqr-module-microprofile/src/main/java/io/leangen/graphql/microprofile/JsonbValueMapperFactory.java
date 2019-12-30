package io.leangen.graphql.microprofile;

import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;

import javax.json.bind.JsonbConfig;
import java.util.List;
import java.util.Map;

public class JsonbValueMapperFactory implements ValueMapperFactory {

    private final JsonbConfig config;

    private JsonbValueMapperFactory(JsonbConfig config) {
        this.config = config;
    }

    @Override
    public ValueMapper getValueMapper(Map<Class, List<Class<?>>> concreteSubTypes, GlobalEnvironment environment) {
        return new JsonbValueMapper(config);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private JsonbConfig config;

        public Builder withConfig(JsonbConfig config) {
            this.config = config;
            return this;
        }

        public JsonbValueMapperFactory build() {
            return new JsonbValueMapperFactory(config);
        }
    }
}
