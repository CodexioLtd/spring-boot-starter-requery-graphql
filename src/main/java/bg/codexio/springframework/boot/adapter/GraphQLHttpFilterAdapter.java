package bg.codexio.springframework.boot.adapter;

import bg.codexio.springframework.boot.configuration.SupportsProperties;
import bg.codexio.springframework.data.jpa.requery.adapter.HttpFilterAdapter;
import bg.codexio.springframework.data.jpa.requery.payload.FilterOperation;
import bg.codexio.springframework.data.jpa.requery.payload.FilterRequest;
import bg.codexio.springframework.data.jpa.requery.payload.FilterRequestWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.language.*;
import graphql.parser.Parser;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A filter adapter that handles GraphQL requests and translates filter
 * arguments
 * into a {@link FilterRequestWrapper}. This adapter supports both simple and
 * complex filters,
 * leveraging {@link GraphQLComplexFilterAdapter} for complex filter handling.
 */
@Component
public class GraphQLHttpFilterAdapter
        implements HttpFilterAdapter {
    private static final Map<String, FilterOperation> OPERATION_MAP =
            // Mapping of GraphQL suffixes to corresponding filter operations
            Map.ofEntries(
                    Map.entry(
                            "_gt",
                            FilterOperation.GT
                    ),
                    Map.entry(
                            "_gte",
                            FilterOperation.GTE
                    ),
                    Map.entry(
                            "_lt",
                            FilterOperation.LT
                    ),
                    Map.entry(
                            "_lte",
                            FilterOperation.LTE
                    ),
                    Map.entry(
                            "_in",
                            FilterOperation.IN
                    ),
                    Map.entry(
                            "_not_in",
                            FilterOperation.NOT_IN
                    ),
                    Map.entry(
                            "_contains",
                            FilterOperation.CONTAINS
                    ),
                    Map.entry(
                            "_starts_with",
                            FilterOperation.BEGINS_WITH
                    ),
                    Map.entry(
                            "_ends_with",
                            FilterOperation.ENDS_WITH
                    ),
                    Map.entry(
                            "_empty",
                            FilterOperation.EMPTY
                    ),
                    Map.entry(
                            "_not_empty",
                            FilterOperation.NOT_EMPTY
                    ),
                    Map.entry(
                            "_begins_with_caseins",
                            FilterOperation.BEGINS_WITH_CASEINS
                    ),
                    Map.entry(
                            "_ends_with_caseins",
                            FilterOperation.ENDS_WITH_CASEINS
                    ),
                    Map.entry(
                            "_contains_caseins",
                            FilterOperation.CONTAINS_CASEINS
                    )
            );
    private final Logger logger =
            LoggerFactory.getLogger(GraphQLHttpFilterAdapter.class);
    private final ObjectMapper objectMapper;
    private final GraphQLComplexFilterAdapter graphQLComplexFilterAdapter;
    private final SupportsProperties supportsProperties;


    /**
     * Constructs a {@code GraphQLHttpFilterAdapter} with the specified
     * dependencies for JSON processing, complex GraphQL filtering, and
     * configuration support.
     *
     * @param objectMapper                the {@link ObjectMapper} for JSON
     *                                    processing
     * @param graphQLComplexFilterAdapter the adapter for handling complex
     *                                    GraphQL filters
     * @param supportsProperties          the {@link SupportsProperties} for
     *                                    configuring supported features and
     *                                    properties
     */
    public GraphQLHttpFilterAdapter(
            ObjectMapper objectMapper,
            GraphQLComplexFilterAdapter graphQLComplexFilterAdapter,
            SupportsProperties supportsProperties
    ) {
        this.objectMapper = objectMapper;
        this.graphQLComplexFilterAdapter = graphQLComplexFilterAdapter;
        this.supportsProperties = supportsProperties;
    }

    /**
     * Substitutes variables in the given query string with corresponding values
     * from a map of variables.
     *
     * <p>
     * This method iterates through each entry in the {@code variables} map,
     * replacing occurrences of each variable key (enclosed in double quotes
     * and preceded with $ sign) in
     * the {@code query} string with its corresponding value (also enclosed
     * in double
     * quotes). This is useful for dynamically injecting values into a query
     * template.
     * </p>
     *
     * @param query     the query string containing placeholders for variables
     * @param variables a map of variable names and their corresponding values
     * @return the modified query string with variables substituted by their
     * values
     */
    private static String substituteVariablesInQuery(
            String query,
            Map<String, Object> variables
    ) {
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String variablePlaceholder = entry.getKey();
            String value = entry.getValue()
                                .toString();
            query = query.replace(
                    variablePlaceholder,
                    value
            );
        }

        return query;
    }

    /**
     * Determines whether the given {@link HttpServletRequest} supports
     * GraphQL requests.
     *
     * <p>
     * This method checks if the request URL contains the "/graphql" path,
     * indicating that the request is likely related to GraphQL operations.
     * If {@code supportsProperties.isCheckBody()} is {@code true}, it also
     * checks the request body for GraphQL content.
     * </p>
     *
     * <p>
     * The result is based on the configuration in {@code supportsProperties}:
     * - If {@code isInclusive()} is {@code true}, all checks must pass.
     * - Otherwise, at least one check must pass.
     * </p>
     *
     * @param req the {@link HttpServletRequest} to evaluate
     * @return {@code true} if the request supports GraphQL based on the URL
     * and (optionally) the request body; {@code false} otherwise
     */
    @Override
    public boolean supports(HttpServletRequest req) {
        var checks = Stream.of(
                checkUrl(req),
                this.supportsProperties.shouldCheckBody()
                        && checkRequestBody(req)
        );

        return this.supportsProperties.isInclusive()
               ? checks.allMatch(Boolean::booleanValue)
               : checks.anyMatch(Boolean::booleanValue);
    }

    /**
     * Checks if the URL of the given {@link HttpServletRequest} matches
     * the configured URL pattern for GraphQL requests.
     *
     * @param req the {@link HttpServletRequest} whose URL is evaluated
     * @return {@code true} if the request URL matches the specified pattern;
     * {@code false} otherwise
     */
    private boolean checkUrl(HttpServletRequest req) {
        return req.getRequestURL()
                  .toString()
                  .matches(this.supportsProperties.getUrlPattern());
    }

    /**
     * Checks if the request body of the given {@link HttpServletRequest}
     * contains a "query" field, indicating a GraphQL request.
     *
     * <p>
     * Attempts to parse the request body into a JSON map and check for the
     * presence of the "query" key. Logs an error and returns {@code false}
     * if an {@link IOException} occurs during parsing.
     * </p>
     *
     * @param req the {@link HttpServletRequest} to evaluate
     * @return {@code true} if the request body contains a "query" field;
     * {@code false} if the field is missing or if an error occurs
     */
    private boolean checkRequestBody(HttpServletRequest req) {
        try {
            return createJsonMap(req).get("query") != null;
        } catch (IOException e) {
            this.logger.error(
                    e.getMessage(),
                    e
            );

            return false;
        }

    }

    /**
     * Adapts the filter parameters from the given {@link HttpServletRequest}
     * into a {@link FilterRequestWrapper}.
     *
     * <p>
     * This method checks the HTTP method of the request and calls the
     * appropriate processing method based on it:
     * - For GET requests, it calls {@code processGetRequest}.
     * - For POST requests, it calls {@code processPostRequest}.
     * - For other HTTP methods, it returns an empty
     * {@link FilterRequestWrapper}.
     * </p>
     *
     * @param request the {@link HttpServletRequest} containing filter
     *                parameters
     * @param <T>     the type of the result in the {@link FilterRequestWrapper}
     * @return a {@link FilterRequestWrapper} containing the adapted filter
     * requests, or an empty wrapper if the HTTP method is unsupported
     */
    @Override
    public <T> FilterRequestWrapper<T> adapt(HttpServletRequest request) {
        if (request.getMethod()
                   .equals("GET")) {
            return processGetRequest(request);
        } else if (request.getMethod()
                          .equals("POST")) {
            return processPostRequest(request);
        } else {
            return new FilterRequestWrapper<>();
        }
    }

    /**
     * Processes a GET request, extracting filter data from the query
     * parameters.
     *
     * <p>
     * This method retrieves the "query" and "variables" parameters from the
     * request, then prepares and converts them into a
     * {@link FilterRequestWrapper}
     * by calling {@code prepareAndConvertQuery}.
     * </p>
     *
     * @param request the {@link HttpServletRequest} containing filter
     *                parameters
     * @param <T>     the type of the result in the {@link FilterRequestWrapper}
     * @return a {@link FilterRequestWrapper} representing the filter,
     * or an empty wrapper if an error occurs
     */
    private <T> FilterRequestWrapper<T> processGetRequest(HttpServletRequest request) {
        try {
            var query = request.getParameter("query");
            var variables = request.getParameter("variables");

            return this.prepareAndConvertQuery(
                    query,
                    variables
            );
        } catch (Exception e) {
            this.logger.error(
                    e.getMessage(),
                    e
            );

            return new FilterRequestWrapper<>();
        }
    }

    /**
     * Processes a POST request, extracting filter data from the request body.
     *
     * <p>
     * This method parses the request body into a JSON map to retrieve the
     * "query" and "variables" fields. It then prepares and converts these into
     * a {@link FilterRequestWrapper} by calling {@code prepareAndConvertQuery}.
     * </p>
     *
     * @param request the {@link HttpServletRequest} containing filter data
     *                in the request body
     * @param <T>     the type of the result in the {@link FilterRequestWrapper}
     * @return a {@link FilterRequestWrapper} representing the filter,
     * or an empty wrapper if an error occurs
     */
    private <T> FilterRequestWrapper<T> processPostRequest(HttpServletRequest request) {
        try {
            var jsonMap = createJsonMap(request);

            var query = (String) jsonMap.get("query");
            var variables = (String) jsonMap.get("variables");

            return this.prepareAndConvertQuery(
                    query,
                    variables
            );

        } catch (Exception e) {
            this.logger.error(
                    e.getMessage(),
                    e
            );
            return new FilterRequestWrapper<>();
        }
    }

    /**
     * Prepares and converts a GraphQL query into a
     * {@link FilterRequestWrapper}.
     *
     * <p>
     * This method substitutes variables into the query if provided, extracts
     * the filter data from the query, and adapts it into a
     * {@link FilterRequestWrapper}.
     * If the query contains complex filters, it uses {@code
     * graphQLComplexFilterAdapter}
     * to handle the adaptation.
     * </p>
     *
     * @param query     the GraphQL query string to process
     * @param variables the JSON string of variables to substitute into the
     *                  query
     * @param <T>       the type of the result in the
     *                  {@link FilterRequestWrapper}
     * @return a {@link FilterRequestWrapper} containing the adapted filter,
     * or an empty wrapper if an error occurs or if no filter is found
     */
    private <T> FilterRequestWrapper<T> prepareAndConvertQuery(
            String query,
            String variables
    ) {
        try {
            var computedQuery = substituteVariablesInQuery(
                    query,
                    createVariablesMap(variables)
            );

            return extractFilterBody(computedQuery).map(this.graphQLComplexFilterAdapter::<T>adapt)
                                                   .orElseGet(() -> new FilterRequestWrapper<T>(parseGraphQLQuery(computedQuery)));
        } catch (Exception e) {
            this.logger.error(
                    e.getMessage(),
                    e
            );
            return new FilterRequestWrapper<>();
        }
    }

    /**
     * Creates a map of variables from a JSON string.
     *
     * <p>
     * This method parses the given JSON string into a {@link Map} using the
     * configured {@link ObjectMapper}. The JSON string should represent a valid
     * JSON object containing variable key-value pairs.
     * </p>
     *
     * @param variables the JSON string representing variables as key-value
     *                  pairs
     * @return a {@link Map} of variable names and their corresponding values
     * @throws JsonProcessingException if an error occurs while parsing the
     *                                 JSON string
     */
    private Map<String, Object> createVariablesMap(String variables)
            throws JsonProcessingException {
        if (variables == null) {
            return new HashMap<>();
        }
        return this.objectMapper.readValue(
                variables,
                HashMap.class
        );
    }

    /**
     * Creates a map from the JSON content in the given
     * {@link HttpServletRequest}.
     *
     * <p>
     * This method reads the request body, parsing it as JSON and converting it
     * into a {@link Map} using the configured {@link ObjectMapper}. If the
     * {@code ContentCachingRequestWrapper} has an empty content cache, it reads
     * directly from the request's reader instead.
     * </p>
     *
     * @param request the {@link HttpServletRequest} containing JSON data in
     *                the body
     * @return a {@link Map} representing the parsed JSON content
     * @throws IOException if an error occurs while reading or parsing the
     *                     request body
     */
    private Map<String, Object> createJsonMap(HttpServletRequest request)
            throws IOException {
        var requestWrapper = (ContentCachingRequestWrapper) request;

        if (requestWrapper.getContentAsString()
                          .isEmpty()) {
            return this.objectMapper.readValue(
                    requestWrapper.getReader()
                           .lines()
                           .collect(Collectors.joining(System.lineSeparator())),
                    new TypeReference<>() {}
            );
        }

        return this.objectMapper.readValue(
                requestWrapper.getContentAsString(),
                new TypeReference<>() {}
        );
    }

    /**
     * Parses a GraphQL query and converts it into a list of
     * {@link FilterRequest}.
     *
     * <p>
     * This method uses a {@link Parser} to parse the given GraphQL query
     * string into a document representation and then processes the document
     * to extract filter requests.
     * </p>
     *
     * @param query the GraphQL query string
     * @return a list of {@link FilterRequest} extracted from the query
     */
    private List<FilterRequest> parseGraphQLQuery(String query) {
        var parser = new Parser();
        var document = parser.parseDocument(query);

        return processDocument(document);
    }

    /**
     * Processes the GraphQL document, extracting filter arguments into
     * {@link FilterRequest}s.
     *
     * <p>
     * This method iterates through the definitions in the provided GraphQL
     * {@link Document}, targeting only query operations. For each query field,
     * it processes the arguments to extract filter criteria, converting each
     * argument into a {@link FilterRequest}. If an argument's value is a map,
     * it delegates to {@code handleMapValue} for further processing.
     * </p>
     *
     * @param document the GraphQL {@link Document} containing query definitions
     * @return a list of {@link FilterRequest} extracted from the document
     */
    private List<FilterRequest> processDocument(
            Document document
    ) {
        var filterRequests = new ArrayList<FilterRequest>();
        var operationDefinitionList =
                document.getDefinitionsOfType(OperationDefinition.class);
        for (var operation : operationDefinitionList) {
            if (!operation.getOperation()
                          .equals(OperationDefinition.Operation.QUERY)) {
                continue;
            }
            for (var field : operation.getSelectionSet()
                                      .getSelectionsOfType(Field.class)) {
                for (var argument : field.getArguments()) {
                    var value = argument.getValue();
                    var extractedValue = extractValue(value);
                    if (extractedValue instanceof Map) {
                        handleMapValue(
                                argument.getName(),
                                extractedValue,
                                filterRequests
                        );
                        continue;
                    }
                    var filterRequest = new FilterRequest(
                            extractName(argument.getName()),
                            extractedValue,
                            getOperationFromArgument(argument.getName())
                    );
                    filterRequests.add(filterRequest);
                }
            }
        }
        return filterRequests;
    }

    /**
     * Recursively handles a map value, extracting nested values and creating
     * {@link FilterRequest} objects for each key-value pair.
     *
     * <p>
     * This method processes each entry in the provided map, extracting filter
     * criteria and adding them to the {@code filterRequests} list. If an
     * entry's
     * value is itself a map, the method calls itself recursively to handle
     * nested structures, constructing the {@link FilterRequest} name by
     * appending
     * keys with dot notation.
     * </p>
     *
     * @param containingObjectName the name of the parent object containing
     *                             this map
     * @param extractedValue       the map value extracted, expected to be
     *                             another map
     * @param filterRequests       the list of {@link FilterRequest} objects
     *                             to populate
     */
    private void handleMapValue(
            String containingObjectName,
            Object extractedValue,
            List<FilterRequest> filterRequests
    ) {
        if (!(extractedValue instanceof Map)) {
            return;
        }

        for (var entry : ((Map<String, Object>) extractedValue).entrySet()) {
            var nestedValue = (Value<?>) entry.getValue();
            var nestedExtractedValue = extractValue(nestedValue);
            if (nestedExtractedValue instanceof Map) {
                handleMapValue(
                        entry.getKey(),
                        nestedExtractedValue,
                        filterRequests
                );
            }
            var filterRequest = new FilterRequest(
                    containingObjectName.concat(".")
                                        .concat(extractName(entry.getKey())),
                    nestedExtractedValue,
                    getOperationFromArgument(entry.getKey())
            );
            filterRequests.add(filterRequest);
        }
    }

    /**
     * Extracts the base name of a field by removing the operation suffix
     * (e.g., "_gt", "_lte") using the {@code OPERATION_MAP}.
     *
     * <p>
     * This method checks if the provided field name ends with any suffix
     * defined
     * in {@code OPERATION_MAP}. If a match is found, the suffix is removed, and
     * the base field name is returned. If no suffix matches, the original field
     * name is returned unchanged.
     * </p>
     *
     * @param name the field name potentially containing an operation suffix
     * @return the base field name without the operation suffix, or the original
     * name if no suffix is matched
     */
    private String extractName(String name) {
        return OPERATION_MAP.keySet()
                            .stream()
                            .filter(name::endsWith)
                            .findFirst()
                            .map(suffix -> name.substring(
                                    0,
                                    name.length() - suffix.length()
                            ))
                            .orElse(name);
    }

    /**
     * Extracts the underlying value from a GraphQL {@link Value} object
     * depending on
     * its specific type.
     *
     * <p>
     * This method supports various GraphQL value types, including
     * {@link StringValue},
     * {@link IntValue}, {@link BooleanValue}, {@link FloatValue},
     * {@link EnumValue},
     * {@link ObjectValue}, and {@link ArrayValue}. Each type is converted to
     * its
     * corresponding Java representation:
     * - {@link StringValue}, {@link IntValue}, {@link BooleanValue}, and
     * {@link FloatValue}
     * are returned as their native Java types.
     * - {@link EnumValue} returns the name of the enum.
     * - {@link ObjectValue} is processed with {@code handleComplexObject} to
     * handle nested objects.
     * - {@link ArrayValue} is mapped to a list of extracted values.
     * </p>
     *
     * @param value the {@link Value} object to extract from
     * @return the corresponding Java object representation of the value, or
     * {@code null}
     * if the type is unsupported
     */
    private Object extractValue(Value<?> value) {
        return switch (value) {
            case StringValue sv -> sv.getValue();
            case IntValue iv -> iv.getValue();
            case BooleanValue bv -> bv.isValue();
            case FloatValue fv -> fv.getValue();
            case EnumValue ev -> ev.getName();
            case ObjectValue ov -> handleComplexObject(ov);
            case ArrayValue av -> av.getValues()
                                    .stream()
                                    .map(this::extractValue)
                                    .toList();
            default -> null;
        };
    }

    /**
     * Processes a complex {@link ObjectValue}, extracting each field's name
     * and value into a {@link Map}. This method is used to handle
     * object-structured
     * values within GraphQL queries.
     *
     * <p>
     * This method iterates over the fields of the provided {@link ObjectValue},
     * collecting each field's name and its corresponding value into a map.
     * </p>
     *
     * @param value the {@link ObjectValue} to extract fields from
     * @return a map of field names to their corresponding values
     */
    private Map<String, Object> handleComplexObject(ObjectValue value) {
        return value.getObjectFields()
                    .stream()
                    .collect(Collectors.toMap(
                            ObjectField::getName,
                            ObjectField::getValue
                    ));
    }

    /**
     * Determines the filter operation (e.g., EQ, GT, LTE) based on the
     * argument's suffix.
     *
     * <p>
     * This method analyzes the suffix of the provided argument name to find a
     * matching operation in {@code OPERATION_MAP}. If a matching suffix is
     * found,
     * the corresponding {@link FilterOperation} is returned. If no suffix
     * matches,
     * it defaults to {@link FilterOperation#EQ}.
     * </p>
     *
     * @param argumentName the argument name to analyze for an operation suffix
     * @return the corresponding {@link FilterOperation}, or
     * {@link FilterOperation#EQ} if no matching suffix is found
     */
    private FilterOperation getOperationFromArgument(String argumentName) {
        return OPERATION_MAP.entrySet()
                            .stream()
                            .filter(entry -> argumentName.endsWith(entry.getKey()))
                            .findFirst()
                            .map(Map.Entry::getValue)
                            .orElse(FilterOperation.EQ);
    }

    /**
     * Extracts the complex filter body from a GraphQL query string, accounting
     * for nested structures and escaping within the filter argument.
     *
     * <p>
     * This method uses a regular expression to locate the "filter" argument
     * within a GraphQL query string and extracts its content, which may include
     * nested fields and values. The extraction process is designed to handle
     * complex JSON-like structures with nested braces, quotes, and escape
     * characters
     * to accurately capture the full filter body for further processing.
     * </p>
     *
     * @param query the full GraphQL query string containing filter criteria
     * @return an {@link Optional} containing the extracted filter body as a
     * string, or an empty {@link Optional} if no filter is found
     */
    private Optional<String> extractFilterBody(String query) {
        // Use regex to extract the contents of the filter argument
        var pattern = Pattern.compile(
                "filter\\s*:\\s*(\\{.*\\})",
                Pattern.DOTALL
        );
        var matcher = pattern.matcher(query);

        if (!matcher.find()) {
            return Optional.empty();
        }
        query = matcher.group(1);
        var filter = new StringBuilder();
        var quoteCounter = 0;
        var braceCounter = 0;
        var started = false;
        for (var i = 0; i < query.length(); i++) {
            if (started && quoteCounter == 0 && braceCounter == 0) {
                break;
            }

            filter.append(query.charAt(i));

            if (query.charAt(i) == '"' && query.charAt(i - 1) != '\\'
                    && quoteCounter > 0) {
                started = true;
                quoteCounter--;
                continue;
            }

            if (query.charAt(i) == '"' && query.charAt(i - 1) != '\\'
                    && quoteCounter == 0) {
                started = true;
                quoteCounter++;
                continue;
            }

            if (query.charAt(i) == '{' && quoteCounter == 0) {
                started = true;
                braceCounter++;
                continue;
            }

            if (query.charAt(i) == '}' && quoteCounter == 0) {
                started = true;
                braceCounter--;
            }
        }
        return Optional.of(filter.toString());
    }
}
