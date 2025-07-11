package xyz.quartzframework.data.query;

import lombok.val;
import xyz.quartzframework.core.bean.annotation.Injectable;
import xyz.quartzframework.core.bean.annotation.NamedInstance;
import xyz.quartzframework.data.storage.StorageDefinition;
import xyz.quartzframework.data.util.ParameterBindingUtil;

import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Injectable
@NamedInstance("hqlQueryParser")
public class HQLQueryParser implements QueryParser {

    @Override
    public boolean supports(Method method) {
        val a = method.getAnnotation(Query.class);
        return a != null && isInPattern(a);
    }

    private boolean isInPattern(Query query) {
        String q = query.value().trim().toLowerCase();
        if (q.startsWith("from ")) return true;
        return q.startsWith("select ") && !q.contains("select *") && !query.nativeQuery();
    }

    @Override
    public String queryString(Method method) {
        val annotation = method.getAnnotation(Query.class);
        return annotation != null ? annotation.value() : null;
    }

    @Override
    public DynamicQueryDefinition parse(Method method, StorageDefinition storageDefinition) {
        val query = queryString(method);
        Set<String> aliases = extractAliases(query);
        val conditions = new ArrayList<QueryCondition>();
        val substitutions = new ArrayList<QuerySubstitution>();
        val orders = new ArrayList<Order>();
        Integer limit = null;

        val whereStart = query.toLowerCase().indexOf("where");
        val orderStart = query.toLowerCase().indexOf("order by");
        String whereClause = null;

        if (whereStart >= 0) {
            whereClause = (orderStart > whereStart)
                    ? query.substring(whereStart + 5, orderStart).trim()
                    : query.substring(whereStart + 5).trim();
        }

        if (whereClause != null) {
            val queryConditions = parseConditions(whereClause, aliases, substitutions);
            conditions.addAll(queryConditions);
        }

        if (orderStart >= 0) {
            String orderPart = query.substring(orderStart + "order by".length()).trim();
            String[] orderTokens = orderPart.split(",");
            for (String token : orderTokens) {
                String[] parts = token.trim().split("\\s+");
                String property = parts[0];
                boolean desc = parts.length > 1 && parts[1].equalsIgnoreCase("desc");
                orders.add(new Order(property, desc));
            }
        }

        int limitStart = query.toLowerCase().lastIndexOf("limit ");
        if (limitStart >= 0) {
            try {
                String limitValue = query.substring(limitStart + 6).trim().split("\\s+")[0];
                limit = Integer.parseInt(limitValue);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid LIMIT value in query: " + query, e);
            }
        }

        QueryAction action = resolveActionFromReturnType(method, query);
        Class<?> returnType;

        Matcher selectNew = Pattern.compile("(?i)select\\s+new\\s+([\\w.]+)\\s*\\(").matcher(query);
        if (selectNew.find()) {
            try {
                returnType = Class.forName(selectNew.group(1));
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Invalid projection class: " + selectNew.group(1), e);
            }
        } else if (query.toLowerCase().matches("select\\s+\\w+(\\s*,\\s*\\w+)+.*")) {
            returnType = Object[].class;
        } else if (query.toLowerCase().startsWith("select") && !query.contains("new") && query.contains(",")) {
            returnType = Object[].class;
        } else {
            returnType = storageDefinition.entityClass();
        }

        boolean isDistinct = query.toLowerCase().contains("distinct");

        val def = new DynamicQueryDefinition(
                method,
                action,
                substitutions,
                conditions,
                orders,
                limit,
                isDistinct,
                false,
                query,
                returnType,
                null
        );

        ParameterBindingUtil.validateNamedParameters(method, def);
        return def;
    }

    private List<QueryCondition> parseConditions(String whereClause, Set<String> aliases, List<QuerySubstitution> substitutions) {
        List<QueryCondition> conditions = new ArrayList<>();
        String[] tokens = whereClause.split("(?i)\\s+(and|or)\\s+");
        Matcher connectorMatcher = Pattern.compile("(?i)\\s+(and|or)\\s+").matcher(whereClause);
        List<String> connectors = new ArrayList<>();
        while (connectorMatcher.find()) {
            connectors.add(connectorMatcher.group(1).toLowerCase());
        }

        boolean lastWasOr = false;
        for (int i = 0; i < tokens.length; i++) {
            String expr = tokens[i].trim();
            try {
                QueryCondition cond = parseSingleCondition(expr, aliases, substitutions);
                cond.setOr(lastWasOr);
                conditions.add(cond);
            } catch (IllegalArgumentException e) {
                tryAddFallbackSubstitution(expr, substitutions);
            }
            if (i < connectors.size()) {
                lastWasOr = connectors.get(i).equalsIgnoreCase("or");
            }
        }
        return conditions;
    }

    private void tryAddFallbackSubstitution(String expr, List<QuerySubstitution> substitutions) {
        Matcher fallbackMatcher = Pattern.compile("(?i)(:\\w+|\\?\\d*|\\?)").matcher(expr);
        while (fallbackMatcher.find()) {
            String token = fallbackMatcher.group(1);
            if (token.startsWith(":")) {
                substitutions.add(QuerySubstitution.named(token.substring(1), token));
            } else if (token.startsWith("?")) {
                String idx = token.length() == 1 ? "0" : String.valueOf(Integer.parseInt(token.substring(1)) - 1);
                substitutions.add(QuerySubstitution.positional(idx, token));
            }
        }
    }

    private QueryCondition parseSingleCondition(String expr, Set<String> aliases, List<QuerySubstitution> substitutions) {
        Pattern condPattern = Pattern.compile(
                "(lower\\([\\w.]+\\)|upper\\([\\w.]+\\)|[\\w.]+)\\s*" +
                        "(not like|not in|is not null|is null|>=|<=|!=|<>|=|>|<|like|in)\\s*" +
                        "(lower\\([^)]*\\)|upper\\([^)]*\\)|:\\w+|\\?\\d*|\\?|true|false|null|'[^']*'|\\(([^)]+)?\\))?",
                Pattern.CASE_INSENSITIVE
        );

        Matcher m = condPattern.matcher(expr);
        if (!m.find()) throw new IllegalArgumentException("Invalid condition expression: " + expr);

        String rawCondition = m.group(0).trim();
        String rawField = m.group(1);
        String operator = m.group(2).toLowerCase();
        String rawValue = m.group(3) != null ? m.group(3).trim() : null;

        String fieldName = normalizeField(extractInner(rawField), aliases);
        String fieldFunc = extractCaseFunction(rawField);
        String valueFunc = extractCaseFunction(rawValue);

        CaseFunction fieldCase = toCaseFunction(fieldFunc);
        CaseFunction valueCase = toCaseFunction(valueFunc);
        boolean ignoreCase = fieldCase != CaseFunction.NONE && fieldCase == valueCase;

        AttributePath attribute = new AttributePath(rawField, fieldName, fieldCase);

        Operation op = switch (operator) {
            case "=", "==" -> Operation.EQUAL;
            case "!=", "<>" -> Operation.NOT_EQUAL;
            case ">" -> Operation.GREATER_THAN;
            case ">=" -> Operation.GREATER_THAN_OR_EQUAL;
            case "<" -> Operation.LESS_THAN;
            case "<=" -> Operation.LESS_THAN_OR_EQUAL;
            case "like" -> Operation.LIKE;
            case "not like" -> Operation.NOT_LIKE;
            case "in" -> Operation.IN;
            case "not in" -> Operation.NOT_IN;
            case "is null" -> Operation.IS_NULL;
            case "is not null" -> Operation.IS_NOT_NULL;
            default -> throw new IllegalArgumentException("Unknown operator: " + operator);
        };

        boolean expectsValue = switch (op) {
            case IS_NULL, IS_NOT_NULL -> false;
            default -> true;
        };

        if (expectsValue && rawValue != null) {
            String val = extractInner(rawValue);
            if (val.startsWith("?")) {
                String idx = val.length() == 1 ? "0" : String.valueOf(Integer.parseInt(val.substring(1)) - 1);
                substitutions.add(QuerySubstitution.positional(idx, rawValue));
            } else if (val.startsWith(":")) {
                substitutions.add(QuerySubstitution.named(val.substring(1), rawValue));
            } else if (rawValue.equalsIgnoreCase("true")) {
                substitutions.add(QuerySubstitution.literal(true, rawValue));
            } else if (rawValue.equalsIgnoreCase("false")) {
                substitutions.add(QuerySubstitution.literal(false, rawValue));
            } else if (rawValue.equalsIgnoreCase("null")) {
                substitutions.add(QuerySubstitution.literal(null, rawValue));
            } else if (rawValue.startsWith("'") && rawValue.endsWith("'")) {
                substitutions.add(QuerySubstitution.literal(rawValue.substring(1, rawValue.length() - 1), rawValue));
            } else if (rawValue.startsWith("(") && rawValue.endsWith(")")) {
                String inner = rawValue.substring(1, rawValue.length() - 1).trim();
                if (inner.startsWith("?")) {
                    String idx = inner.length() == 1 ? "0" : String.valueOf(Integer.parseInt(inner.substring(1)) - 1);
                    substitutions.add(QuerySubstitution.positional(idx, rawValue));
                } else if (inner.startsWith(":")) {
                    substitutions.add(QuerySubstitution.named(inner.substring(1), rawValue));
                } else {
                    throw new IllegalArgumentException("Unsupported IN/NOT IN value: " + rawValue);
                }
            } else {
                throw new IllegalArgumentException("Unsupported value literal: " + rawValue);
            }
        }

        return new QueryCondition(
                rawCondition,
                attribute,
                op,
                rawValue,
                ignoreCase
        );
    }

    private String extractCaseFunction(String expr) {
        if (expr == null) return null;
        Matcher m = Pattern.compile("(?i)(lower|upper)\\(.*\\)").matcher(expr);
        return m.matches() ? m.group(1).toLowerCase() : null;
    }

    private String extractInner(String expr) {
        if (expr == null) return null;
        Matcher m = Pattern.compile("(?i)(?:lower|upper)\\((.+)\\)").matcher(expr);
        return m.matches() ? m.group(1).trim() : expr.trim();
    }

    private CaseFunction toCaseFunction(String func) {
        if (func == null) return CaseFunction.NONE;
        return switch (func.toLowerCase()) {
            case "lower" -> CaseFunction.LOWER;
            case "upper" -> CaseFunction.UPPER;
            default -> CaseFunction.NONE;
        };
    }

    private String normalizeField(String name, Set<String> aliases) {
        if (name == null) return null;

        int dot = name.indexOf('.');
        if (dot > 0) {
            String prefix = name.substring(0, dot);
            String suffix = name.substring(dot + 1);
            if (aliases.contains(prefix)) {
                return prefix + "." + suffix;
            }
        }
        if (name.contains("_")) return toCamelCase(name);
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private Set<String> extractAliases(String query) {
        Set<String> aliases = new HashSet<>();
        Pattern aliasPattern = Pattern.compile(
                "(?i)(from|join)\\s+[\\w.]+\\s+(?:as\\s+)?(\\w+)"
        );
        Matcher matcher = aliasPattern.matcher(query);
        while (matcher.find()) {
            aliases.add(matcher.group(2));
        }
        return aliases;
    }

    private String toCamelCase(String input) {
        StringBuilder result = new StringBuilder();
        String[] parts = input.split("_");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == 0) result.append(part.toLowerCase());
            else result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase());
        }
        return result.toString();
    }

    private QueryAction resolveActionFromReturnType(Method method, String query) {
        val returnType = method.getReturnType();
        String q = query.toLowerCase().trim();

        if (returnType == boolean.class || returnType == Boolean.class) {
            return QueryAction.EXISTS;
        }

        if ((returnType == long.class || returnType == Long.class || Number.class.isAssignableFrom(returnType))
                && q.contains("count(")) {
            return QueryAction.COUNT;
        }

        return QueryAction.FIND;
    }
}