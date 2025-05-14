package cn.siwei.fubin.swmybatisenhance.helper;

import cn.siwei.fubin.BaseException;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class UpdateNumCheckHelper {

    public static void checkUpdateDataList(Integer num){
        if (!ObjectUtils.isEmpty(num)) {
            if (num == 0) {
                throw new BaseException(400, "未找到符合条件的数据，无法操作");
            }
        }
    }
}
