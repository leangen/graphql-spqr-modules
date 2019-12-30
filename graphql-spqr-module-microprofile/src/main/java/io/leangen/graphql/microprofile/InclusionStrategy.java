package io.leangen.graphql.microprofile;

import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.metadata.strategy.DefaultInclusionStrategy;
import io.leangen.graphql.util.ClassUtils;
import org.eclipse.microprofile.graphql.Ignore;

import javax.json.bind.annotation.JsonbTransient;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.List;

public class InclusionStrategy extends DefaultInclusionStrategy {

    private final List<Class<? extends Annotation>> markers = Arrays.asList(
            Ignore.class,
            JsonbTransient.class,
            GraphQLIgnore.class
    );

    public InclusionStrategy(String... basePackages) {
        super(basePackages);
    }

    @Override
    protected boolean isDirectlyIgnored(AnnotatedElement element) {
        return markers.stream().anyMatch(element::isAnnotationPresent) || super.isDirectlyIgnored(element);
    }

    @Override
    protected boolean isIgnored(AnnotatedElement element) {
        return markers.stream().anyMatch(annotation -> ClassUtils.hasAnnotation(element, annotation))
                || super.isIgnored(element);
    }
}
