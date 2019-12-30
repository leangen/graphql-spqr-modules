package io.leangen.graphql.microprofile;

public class BeanResolverBuilder extends io.leangen.graphql.metadata.strategy.query.BeanResolverBuilder {

    public BeanResolverBuilder(String... basePackages) {
        super(basePackages);
        this.operationInfoGenerator = new OperationInfoGenerator();
        this.argumentBuilder = new ArgumentBuilder();
        this.propertyElementReducer = AnnotationResolverBuilder::annotatedElementReducer;
    }
}
