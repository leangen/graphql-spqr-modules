package io.leangen.graphql.microprofile;

import io.leangen.graphql.metadata.TypedElement;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbDateFormat;
import javax.json.bind.annotation.JsonbProperty;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("WeakerAccess")
public class JsonbUtils {

    static Set<Property> findProperties(Class<?> type) {
        Map<String, Parameter> params = getCreatorParams(type);
        Map<String, Field> fields = getFields(type);
        Map<String, Method> setters = getSetters(type);

        return Utils.concat(params.keySet().stream(), fields.keySet().stream(), setters.keySet().stream())
                .map(name -> new Property(name, fields.get(name), setters.get(name), params.get(name)))
                .collect(Collectors.toSet());
    }

    static Optional<JsonbDateFormat> findDateFormat(TypedElement element) {
        return element.getElements().stream()
                .map(el -> ClassUtils.findApplicableAnnotation(el, JsonbDateFormat.class))
                .filter(Optional::isPresent)
                .findFirst()
                .orElse(Optional.empty());
    }

    private static Map<String, Parameter> getCreatorParams(Class<?> type) {
        return Stream.concat(Arrays.stream(type.getDeclaredConstructors()), Arrays.stream(type.getDeclaredMethods()))
                .filter(ctor -> ctor.isAnnotationPresent(JsonbCreator.class))
                .findFirst()
                .map(executable -> Arrays.stream(executable.getParameters())
                        .filter(p -> !p.isImplicit() && !p.isSynthetic()) //real params only
                        .collect(Collectors.toMap(JsonbUtils::getParamName, Function.identity()))
                )
                .orElse(Collections.emptyMap());
    }

    private static String getParamName(Parameter parameter) {
        return Optional.ofNullable(parameter.getAnnotation(JsonbProperty.class))
                .map(JsonbProperty::value)
                .orElse(parameter.getName());
    }

    private static Map<String, Field> getFields(Class<?> type) {
        return Stream.concat(Arrays.stream(type.getFields()), Arrays.stream(type.getDeclaredFields()))
                .distinct()
                .collect(Collectors.toMap(Field::getName, Function.identity()));
    }

    private static Map<String, Method> getSetters(Class<?> type) {
        return Stream.concat(Arrays.stream(type.getMethods()), Arrays.stream(type.getDeclaredMethods()))
                .distinct()
                .filter(ClassUtils::isSetter)
                .collect(Collectors.toMap(ClassUtils::getFieldNameFromSetter, Function.identity()));
    }
}
