package cn.siwei.fubin.swmybatisenhance.util;


import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * stream 流工具类
 *
 * @author Lion Li
 */

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StreamUtils {

    // 默认分隔符（Hutool 中为逗号）
    public static final String SEPARATOR = ",";
    public static final String EMPTY = "";

    /**
     * 将collection过滤
     */
    public static <E> List<E> filter(Collection<E> collection, Predicate<E> function) {
        if (isEmpty(collection)) {
            return Collections.emptyList();
        }
        return collection.stream()
                .filter(function)
                .collect(Collectors.toList());
    }

    /**
     * 将collection拼接（使用默认分隔符）
     */
    public static <E> String join(Collection<E> collection, Function<E, String> function) {
        return join(collection, function, SEPARATOR);
    }

    /**
     * 将collection拼接（自定义分隔符）
     */
    public static <E> String join(Collection<E> collection,
                                  Function<E, String> function,
                                  CharSequence delimiter) {
        if (isEmpty(collection)) {
            return EMPTY;
        }
        return collection.stream()
                .map(function)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(delimiter));
    }

    /**
     * 将collection排序
     */
    public static <E> List<E> sorted(Collection<E> collection, Comparator<E> comparing) {
        if (isEmpty(collection)) {
            return Collections.emptyList();
        }
        return collection.stream()
                .filter(Objects::nonNull)
                .sorted(comparing)
                .collect(Collectors.toList());
    }

    /**
     * 将collection转化为类型不变的map
     */
    public static <V, K> Map<K, V> toIdentityMap(Collection<V> collection, Function<V, K> key) {
        if (isEmpty(collection)) {
            return new HashMap<>();
        }
        return collection.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        key,
                        Function.identity(),
                        (l, r) -> l
                ));
    }

    /**
     * 将Collection转化为map(value类型与collection的泛型不同)
     */
    public static <E, K, V> Map<K, V> toMap(Collection<E> collection,
                                            Function<E, K> key,
                                            Function<E, V> value) {
        if (isEmpty(collection)) {
            return new HashMap<>();
        }
        return collection.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        key,
                        value,
                        (l, r) -> l
                ));
    }

    /**
     * 将collection按照规则分类成map
     */
    public static <E, K> Map<K, List<E>> groupByKey(Collection<E> collection, Function<E, K> key) {
        if (isEmpty(collection)) {
            return new LinkedHashMap<>();
        }
        return collection.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        key,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    /**
     * 将collection按照两个规则分类成双层map
     */
    public static <E, K, U> Map<K, Map<U, List<E>>> groupBy2Key(Collection<E> collection,
                                                                Function<E, K> key1,
                                                                Function<E, U> key2) {
        if (isEmpty(collection)) {
            return new LinkedHashMap<>();
        }
        return collection.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        key1,
                        LinkedHashMap::new,
                        Collectors.groupingBy(
                                key2,
                                LinkedHashMap::new,
                                Collectors.toList()
                        )
                ));
    }

    /**
     * 将collection按照两个规则分类成双层map
     */
    public static <E, T, U> Map<T, Map<U, E>> group2Map(Collection<E> collection,
                                                        Function<E, T> key1,
                                                        Function<E, U> key2) {
        if (isEmpty(collection) || key1 == null || key2 == null) {
            return new LinkedHashMap<>();
        }
        return collection.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        key1,
                        LinkedHashMap::new,
                        Collectors.toMap(
                                key2,
                                Function.identity(),
                                (l, r) -> l
                        )
                ));
    }

    /**
     * 将collection转化为List集合（泛型不同）
     */
    public static <E, T> List<T> toList(Collection<E> collection, Function<E, T> function) {
        if (isEmpty(collection)) {
            return new ArrayList<>();
        }
        return collection.stream()
                .map(function)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 将collection转化为Set集合（泛型不同）
     */
    public static <E, T> Set<T> toSet(Collection<E> collection, Function<E, T> function) {
        if (isEmpty(collection) || function == null) {
            return new HashSet<>();
        }
        return collection.stream()
                .map(function)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * 合并两个相同key类型的map
     */
    public static <K, X, Y, V> Map<K, V> merge(Map<K, X> map1,
                                               Map<K, Y> map2,
                                               BiFunction<X, Y, V> merge) {
        if (isEmpty(map1) && isEmpty(map2)) {
            return new HashMap<>();
        }

        // 获取所有唯一键
        Set<K> keys = new HashSet<>();
        if (!isEmpty(map1)) keys.addAll(map1.keySet());
        if (!isEmpty(map2)) keys.addAll(map2.keySet());

        // 构建合并后的Map
        Map<K, V> result = new HashMap<>();
        for (K key : keys) {
            X value1 = map1 != null ? map1.get(key) : null;
            Y value2 = map2 != null ? map2.get(key) : null;
            V mergedValue = merge.apply(value1, value2);

            if (mergedValue != null) {
                result.put(key, mergedValue);
            }
        }
        return result;
    }

    // ============== 工具方法 ==============
    private static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    private static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }
}
