package io.leangen.graphql.microprofile;

import io.leangen.graphql.generator.mapping.common.IdAdapter;
import io.leangen.graphql.generator.mapping.common.ScalarMapper;
import io.leangen.graphql.metadata.strategy.query.ResolverBuilder;
import io.leangen.graphql.module.Module;

import java.util.Arrays;
import java.util.List;

public class MicroProfileModule implements Module {

    @Override
    public void setUp(SetupContext context) {
        List<ResolverBuilder> resolverBuilders = Arrays.asList(new AnnotationResolverBuilder(), new BeanResolverBuilder());
        TemporalScalarAdapter temporalScalarAdapter = new TemporalScalarAdapter();
        context.getSchemaGenerator()
                .withTypeInfoGenerator(new TypeInfoGenerator())
                .withInclusionStrategy(new InclusionStrategy())
                .withResolverBuilders((conf, builders) -> resolverBuilders)
                .withNestedResolverBuilders((conf, builders) -> resolverBuilders)
                .withResolverInterceptors(new PartialResultsResolverInterceptor())
                .withResolverInterceptorFactories((config, factories) -> factories.append(temporalScalarAdapter))
                .withValueMapperFactory(JsonbValueMapperFactory.builder().build())
                .withSchemaTransformers(new IdTransformer(), new NonNullTransformer())
                .withOutputConverters(temporalScalarAdapter)
                .withArgumentInjectors(temporalScalarAdapter)
                .withTypeMappersPrepended((conf, mappers) -> mappers
                        .insertAfterOrAppend(IdAdapter.class, new EnumMapper(conf.javaDeprecationConfig))
                        .insertBeforeOrPrepend(ScalarMapper.class, temporalScalarAdapter)
                );
    }
}
