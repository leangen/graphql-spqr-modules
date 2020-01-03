package io.leangen.graphql.microprofile;

import io.leangen.graphql.annotations.GraphQLSubscription;
import io.leangen.graphql.metadata.Resolver;
import io.leangen.graphql.metadata.TypedElement;
import io.leangen.graphql.metadata.exceptions.TypeMappingException;
import io.leangen.graphql.metadata.strategy.query.PublicResolverBuilder;
import io.leangen.graphql.metadata.strategy.query.ResolverBuilderParams;
import io.leangen.graphql.metadata.strategy.value.Property;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AnnotationResolverBuilder extends PublicResolverBuilder {

    public AnnotationResolverBuilder() {
        this.operationInfoGenerator = new OperationInfoGenerator();
        this.argumentBuilder = new ArgumentBuilder();
        this.propertyElementReducer = AnnotationResolverBuilder::annotatedElementReducer;
    }

    @Override
    public Collection<Resolver> buildQueryResolvers(ResolverBuilderParams params) {
        return super.buildQueryResolvers(params).stream()
                .flatMap(res -> {
                    if (!res.getSourceTypes().isEmpty() && res.getTypedElement().isAnnotationPresent(Query.class)) {
                        return Stream.of(res, topLevelResolver(res));
                    }
                    return Stream.of(res);
                })
                .collect(Collectors.toList());
    }

    @Override
    protected boolean isQuery(Method method, ResolverBuilderParams params) {
        return method.isAnnotationPresent(Query.class)
                || Arrays.stream(method.getParameters()).anyMatch(param -> param.isAnnotationPresent(Source.class));
    }

    @Override
    protected boolean isQuery(Field field, ResolverBuilderParams params) {
        return field.isAnnotationPresent(Query.class);
    }

    @Override
    protected boolean isQuery(Property property, ResolverBuilderParams params) {
        return isQuery(property.getGetter(), params) || isQuery(property.getField(), params);
    }

    @Override
    protected boolean isMutation(Method method, ResolverBuilderParams params) {
        return method.isAnnotationPresent(Mutation.class);
    }

    @Override
    protected boolean isSubscription(Method method, ResolverBuilderParams params) {
        return method.isAnnotationPresent(GraphQLSubscription.class);
    }

    //TODO Solve in SPQR and removed this workaround
    private Resolver topLevelResolver(Resolver res) {
        return new Resolver(
                res.getOperationName(),
                res.getOperationDescription(),
                res.getOperationDeprecationReason(),
                res.isBatched(),
                res.getExecutable(),
                res.getTypedElement(),
                res.getArguments(),
                res.getComplexityExpression()) {

            @Override
            public Set<Type> getSourceTypes() {
                return Collections.emptySet();
            }

            @Override
            public boolean equals(Object that) {
                return false; //This instance must not be removed by deduplication
            }
        };
    }

    public static TypedElement annotatedElementReducer(TypedElement field, TypedElement getter) {
        if (field.isAnnotationPresent(Query.class) && getter.isAnnotationPresent(Query.class)) {
            throw new TypeMappingException("Ambiguous mapping of " + field);
        }
        return field.isAnnotationPresent(Query.class) ? field : getter;
    }
}
