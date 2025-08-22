package cn.siwei.fubin.swmybatisenhance.util;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 字符串工具类
 *
 * @author Lion Li
 */


public class StringUtils {

    public static final String SEPARATOR = ",";
    public static final String EMPTY = "";

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern URL_PATTERN = Pattern.compile("^(https?://).+");
    private static final Pattern WILDCARD_PATTERN = Pattern.compile("\\?|\\*|\\*\\*");

    /**
     * 驼峰转下划线命名
     */
    public static String camel4underline(String param) {
        if (param == null || param.isEmpty()) {
            return EMPTY;
        }

        Matcher matcher = UPPERCASE_PATTERN.matcher(param);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            matcher.appendReplacement(result, "_" + matcher.group().toLowerCase());
        }

        matcher.appendTail(result);

        if (result.charAt(0) == '_') {
            return result.substring(1);
        }
        return result.toString();
    }

    /**
     * 根据字段名获取getter方法名
     */
    public static String getGetMethodName(String fieldName) {
        if (isEmpty(fieldName)) {
            return EMPTY;
        }
        return "get" + capitalize(fieldName);
    }

    /**
     * 根据字段名获取setter方法名
     */
    public static String getSetMethodName(String fieldName) {
        if (isEmpty(fieldName)) {
            return EMPTY;
        }
        return "set" + capitalize(fieldName);
    }

    // 首字母大写
    private static String capitalize(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * 空白值返回默认值
     */
    public static String blankToDefault(String str, String defaultValue) {
        return isBlank(str) ? defaultValue : str;
    }

    /**
     * 判断字符串是否为空（null或空字符串）
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * 判断字符串是否非空
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * 去除字符串两端空白
     */
    public static String trim(String str) {
        return str == null ? null : str.trim();
    }

    /**
     * 截取字符串（从开始到结尾）
     */
    public static String substring(final String str, int start) {
        if (str == null) {
            return null;
        }
        if (start < 0) {
            start = 0;
        }
        return start < str.length() ? str.substring(start) : EMPTY;
    }

    /**
     * 截取字符串（指定起止位置）
     */
    public static String substring(final String str, int start, int end) {
        if (str == null) {
            return null;
        }
        if (start < 0) start = 0;
        if (end > str.length()) end = str.length();
        return start < end ? str.substring(start, end) : EMPTY;
    }

    /**
     * 格式化文本（使用{}占位符）
     */
    public static String format(String template, Object... params) {
        if (isEmpty(template) ){
            return template;
        }
        if (params == null || params.length == 0) {
            return template.replace("\\{}", "{}");
        }

        StringBuilder sb = new StringBuilder();
        int paramIndex = 0;
        for (int i = 0; i < template.length(); i++) {
            char c = template.charAt(i);
            if (c == '{') {
                if (i + 1 < template.length() && template.charAt(i + 1) == '}') {
                    if (paramIndex < params.length) {
                        sb.append(params[paramIndex++]);
                        i++; // 跳过下一个字符 '}'
                    } else {
                        sb.append("{}");
                        i++;
                    }
                } else {
                    sb.append(c);
                }
            } else if (c == '\\' && i + 1 < template.length()) {
                char next = template.charAt(i + 1);
                if (next == '{' || next == '\\') {
                    sb.append(next);
                    i++; // 跳过转义字符
                } else {
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 检查是否为http/https链接
     */
    public static boolean ishttp(String link) {
        return link != null && URL_PATTERN.matcher(link).matches();
    }

    /**
     * 字符串转Set
     */
    public static Set<String> str2Set(String str, String sep) {
        return new HashSet<>(str2List(str, sep, true, false));
    }

    /**
     * 字符串转List
     */
    public static List<String> str2List(String str, String sep,
                                        boolean filterBlank, boolean trim) {
        if (isEmpty(str)) {
            return Collections.emptyList();
        }

        return Stream.of(str.split(sep))
                .map(s -> trim ? s.trim() : s)
                .filter(s -> !filterBlank || !isBlank(s))
                .collect(Collectors.toList());
    }

    /**
     * 检查字符串是否空白（null/空字符串/纯空白字符）
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 忽略大小写检查是否包含任意子串
     */
    public static boolean containsAnyIgnoreCase(CharSequence cs, CharSequence... searchSeq) {
        if (cs == null || searchSeq == null) {
            return false;
        }
        String str = cs.toString().toLowerCase();
        for (CharSequence seq : searchSeq) {
            if (seq != null && str.contains(seq.toString().toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 驼峰转下划线命名（别名）
     */
    public static String toUnderScoreCase(String str) {
        return camel4underline(str);
    }

    /**
     * 检查字符串是否在集合中（忽略大小写）
     */
    public static boolean inStringIgnoreCase(String str, String... strs) {
        if (str == null || strs == null) {
            return false;
        }
        return Stream.of(strs)
                .anyMatch(s -> s != null && s.equalsIgnoreCase(str));
    }

    /**
     * 下划线转大驼峰命名（首字母大写）
     */
    public static String convertToCamelCase(String name) {
        return capitalize(toCamelCase(name));
    }

    /**
     * 下划线转小驼峰命名（首字母小写）
     */
    public static String toCamelCase(String s) {
        if (isEmpty(s)) {
            return s;
        }

        StringBuilder result = new StringBuilder();
        boolean nextUpper = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '_') {
                nextUpper = true;
            } else {
                if (nextUpper) {
                    result.append(Character.toUpperCase(c));
                    nextUpper = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            }
        }
        return result.toString();
    }

    /**
     * 通配符匹配
     */
    public static boolean matches(String str, List<String> patterns) {
        if (isEmpty(str) || patterns == null || patterns.isEmpty()) {
            return false;
        }
        return patterns.stream().anyMatch(pattern -> isMatch(pattern, str));
    }

    /**
     * 通配符匹配实现
     */
    public static boolean isMatch(String pattern, String url) {
        if (pattern == null || url == null) {
            return false;
        }

        // 用 Matcher 循环，先 find 再 group
        Matcher m = WILDCARD_PATTERN.matcher(pattern);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String matched = m.group();   // 此时 find() 已保证匹配成功
            String replacement;
            switch (matched) {
                case "?":  replacement = ".";      break;
                case "*":  replacement = "[^/]*";  break;
                case "**": replacement = ".*";     break;
                default:   replacement = matched;  // 理论上不会走到这里
            }
            m.appendReplacement(sb, replacement);
        }
        m.appendTail(sb);

        String regex = sb.toString().replace("/", "\\/");
        regex = "^" + regex + "$";
        return Pattern.compile(regex).matcher(url).matches();
    }

    /**
     * 数字左侧补零
     */
    public static String padl(final Number num, final int size) {
        return num != null ? padl(num.toString(), size, '0') : null;
    }

    /**
     * 字符串左侧补字符
     */
    public static String padl(final String s, final int size, final char c) {
        if (s == null) {
            return repeat(c, size);
        }

        if (s.length() >= size) {
            return s.substring(s.length() - size);
        }

        return repeat(c, size - s.length()) + s;
    }

    // 重复字符
    private static String repeat(char c, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, c);
        return new String(chars);
    }

    /**
     * 分割字符串（默认逗号分隔）
     */
    public static List<String> splitList(String str) {
        return splitTo(str, String::valueOf);
    }

    /**
     * 分割字符串（指定分隔符）
     */
    public static List<String> splitList(String str, String separator) {
        return splitTo(str, separator, String::valueOf);
    }

    /**
     * 分割并转换（默认逗号分隔）
     */
    public static <T> List<T> splitTo(String str, Function<Object, T> mapper) {
        return splitTo(str, SEPARATOR, mapper);
    }

    /**
     * 分割并转换（指定分隔符）
     */
    public static <T> List<T> splitTo(String str, String separator, Function<Object, T> mapper) {
        if (isBlank(str)) {
            return Collections.emptyList();
        }

        return Stream.of(str.split(separator))
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .map(mapper)
                .collect(Collectors.toList());
    }
}
