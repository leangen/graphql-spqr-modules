package io.leangen.graphql.microprofile;

import io.leangen.graphql.generator.JavaDeprecationMappingConfig;
import io.leangen.graphql.metadata.messages.MessageBundle;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.Name;

import java.util.Optional;

public class EnumMapper extends io.leangen.graphql.generator.mapping.common.EnumMapper {

    public EnumMapper(JavaDeprecationMappingConfig javaDeprecationConfig) {
        super(javaDeprecationConfig);
    }

    @Override
    protected String getValueName(Enum<?> value, MessageBundle messageBundle) {
        return Optional.ofNullable(ClassUtils.getEnumConstantField(value).getAnnotation(Name.class))
                .map(Name::value)
                .filter(Utils::isNotEmpty)
                .map(messageBundle::interpolate)
                .orElseGet(() -> super.getValueName(value, messageBundle));
    }

    @Override
    protected String getValueDescription(Enum<?> value, MessageBundle messageBundle) {
        return Optional.ofNullable(ClassUtils.getEnumConstantField(value).getAnnotation(Description.class))
                .map(Description::value)
                .filter(Utils::isNotEmpty)
                .map(messageBundle::interpolate)
                .orElseGet(() -> super.getValueDescription(value, messageBundle));
    }
}
