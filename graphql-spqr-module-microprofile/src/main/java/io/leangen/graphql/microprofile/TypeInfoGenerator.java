package io.leangen.graphql.microprofile;

import io.leangen.graphql.metadata.messages.MessageBundle;
import io.leangen.graphql.metadata.strategy.type.DefaultTypeInfoGenerator;
import io.leangen.graphql.util.Utils;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.Enum;
import org.eclipse.microprofile.graphql.Input;
import org.eclipse.microprofile.graphql.Interface;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Type;

import java.lang.reflect.AnnotatedType;
import java.util.Optional;
import java.util.stream.Stream;

public class TypeInfoGenerator extends DefaultTypeInfoGenerator {

    @Override
    protected String generateBaseName(AnnotatedType type, MessageBundle messageBundle) {
        return Stream.of(
                Optional.ofNullable(type.getAnnotation(Interface.class)).map(Interface::value),
                Optional.ofNullable(type.getAnnotation(Type.class)).map(Type::value),
                Optional.ofNullable(type.getAnnotation(Name.class)).map(Name::value))
                .map(optional -> optional.filter(Utils::isNotEmpty))
                .reduce(Utils::or).get()
                .map(messageBundle::interpolate)
                .orElseGet(() -> super.generateBaseName(type, messageBundle));
    }

    @Override
    public String generateTypeDescription(AnnotatedType type, MessageBundle messageBundle) {
        return Optional.ofNullable(type.getAnnotation(Description.class))
                .map(Description::value)
                .map(messageBundle::interpolate)
                .orElse(super.generateTypeDescription(type, messageBundle));
    }

    @Override
    public String generateInputTypeName(AnnotatedType type, MessageBundle messageBundle) {
        return Utils.or(
                Optional.ofNullable(type.getAnnotation(Input.class))
                        .map(Input::value)
                        .filter(Utils::isNotEmpty),
                Optional.ofNullable(type.getAnnotation(Name.class))
                        .map(Name::value)
                        .filter(Utils::isNotEmpty))
                .map(messageBundle::interpolate)
                .orElseGet(() -> super.generateInputTypeName(type, messageBundle));
    }

    @Override
    public String generateEnumTypeName(AnnotatedType type, MessageBundle messageBundle) {
        return Optional.ofNullable(type.getAnnotation(Enum.class))
                .map(Enum::value)
                .filter(Utils::isNotEmpty)
                .map(messageBundle::interpolate)
                .orElseGet(() -> generateBaseName(type, messageBundle));
    }
}
