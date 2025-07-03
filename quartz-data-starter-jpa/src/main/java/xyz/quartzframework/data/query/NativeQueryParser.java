package xyz.quartzframework.data.query;

import lombok.val;
import xyz.quartzframework.core.bean.annotation.Injectable;
import xyz.quartzframework.core.bean.annotation.NamedInstance;

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
    public DynamicQueryDefinition parse(Method method) {
        val raw = queryString(method);
        val conditions = new ArrayList<Condition>();
        val namedMatcher = Pattern.compile(":(\\w+)").matcher(raw);
        while (namedMatcher.find()) {
            String name = namedMatcher.group(1);
            conditions.add(new Condition(null, Operation.EQUAL, null, null, name));
        }
        val posMatcher = Pattern.compile("\\?(\\d*)").matcher(raw);
        while (posMatcher.find()) {
            String indexStr = posMatcher.group(1);
            Integer paramIndex = indexStr.isEmpty() ? conditions.size() : Integer.parseInt(indexStr) - 1;
            conditions.add(new Condition(null, Operation.EQUAL, null, paramIndex, null));
        }
        return new DynamicQueryDefinition(
                method,
                resolveActionFromReturnType(method),
                conditions,
                List.of(),
                null,
                false,
                true,
                raw
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