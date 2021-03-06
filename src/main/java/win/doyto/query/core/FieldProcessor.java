package win.doyto.query.core;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import win.doyto.query.annotation.NestedQueries;
import win.doyto.query.annotation.NestedQuery;
import win.doyto.query.annotation.QueryField;
import win.doyto.query.annotation.QueryTableAlias;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import static win.doyto.query.core.Constant.*;

/**
 * FieldProcessor
 *
 * @author f0rb on 2019-06-04
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class FieldProcessor {

    private static final Map<Field, Processor> FIELD_PROCESSOR_MAP = new ConcurrentHashMap<>();
    public static final Processor EMPTY_PROCESSOR = ((argList, value) -> EMPTY);

    public static String execute(Field field, List<Object> argList, Object value) {
        return FIELD_PROCESSOR_MAP.get(field).process(argList, value);
    }

    public static void init(Field field) {
        Processor processor;
        if (field.isAnnotationPresent(QueryTableAlias.class)) {
            processor = initFieldAnnotatedByQueryTableAlias(field);
        } else if (field.isAnnotationPresent(QueryField.class)) {
            processor = initFieldAnnotatedByQueryField(field);
        } else if (field.isAnnotationPresent(NestedQueries.class)) {
            processor = initFieldAnnotatedByNestedQueries(field);
        } else {
            processor = initCommonField(field);
        }
        FIELD_PROCESSOR_MAP.put(field, processor);
    }

    private static Processor initCommonField(Field field) {
        String fieldName = field.getName();
        return chooseProcessorForFieldWithOr(fieldName);
    }

    private static Processor initFieldAnnotatedByQueryTableAlias(Field field) {
        String fieldName = field.getName();
        String tableAlias = field.getAnnotation(QueryTableAlias.class).value();
        String fieldNameWithAlias = tableAlias + "." + fieldName;
        return chooseProcessorForFieldWithOr(fieldNameWithAlias);
    }

    private static Processor chooseProcessorForFieldWithOr(String fieldName) {
        if (CommonUtil.containsOr(fieldName)) {
            return (argList, value) -> QuerySuffix.buildConditionForFieldContainsOr(fieldName, argList, value);
        } else {
            return (argList, value) -> QuerySuffix.buildConditionForField(fieldName, argList, value);
        }
    }

    private static Processor initFieldAnnotatedByQueryField(Field field) {
        String andSQL = field.getAnnotation(QueryField.class).and();
        int holderCount = StringUtils.countMatches(andSQL, PLACE_HOLDER);
        return (argList, value) -> {
            for (int i = 0; i < holderCount; i++) {
                argList.add(value);
            }
            return andSQL;
        };
    }

    private static Processor initFieldAnnotatedByNestedQueries(Field field) {
        NestedQueries nestedQueries = field.getAnnotation(NestedQueries.class);
        Processor processor = chooseProcessorForNestedQuery(field);
        return (argList, value) -> resolvedNestedQueries(argList, value, nestedQueries, processor);
    }

    protected static Processor chooseProcessorForNestedQuery(Field field) {
        Processor processor;
        Class<?> fieldType = field.getType();
        if (boolean.class.isAssignableFrom(fieldType)) {
            processor = EMPTY_PROCESSOR;
        } else if (PageQuery.class.isAssignableFrom(fieldType)) {
            processor = (argList, value) -> BuildHelper.buildWhere((PageQuery) value, argList);
        } else {
            String fieldName = field.getName();
            if (CommonUtil.containsOr(fieldName)) {
                processor = (argList, value) -> WHERE + QuerySuffix.buildConditionForFieldContainsOr(fieldName, argList, value);
            } else {
                processor = (argList, value) -> WHERE + QuerySuffix.buildConditionForField(fieldName, argList, value);
            }
        }
        return processor;
    }

    protected static String resolvedNestedQueries(List<Object> argList, Object value, NestedQueries nestedQueries, Processor processor) {
        StringBuilder subquery = resolvedNestedQueries(nestedQueries);
        IntStream.range(0, StringUtils.countMatches(subquery, PLACE_HOLDER)).mapToObj(i -> value).forEach(argList::add);
        if (nestedQueries.appendWhere()) {
            subquery.append(processor.process(argList, value));
        }
        return nestedQueries.column() + subquery + StringUtils.repeat(')', nestedQueries.value().length);
    }

    private static StringBuilder resolvedNestedQueries(NestedQueries nestedQueries) {
        StringBuilder nestedQueryBuilder = new StringBuilder();
        String lastOp = nestedQueries.op();
        String lastWhere = nestedQueries.column();
        NestedQuery[] nestedQueryArr = nestedQueries.value();

        for (int i = 0; i < nestedQueryArr.length; i++) {
            NestedQuery nestedQuery = nestedQueryArr[i];
            if (i > 0) {
                nestedQueryBuilder.append(WHERE).append(StringUtils.defaultIfBlank(lastWhere, nestedQuery.select()));
            }
            nestedQueryBuilder.append(SPACE).append(lastOp).append(" (").append(getNestedQuery(nestedQuery));

            lastOp = nestedQuery.op();
            lastWhere = nestedQuery.where();
        }
        return nestedQueryBuilder;
    }

    private static String getNestedQuery(NestedQuery nestedQuery) {
        return SELECT +
                nestedQuery.select() +
                FROM +
                nestedQuery.from() +
                StringUtils.defaultIfBlank(SPACE + nestedQuery.extra(), EMPTY);
    }

    interface Processor {
        String process(List<Object> argList, Object value);
    }

}
