package io.leangen.graphql.microprofile;

import io.leangen.graphql.util.Utils;
import org.eclipse.microprofile.graphql.Name;

import javax.json.bind.annotation.JsonbProperty;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Property {

    private final String internalName;
    private final String explicitName;

    private final Field field;
    private final Method setter;
    private final Parameter ctorParam;

    Property(String defaultName, Field field, Method setter, Parameter ctorParam) {
        Utils.requireNonEmpty(defaultName);
        if (setter == null && ctorParam == null && field == null) {
            throw new IllegalArgumentException("At least one writable element is required");
        }
        this.field = field;
        this.setter = setter;
        this.ctorParam = ctorParam;

        this.internalName = Stream.of(ctorParam, setter, field)
                .filter(el -> el != null && el.isAnnotationPresent(JsonbProperty.class))
                .map(el -> el.getAnnotation(JsonbProperty.class).value())
                .findFirst()
                .orElse(defaultName);

        this.explicitName = Stream.of(ctorParam, setter, field)
                .filter(el -> el != null && el.isAnnotationPresent(Name.class))
                .map(el -> el.getAnnotation(Name.class).value())
                .findFirst()
                .orElse(null);
    }

    String getInternalName() {
        return internalName;
    }

    String getExplicitName() {
        return explicitName;
    }

    Field getField() {
        return field;
    }

    Method getSetter() {
        return setter;
    }

    Parameter getCtorParam() {
        return ctorParam;
    }

    List<AnnotatedElement> getElements() {
        return Stream.of(ctorParam, setter, field)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    Class<?> getDeclaringClass() {
        if (ctorParam != null) {
            return ctorParam.getDeclaringExecutable().getDeclaringClass();
        }
        if (setter != null) {
            return setter.getDeclaringClass();
        }
        return field.getDeclaringClass();
    }
}
