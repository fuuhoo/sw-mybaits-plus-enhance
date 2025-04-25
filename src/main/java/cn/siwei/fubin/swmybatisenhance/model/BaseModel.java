package cn.siwei.fubin.swmybatisenhance.model;


import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

//基类
@Data
public class BaseModel implements Serializable {

    
    /**
     * @description: 创建用户
    **/
    @JsonIgnore
    String createUser;

    /**
     * @description: 修改用户
    **/
    @JsonIgnore
    String updateUser;

    /**
     * @description: 创建时间
    **/
    @TableField(fill = FieldFill.INSERT)
    @JsonIgnore
    Date createTime;
    
    /**
     * @description: 修改时间
    **/

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonIgnore
    Date updateTime;

    /**
     * @description: 逻辑删除
    **/
    @JsonIgnore
    @TableField(select = false)
    @TableLogic(value = "0",delval = "id")
    Long deleted;


//    /**
//     * 请求参数
//     */
//    @JsonInclude(JsonInclude.Include.NON_EMPTY)
//    @TableField(exist = false)
//    private Map<String, Object> params = new HashMap<>();


}
