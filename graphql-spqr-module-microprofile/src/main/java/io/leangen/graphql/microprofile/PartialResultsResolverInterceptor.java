package io.leangen.graphql.microprofile;

import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.execution.DataFetcherResult;
import io.leangen.graphql.execution.InvocationContext;
import io.leangen.graphql.execution.ResolverInterceptor;
import org.eclipse.microprofile.graphql.GraphQLException;
/**
 * Used to handle instances of <code>GraphQLException</code> that contain partial results.
 */
public class PartialResultsResolverInterceptor implements ResolverInterceptor {

    @Override
    public Object aroundInvoke(InvocationContext context, Continuation continuation) throws Exception {
        try {
            return continuation.proceed(context);
        } catch (GraphQLException e) {
            Object partialResults = e.getPartialResults();
            if (partialResults == null) {
                throw e;
            }
            GraphQLError error = GraphqlErrorBuilder
                    .newError(context.getResolutionEnvironment().dataFetchingEnvironment)
                    .errorType(fromExceptionType(e.getExceptionType()))
                    .message(e.getMessage())
                    .build();

            return DataFetcherResult.newResult()
                    .data(partialResults)
                    .error(error)
                    .build();
        }
    }

    private static ErrorType fromExceptionType(GraphQLException.ExceptionType exType) {
        if (exType != null) {
            switch (exType) {
                case DataFetchingException: return ErrorType.DataFetchingException;
                case OperationNotSupported: return ErrorType.OperationNotSupported;
                case ExecutionAborted:      return ErrorType.ExecutionAborted;
            }
        }
        return ErrorType.DataFetchingException;
    }
}

