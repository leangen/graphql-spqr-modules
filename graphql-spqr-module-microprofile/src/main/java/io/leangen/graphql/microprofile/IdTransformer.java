package io.leangen.graphql.microprofile;


import graphql.Scalars;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.mapping.SchemaTransformer;
import io.leangen.graphql.metadata.DirectiveArgument;
import io.leangen.graphql.metadata.InputField;
import io.leangen.graphql.metadata.Operation;
import io.leangen.graphql.metadata.OperationArgument;
import org.eclipse.microprofile.graphql.Id;

public class IdTransformer implements SchemaTransformer {

    @Override
    public GraphQLFieldDefinition transformField(GraphQLFieldDefinition field, Operation operation, OperationMapper operationMapper, BuildContext buildContext) {
        if (operation.getTypedElement().isAnnotationPresent(Id.class)) {
            return field.transform(builder -> builder.type(Scalars.GraphQLID));
        }
        return field;
    }

    @Override
    public GraphQLInputObjectField transformInputField(GraphQLInputObjectField field, InputField inputField, OperationMapper operationMapper, BuildContext buildContext) {
        if (inputField.getTypedElement().isAnnotationPresent(Id.class)) {
            return field.transform(builder -> builder.type(Scalars.GraphQLID));
        }
        return field;
    }

    @Override
    public GraphQLArgument transformArgument(GraphQLArgument argument, OperationArgument operationArgument, OperationMapper operationMapper, BuildContext buildContext) {
        if (operationArgument.getTypedElement().isAnnotationPresent(Id.class)) {
            return argument.transform(builder -> builder.type(Scalars.GraphQLID));
        }
        return argument;
    }

    @Override
    public GraphQLArgument transformArgument(GraphQLArgument argument, DirectiveArgument directiveArgument, OperationMapper operationMapper, BuildContext buildContext) {
        if (directiveArgument.getTypedElement().isAnnotationPresent(Id.class)) {
            return argument.transform(builder -> builder.type(Scalars.GraphQLID));
        }
        return argument;
    }
}
