package io.leangen.graphql.microprofile;

import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLNonNull;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.mapping.SchemaTransformer;
import io.leangen.graphql.metadata.DirectiveArgument;
import io.leangen.graphql.metadata.InputField;
import io.leangen.graphql.metadata.Operation;
import io.leangen.graphql.metadata.OperationArgument;
import org.eclipse.microprofile.graphql.NonNull;

public class NonNullTransformer implements SchemaTransformer {

    @Override
    public GraphQLFieldDefinition transformField(GraphQLFieldDefinition field, Operation operation, OperationMapper operationMapper, BuildContext buildContext) {
        if (operation.getTypedElement().isAnnotationPresent(NonNull.class)) {
            return field.transform(builder -> builder.type(GraphQLNonNull.nonNull(field.getType())));
        }
        return field;
    }

    @Override
    public GraphQLInputObjectField transformInputField(GraphQLInputObjectField field, InputField inputField, OperationMapper operationMapper, BuildContext buildContext) {
        if (inputField.getTypedElement().isAnnotationPresent(NonNull.class) && inputField.getDefaultValue() == null) {
            return field.transform(builder -> builder.type(GraphQLNonNull.nonNull(field.getType())));
        }
        return field;
    }

    @Override
    public GraphQLArgument transformArgument(GraphQLArgument argument, OperationArgument operationArgument, OperationMapper operationMapper, BuildContext buildContext) {
        if (operationArgument.getTypedElement().isAnnotationPresent(NonNull.class) && argument.getDefaultValue() == null) {
            return argument.transform(builder -> builder.type(GraphQLNonNull.nonNull(argument.getType())));
        }
        return argument;
    }

    @Override
    public GraphQLArgument transformArgument(GraphQLArgument argument, DirectiveArgument directiveArgument, OperationMapper operationMapper, BuildContext buildContext) {
        if (directiveArgument.getTypedElement().isAnnotationPresent(NonNull.class) && argument.getDefaultValue() == null) {
            return argument.transform(builder -> builder.type(GraphQLNonNull.nonNull(argument.getType())));
        }
        return argument;
    }
}
