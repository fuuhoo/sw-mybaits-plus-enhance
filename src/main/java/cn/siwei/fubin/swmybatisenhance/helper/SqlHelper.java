package cn.siwei.fubin.swmybatisenhance.helper;


import org.springframework.util.CollectionUtils;

import java.util.Collection;

public class SqlHelper {

    /**
     * 构建 IN 子句，例如：account IN (#{accountList[0]}, #{accountList[1]})
     * @param column 列名
     * @param paramName MyBatis 参数名（即 @Param 指定的名称）
     * @param values 集合值（用于判空）
     * @return 完整的 IN 子句字符串；若集合为空则返回 null
     */
    public static String buildInClause(String column, String paramName, Collection<?> values) {
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }
        StringBuilder inClause = new StringBuilder(column).append(" IN (");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) inClause.append(", ");
            inClause.append("#{").append(paramName).append("[").append(i).append("]}");
        }
        inClause.append(")");
        return inClause.toString();
    }

    /**
     * 重载方法，使用默认的参数名 "list"（适用于无 @Param 或单个 List 参数的情况）
     */
    public static String buildInClause(String column, Collection<?> values) {
        return buildInClause(column, "list", values);
    }
}