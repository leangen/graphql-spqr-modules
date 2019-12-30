package io.leangen.graphql.microprofile;

import io.leangen.graphql.metadata.OperationArgument;
import io.leangen.graphql.metadata.messages.MessageBundle;
import io.leangen.graphql.metadata.strategy.query.AnnotatedArgumentBuilder;
import io.leangen.graphql.metadata.strategy.query.ArgumentBuilderParams;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Source;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.Optional;

public class ArgumentBuilder extends AnnotatedArgumentBuilder {

    @Override
    protected OperationArgument buildResolverArgument(Parameter parameter, AnnotatedType parameterType, ArgumentBuilderParams builderParams) {
        ValueMapper valueMapper = JsonbValueMapperFactory.builder().build().getValueMapper(Collections.emptyMap(), builderParams.getEnvironment());
        return new OperationArgument(
                parameterType,
                getArgumentName(parameter, parameterType, builderParams),
                getArgumentDescription(parameter, parameterType, builderParams.getEnvironment().messageBundle),
                defaultValue(parameter, parameterType, valueMapper),
                parameter,
                parameter.isAnnotationPresent(Source.class),
                builderParams.getEnvironment().inclusionStrategy.includeArgument(parameter, parameterType)
        );
    }

    @Override
    protected String getArgumentName(Parameter parameter, AnnotatedType parameterType, ArgumentBuilderParams builderParams) {
        return Optional.ofNullable(parameter.getAnnotation(Name.class))
                .map(name -> builderParams.getEnvironment().messageBundle.interpolate(name.value()))
                .orElseGet(() -> super.getArgumentName(parameter, parameterType, builderParams));
    }

    @Override
    protected String getArgumentDescription(Parameter parameter, AnnotatedType parameterType, MessageBundle messageBundle) {
        return Optional.ofNullable(parameter.getAnnotation(Description.class))
                .map(description -> messageBundle.interpolate(description.value()))
                .orElseGet(() -> super.getArgumentDescription(parameter, parameterType, messageBundle));
    }

    protected Object defaultValue(Parameter parameter, AnnotatedType parameterType, ValueMapper valueMapper) {
        return Optional.ofNullable(parameter.getAnnotation(DefaultValue.class))
                .map(def -> valueMapper.fromString(def.value(), parameterType))
                .orElse(null);
    }
}
