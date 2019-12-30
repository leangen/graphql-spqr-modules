package io.leangen.graphql.microprofile;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.GraphQLScalarType;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAccessor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static io.leangen.graphql.util.Scalars.literalOrException;
import static io.leangen.graphql.util.Scalars.serializationException;
import static io.leangen.graphql.util.Scalars.valueParsingException;

public class TemporalScalars {

    public static final GraphQLScalarType GraphQLLocalDate = temporalScalar("Date", "a local date");

    public static final GraphQLScalarType GraphQLLocalTime = temporalScalar("Time", "a local time");

    public static final GraphQLScalarType GraphQLLocalDateTime = temporalScalar("DateTime", "a local date and time");


    private static GraphQLScalarType temporalScalar(String name, String description) {
        return GraphQLScalarType.newScalar()
                .name(name)
                .description("Built-in scalar representing " + description)
                .coercing(new Coercing<String, String>() {

                    @Override
                    public String serialize(Object dataFetcherResult) {
                        if (dataFetcherResult instanceof TemporalAccessor || dataFetcherResult instanceof String) {
                            return dataFetcherResult.toString();
                        }
                        throw serializationException(dataFetcherResult, TemporalAccessor.class, String.class);
                    }

                    @Override
                    public String parseValue(Object input) {
                        if (input instanceof String) {
                            return (String) input;
                        }
                        throw valueParsingException(input, String.class);
                    }

                    @Override
                    public String parseLiteral(Object input) {
                        return literalOrException(input, StringValue.class).getValue();
                    }
                }).build();
    }

    private static final Map<Type, GraphQLScalarType> SCALAR_MAPPING = getScalarMapping();

    public static boolean isScalar(Type javaType) {
        return SCALAR_MAPPING.containsKey(javaType);
    }

    public static GraphQLScalarType toGraphQLScalarType(Type javaType) {
        return SCALAR_MAPPING.get(javaType);
    }

    private static Map<Type, GraphQLScalarType> getScalarMapping() {
        Map<Type, GraphQLScalarType> scalarMapping = new HashMap<>();
        scalarMapping.put(LocalDate.class, GraphQLLocalDate);
        scalarMapping.put(LocalTime.class, GraphQLLocalTime);
        scalarMapping.put(LocalDateTime.class, GraphQLLocalDateTime);
        return Collections.unmodifiableMap(scalarMapping);
    }
}
