package io.leangen.graphql.microprofile;

import graphql.schema.GraphQLScalarType;
import io.leangen.graphql.execution.InvocationContext;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.execution.ResolverInterceptor;
import io.leangen.graphql.execution.ResolverInterceptorFactory;
import io.leangen.graphql.execution.ResolverInterceptorFactoryParams;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.mapping.ArgumentInjector;
import io.leangen.graphql.generator.mapping.ArgumentInjectorParams;
import io.leangen.graphql.generator.mapping.OutputConverter;
import io.leangen.graphql.generator.mapping.common.CachingMapper;

import javax.json.bind.annotation.JsonbDateFormat;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static io.leangen.graphql.microprofile.JsonbUtils.findDateFormat;

public class TemporalScalarAdapter extends CachingMapper<GraphQLScalarType, GraphQLScalarType>
        implements OutputConverter<String, String>, ArgumentInjector, ResolverInterceptorFactory {

    @Override
    protected GraphQLScalarType toGraphQLType(String typeName, AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext) {
        return TemporalScalars.toGraphQLScalarType(javaType.getType());
    }

    @Override
    protected GraphQLScalarType toGraphQLInputType(String typeName, AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext) {
        return toGraphQLType(typeName, javaType, operationMapper, buildContext);
    }

    @Override
    //Supported types are already stringified by the interceptor
    public String convertOutput(String original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        return original; //Pass-through only. Used to prevent other converters from interfering.
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return TemporalScalars.isScalar(type.getType());
    }

    @Override
    protected String getTypeName(AnnotatedType type, BuildContext buildContext) {
        return TemporalScalars.toGraphQLScalarType(type.getType()).getName();
    }

    @Override
    protected String getInputTypeName(AnnotatedType type, BuildContext buildContext) {
        return getTypeName(type, buildContext);
    }

    @Override
    public Object getArgumentValue(ArgumentInjectorParams params) {
        if (params.getInput() == null) {
            return null;
        }
        JsonbDateFormat fmt = params.getParameter().getAnnotation(JsonbDateFormat.class);
        return LocalDate.parse((String) params.getInput(), DateTimeFormatter.ofPattern(fmt.value(), Locale.forLanguageTag(fmt.locale())));
    }

    @Override
    public boolean supports(AnnotatedType type, Parameter parameter) {
        return parameter.isAnnotationPresent(JsonbDateFormat.class);
    }

    @Override
    public List<ResolverInterceptor> getInterceptors(ResolverInterceptorFactoryParams params) {
        return Optional.of(params.getResolver())
                .filter(res -> supports(res.getReturnType()))
                .flatMap(res -> findDateFormat(res.getTypedElement()))
                .map(fmt -> DateTimeFormatter.ofPattern(fmt.value(), Locale.forLanguageTag(fmt.locale())))
                .map(dft -> Collections.singletonList((ResolverInterceptor) new TemporalScalarStringifier(dft)))
                .orElse(Collections.emptyList());
    }

    private static class TemporalScalarStringifier implements ResolverInterceptor {

        private final DateTimeFormatter dateTimeFormatter;

        TemporalScalarStringifier(DateTimeFormatter dateTimeFormatter) {
            this.dateTimeFormatter = dateTimeFormatter;
        }

        @Override
        public Object aroundInvoke(InvocationContext context, Continuation continuation) throws Exception {
            return dateTimeFormatter.format((TemporalAccessor) continuation.proceed(context));
        }
    }
}
