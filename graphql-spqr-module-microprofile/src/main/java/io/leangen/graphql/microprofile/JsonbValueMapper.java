package io.leangen.graphql.microprofile;

import io.leangen.graphql.metadata.InputField;
import io.leangen.graphql.metadata.TypedElement;
import io.leangen.graphql.metadata.exceptions.TypeMappingException;
import io.leangen.graphql.metadata.messages.MessageBundle;
import io.leangen.graphql.metadata.strategy.InclusionStrategy;
import io.leangen.graphql.metadata.strategy.InputFieldInclusionParams;
import io.leangen.graphql.metadata.strategy.type.TypeTransformer;
import io.leangen.graphql.metadata.strategy.value.InputFieldBuilder;
import io.leangen.graphql.metadata.strategy.value.InputFieldBuilderParams;
import io.leangen.graphql.metadata.strategy.value.InputParsingException;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.config.PropertyNamingStrategy;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Member;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class JsonbValueMapper implements ValueMapper, InputFieldBuilder {

    private final JsonbPropertyReader configReader;
    private final Jsonb jsonb;
    private final ClassValue<Jsonb> jsonbCache;

    private static final Logger log = LoggerFactory.getLogger(JsonbValueMapper.class);

    @SuppressWarnings("WeakerAccess")
    public JsonbValueMapper(JsonbConfig config) {
        config = config != null ? config : new JsonbConfig();
        this.configReader = new JsonbPropertyReader(config);
        this.jsonb = JsonbBuilder.create(config);
        this.jsonbCache = new ClassValue<Jsonb>() {
            @Override
            protected Jsonb computeValue(Class type) {
                return JsonbBuilder.create(configReader.getConfig().withPropertyNamingStrategy(
                        new InputFieldPropertyNamingStrategy(type, configReader.getPropertyNamingStrategy())));
            }
        };
    }

    @Override
    public Set<InputField> getInputFields(InputFieldBuilderParams params) {
        return JsonbUtils.findProperties(ClassUtils.getRawType(params.getType().getType())).stream()
                .filter(prop -> isIncluded(params.getType(), prop, params.getEnvironment().inclusionStrategy))
                .map(prop -> toInputField(params, prop))
                .collect(Collectors.toSet());
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T fromInput(Object graphQLInput, Type sourceType, AnnotatedType outputType) throws InputParsingException {
        Type outType = outputType.getType();
        if (graphQLInput == null || outType.equals(sourceType)) {
            return (T) graphQLInput;
        }
        String json = null;
        try {
            json = jsonb.toJson(graphQLInput, sourceType);
            T value = getJsonb(outType).fromJson(json, outType);
            log.trace("fromInput {} | {} | {} -> {}", graphQLInput, sourceType, outputType, value);
            return value;
        } catch (Exception ex) {
            throw new InputParsingException(json != null ? json : graphQLInput, outType, ex);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T fromString(String json, AnnotatedType outputType) throws InputParsingException {
        Type type = outputType.getType();
        if (json == null || String.class.equals(type)) {
            return (T) json;
        }

        try {
            T value = getJsonb(type).fromJson(json, type);
            log.trace("fromString {} | {} -> {}", json, outputType, value);
            return value;
        } catch (Exception ex) {
            throw new InputParsingException(json, type, ex);
        }
    }

    @Override
    public String toString(Object output, AnnotatedType type) {
        String result;
        if (output == null || output.getClass().equals(String.class)) {
            result = (String) output;
        } else {
            result = jsonb.toJson(output);
        }
        log.trace("toString {} -> {}", output, result);
        return result;
    }

    private InputField toInputField(InputFieldBuilderParams params, Property prop) {
        MessageBundle messageBundle = params.getEnvironment().messageBundle;
        TypedElement element = reduce(params.getType(), prop, params.getEnvironment().typeTransformer);
        Object defaultValue = Optional.ofNullable(element.getAnnotation(DefaultValue.class))
                .map(ann -> {
                    if (String.class.equals(ClassUtils.getRawType(element.getJavaType().getType()))) {
                        return ann.value();
                    }
                    return (Object) jsonb.fromJson(messageBundle.interpolate(ann.value()), element.getJavaType().getType());
                })
                .orElse(null);
        String description = Optional.ofNullable(element.getAnnotation(Description.class))
                .map(desc -> messageBundle.interpolate(desc.value()))
                .orElse("");
        return new InputField(
                configReader.getPropertyNamingStrategy().translateName(prop.getInternalName()),
                description,
                element,
                null,
                defaultValue
        );
    }

    private boolean isIncluded(AnnotatedType type, Property prop, InclusionStrategy inclusionStrategy) {

        InputFieldInclusionParams params = InputFieldInclusionParams.builder()
                .withType(type)
                .withElementDeclaringClass(prop.getDeclaringClass())
                .withElements(prop.getElements())
                .withDeserializationInfo(true, false)
                .build();
        return (prop.getCtorParam() != null
                || (prop.getField() != null && configReader.getPropertyVisibilityStrategy().isVisible(prop.getField()))
                || (prop.getSetter() != null && configReader.getPropertyVisibilityStrategy().isVisible(prop.getSetter())))
                && inclusionStrategy.includeInputField(params);
    }

    private TypedElement reduce(AnnotatedType declaringType, Property prop, TypeTransformer transformer) {
        Optional<TypedElement> constParam = Optional.ofNullable(prop.getCtorParam())
                .map(param -> {
                    AnnotatedType type = ClassUtils.getParameterTypes(param.getDeclaringExecutable(), declaringType)[indexOf(param)];
                    return new TypedElement(transform(type, param, transformer), param);
                });
        Optional<TypedElement> setter = Optional.ofNullable(prop.getSetter())
                .map(str -> {
                    AnnotatedType type = ClassUtils.getParameterTypes(str, declaringType)[0];
                    return new TypedElement(transform(type, str, declaringType, transformer), str);
                });
        Optional<TypedElement> field = Optional.ofNullable(prop.getField())
                .map(fld -> {
                    AnnotatedType type = ClassUtils.getFieldType(fld, declaringType);
                    return new TypedElement(transform(type, fld, declaringType, transformer), fld);
                });

        Optional<TypedElement> mutator = Utils.flatten(constParam, setter, field).findFirst();
        Optional<TypedElement> explicit = Utils.flatten(constParam, setter, field)
                .filter(e -> e.isAnnotationPresent(Name.class))
                .findFirst();

        List<TypedElement> elements = Utils.flatten(explicit, mutator, field).distinct().collect(Collectors.toList());
        return new TypedElement(elements);
    }

    private AnnotatedType transform(AnnotatedType type, Member member, AnnotatedType declaringType, TypeTransformer transformer) {
        try {
            return transformer.transform(type);
        } catch (TypeMappingException e) {
            throw TypeMappingException.ambiguousMemberType(member, declaringType, e);
        }
    }

    private AnnotatedType transform(AnnotatedType type, Parameter parameter, TypeTransformer transformer) {
        try {
            return transformer.transform(type);
        } catch (TypeMappingException e) {
            throw TypeMappingException.ambiguousParameterType(parameter.getDeclaringExecutable(), parameter, e);
        }
    }

    private int indexOf(Parameter param) {
        int i = 0;
        for (Parameter p : param.getDeclaringExecutable().getParameters()) {
            if (!p.isSynthetic() && !p.isImplicit()) {
                if (p == param) {
                    return i;
                }
                i++;
            }
        }
        throw new IllegalArgumentException("Parameter " + param + " not found in its declaring executable");
    }

    private Jsonb getJsonb(Type type) {
        return jsonbCache.get(ClassUtils.getRawType(type));
    }

    private static class InputFieldPropertyNamingStrategy implements PropertyNamingStrategy {

        private final PropertyNamingStrategy original;
        private final Map<String, String> propertyNameMap;

        private static final Logger log = LoggerFactory.getLogger(InputFieldPropertyNamingStrategy.class);

        InputFieldPropertyNamingStrategy(Class<?> type, PropertyNamingStrategy original) {
            Map<String, String> propertyNameMap = new ConcurrentHashMap<>();
            JsonbUtils.findProperties(type).stream()
                    .filter(prop -> prop.getExplicitName() != null)
                    .forEach(prop -> {
                        log.trace("addMapping {} -> {}", prop.getInternalName(), prop.getExplicitName());
                        propertyNameMap.put(prop.getInternalName(), prop.getExplicitName());
                    });
            this.original = original;
            this.propertyNameMap = Collections.unmodifiableMap(propertyNameMap);
        }

        @Override
        public String translateName(String propertyName) {
            String translated = propertyNameMap.getOrDefault(propertyName, original.translateName(propertyName));
            log.trace("translateName​ {} -> {}", propertyName, translated);
            return translated;
        }
    }
}
