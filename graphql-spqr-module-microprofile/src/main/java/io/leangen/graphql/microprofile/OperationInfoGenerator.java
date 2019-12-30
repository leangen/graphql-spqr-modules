package io.leangen.graphql.microprofile;

import io.leangen.graphql.metadata.strategy.query.OperationInfoGeneratorParams;
import io.leangen.graphql.metadata.strategy.query.PropertyOperationInfoGenerator;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;

import javax.json.bind.annotation.JsonbProperty;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class OperationInfoGenerator implements io.leangen.graphql.metadata.strategy.query.OperationInfoGenerator {

    private final PropertyOperationInfoGenerator delegate = new PropertyOperationInfoGenerator();

    @Override
    public String name(OperationInfoGeneratorParams params) {
        String explicitName = map(params, Query::value, Mutation::value);
        String fallbackName = Optional.ofNullable(params.getElement().getAnnotation(Name.class))
                .map(Name::value)
                .orElse(null);
        String jsonName = Optional.ofNullable(params.getElement().getAnnotation(JsonbProperty.class))
                .map(JsonbProperty::value)
                .orElse(null);
        String spqrName = delegate.name(params);
        List<AnnotatedElement> elements = params.getElement().getElements();
        Optional<String> field = Utils.extractInstances(elements, Field.class).findFirst().map(Field::getName);
        Optional<String> getter = Utils.extractInstances(elements, Method.class).findFirst().map(ClassUtils::getFieldNameFromGetter);

        return Utils.coalesce(explicitName, fallbackName, jsonName, spqrName, Utils.or(field, getter).get());
    }

    @Override
    public String description(OperationInfoGeneratorParams params) {
        return Optional.ofNullable(params.getElement().getAnnotation(Description.class))
                .map(Description::value)
                .orElseGet(() -> delegate.description(params));

        /*return JsonbUtils.findDateFormat(params.getElement())
                .map(fmt -> description + " Format: " + fmt.value())
                .orElse(description);*/
    }

    @Override
    public String deprecationReason(OperationInfoGeneratorParams params) {
        return Optional.ofNullable(params.getElement().getAnnotation(Deprecated.class))
                .map(d -> "Deprecated")
                .orElseGet(() -> delegate.deprecationReason(params));
    }

    private String map(OperationInfoGeneratorParams params,
                       Function<Query, String> queryMapper,
                       Function<Mutation, String> mutationMapper) {
        switch (params.getOperationType()) {
            case QUERY:
                return Optional.ofNullable(params.getElement().getAnnotation(Query.class))
                        .map(queryMapper)
                        .orElse(null);
            case MUTATION:
                return Optional.ofNullable(params.getElement().getAnnotation(Mutation.class))
                        .map(mutationMapper)
                        .orElse(null);
            default: return null;
        }
    }
}
