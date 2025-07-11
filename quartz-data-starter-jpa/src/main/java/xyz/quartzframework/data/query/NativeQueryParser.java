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
        val substitutions = new ArrayList<QuerySubstitution>();

        // Named parameters: :name
        val namedMatcher = Pattern.compile(":(\\w+)").matcher(raw);
        while (namedMatcher.find()) {
            String name = namedMatcher.group(1);
            substitutions.add(QuerySubstitution.named(name, ":" + name));
        }

        // Positional parameters: ?, ?1, ?2, etc.
        val posMatcher = Pattern.compile("\\?(\\d*)").matcher(raw);
        while (posMatcher.find()) {
            String indexStr = posMatcher.group(1);
            String rawToken = posMatcher.group(0);
            String key = indexStr.isEmpty() ? "0" : String.valueOf(Integer.parseInt(indexStr) - 1);
            substitutions.add(QuerySubstitution.positional(key, rawToken));
        }

        // Limit
        Integer limit = null;
        val limitMatcher = Pattern.compile("\\blimit\\s+(\\d+)", Pattern.CASE_INSENSITIVE).matcher(raw);
        if (limitMatcher.find()) {
            limit = Integer.parseInt(limitMatcher.group(1));
        }

        // Return type inference
        Class<?> returnType = storageDefinition.entityClass();
        if (raw.toLowerCase().startsWith("select") && !raw.contains("*")) {
            returnType = Object[].class;
        }

        return new DynamicQueryDefinition(
                method,
                resolveActionFromReturnType(method),
                substitutions,
                List.of(),
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