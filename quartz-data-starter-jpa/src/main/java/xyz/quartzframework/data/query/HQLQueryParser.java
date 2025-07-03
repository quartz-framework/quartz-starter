package xyz.quartzframework.data.query;

import lombok.val;
import xyz.quartzframework.core.bean.annotation.Injectable;
import xyz.quartzframework.core.bean.annotation.NamedInstance;
import xyz.quartzframework.data.util.ParameterBindingUtil;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Injectable
@NamedInstance("hqlQueryParser")
public class HQLQueryParser implements QueryParser {

    private static final Pattern CONDITION_PATTERN =
            Pattern.compile("(\\w+)\\s*(=|!=|<>|<|<=|>|>=|like|not like|in|not in|is null|is not null)\\s*(\\?\\d*|true|false|null|'[^']*'|\\([^)]*\\))?", Pattern.CASE_INSENSITIVE);

    @Override
    public boolean supports(Method method) {
        val a = method.getAnnotation(Query.class);
        return a != null && isInPattern(a);
    }

    private boolean isInPattern(Query query) {
        String q = query.value().trim().toLowerCase();
        if (q.startsWith("from ")) return true;
        return q.startsWith("select ") && !q.contains("select *");
    }

    @Override
    public String queryString(Method method) {
        val annotation = method.getAnnotation(Query.class);
        if (annotation == null) {
            return null;
        }
        return annotation.value();
    }

    @Override
    public DynamicQueryDefinition parse(Method method) {
        val query = queryString(method);
        val conditions = new ArrayList<Condition>();
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
            Matcher matcher = CONDITION_PATTERN.matcher(whereClause);
            while (matcher.find()) {
                String property = matcher.group(1);
                String op = matcher.group(2).toLowerCase();
                String param = matcher.group(3);

                Operation operation = switch (op) {
                    case "=" -> Operation.EQUAL;
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
                    default -> throw new IllegalArgumentException("Unsupported operation: " + op);
                };
                Object fixedValue = null;
                Integer paramIndex = null;
                String namedParameter = null;
                if (param != null) {
                    String value = param.trim().toLowerCase();
                    if (value.startsWith("?")) {
                        if (value.length() == 1) {
                            paramIndex = conditions.size();
                        } else {
                            paramIndex = Integer.parseInt(value.substring(1)) - 1;
                        }
                    } else if (value.startsWith(":")) {
                        namedParameter = value.substring(1);
                    } else if (value.equals("true")) {
                        fixedValue = Boolean.TRUE;
                    } else if (value.equals("false")) {
                        fixedValue = Boolean.FALSE;
                    } else if (value.equals("null")) {
                        fixedValue = null;
                    } else if (value.matches("'[^']*'")) {
                        fixedValue = value.substring(1, value.length() - 1);
                    } else if (value.matches("-?\\d+")) {
                        fixedValue = Integer.parseInt(value);
                    } else if (value.matches("\\(\\?\\d*\\)")) {
                        String inside = value.substring(1, value.length() - 1).trim();
                        if (inside.equals("?")) {
                            paramIndex = conditions.size();
                        } else {
                            paramIndex = Integer.parseInt(inside.substring(1)) - 1;
                        }
                    }
                    else if (value.matches("\\(([^)]+)\\)")) {
                        String inner = value.substring(1, value.length() - 1);
                        String[] items = inner.split(",");
                        List<Object> values = new ArrayList<>();
                        for (String item : items) {
                            String trimmed = item.trim();
                            if (trimmed.matches("'[^']*'")) {
                                values.add(trimmed.substring(1, trimmed.length() - 1));
                            } else if (trimmed.equalsIgnoreCase("true") || trimmed.equalsIgnoreCase("false")) {
                                values.add(Boolean.parseBoolean(trimmed));
                            } else if (trimmed.matches("-?\\d+")) {
                                values.add(Integer.parseInt(trimmed));
                            } else {
                                throw new IllegalArgumentException("Unsupported literal in collection: " + trimmed);
                            }
                        }
                        fixedValue = values;
                    } else {
                        throw new IllegalArgumentException("Unsupported literal value: " + value);
                    }
                }

                conditions.add(new Condition(property, operation, fixedValue, paramIndex, namedParameter));
            }
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
        QueryAction action = resolveActionFromReturnType(method);
        val def = new DynamicQueryDefinition(method, action, conditions, orders, limit, false, false, null);
        ParameterBindingUtil.validateNamedParameters(method, def);
        return def;
    }

    private QueryAction resolveActionFromReturnType(Method method) {
        val returnType = method.getReturnType();
        if (returnType == boolean.class || returnType == Boolean.class) {
            return QueryAction.EXISTS;
        } else if (Number.class.isAssignableFrom(returnType) || returnType == long.class || returnType == Long.class) {
            return QueryAction.COUNT;
        }
        return QueryAction.FIND;
    }
}