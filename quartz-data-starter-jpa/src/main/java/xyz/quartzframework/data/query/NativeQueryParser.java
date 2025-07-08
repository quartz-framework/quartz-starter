package xyz.quartzframework.data.query;

import lombok.val;
import xyz.quartzframework.core.bean.annotation.Injectable;
import xyz.quartzframework.core.bean.annotation.NamedInstance;
import xyz.quartzframework.data.storage.StorageDefinition;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Injectable
@NamedInstance("nativeQueryParser")
public class NativeQueryParser implements QueryParser {

    @Override
    public boolean supports(Method method) {
        val a = method.getAnnotation(Query.class);
        return a != null && isInPattern(a);
    }

    private boolean isInPattern(Query query) {
        return query.nativeQuery();
    }

    @Override
    public String queryString(Method method) {
        val annotation = method.getAnnotation(Query.class);
        return annotation != null ? annotation.value() : null;
    }

    @Override
    public DynamicQueryDefinition parse(Method method, StorageDefinition storageDefinition) {
        val raw = queryString(method);
        val conditions = new ArrayList<QueryCondition>();

        val namedMatcher = Pattern.compile(":(\\w+)").matcher(raw);
        while (namedMatcher.find()) {
            String name = namedMatcher.group(1);
            val attribute = new AttributePath(name, name, CaseFunction.NONE);
            conditions.add(new QueryCondition(
                    ":" + name,
                    attribute,
                    Operation.EQUAL,
                    null,
                    null,
                    name,
                    ":" + name,
                    false
            ));
        }
        val posMatcher = Pattern.compile("\\?(\\d*)").matcher(raw);
        while (posMatcher.find()) {
            String indexStr = posMatcher.group(1);
            Integer paramIndex = indexStr.isEmpty() ? conditions.size() : Integer.parseInt(indexStr) - 1;
            val attribute = new AttributePath("?", "param" + paramIndex, CaseFunction.NONE);
            conditions.add(new QueryCondition(
                    "?" + (indexStr.isEmpty() ? "" : indexStr),
                    attribute,
                    Operation.EQUAL,
                    null,
                    paramIndex,
                    null,
                    "?" + (indexStr.isEmpty() ? "" : indexStr),
                    false
            ));
        }
        Integer limit = null;
        val limitMatcher = Pattern.compile("\\blimit\\s+(\\d+)", Pattern.CASE_INSENSITIVE).matcher(raw);
        if (limitMatcher.find()) {
            limit = Integer.parseInt(limitMatcher.group(1));
        }
        Class<?> returnType = storageDefinition.entityClass();
        if (raw.toLowerCase().startsWith("select") && !raw.contains("*")) {
            returnType = Object[].class;
        }
        return new DynamicQueryDefinition(
                method,
                resolveActionFromReturnType(method),
                conditions,
                List.of(),
                limit,
                raw.toLowerCase().contains("distinct"),
                true,
                raw,
                returnType,
                null
        );
    }

    private QueryAction resolveActionFromReturnType(Method method) {
        val returnType = method.getReturnType();
        if (returnType == boolean.class || returnType == Boolean.class) return QueryAction.EXISTS;
        if (returnType == long.class || returnType == Long.class ||
                returnType == int.class || returnType == Integer.class ||
                Number.class.isAssignableFrom(returnType)) return QueryAction.COUNT;
        return QueryAction.FIND;
    }
}