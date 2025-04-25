自开发的mybatis-plus的增强
主要功能就是针对mybatisplus做一些增删改查小功能的增强。性能未做测试，但是在小数据量的时候可以减少开发量。

#### ``1、@ForeignkeyCheck``

作用：
在新增数据的时候。如果model指定了外键，可以使用此注解，来检查，外键的数据是否已经提前插入。
会提前使用selectCount方法查询数据库验证实体类是否存在，不存在直接不通过

实现类: 

```
cn.siwei.fubin.swmybatisenhance.validator.ForeignKeyValidator;
```


使用方法:

```
@ForeignkeyCheck(foreignMapperClass = ProjectMapper.class,foreignId = "id")
Long pId;
```

参数解析

```
    Class foreignMapperClass(); #外键的mapper，必填

    boolean canNull() default false; #是否可以为空，也就是传的时候这个字段为null的时候则不校验

    String foreignId() default "id"; #数据id，默认为id，如果指定别的列需要显式的声明

    String message() default "数据不存在"; #默认提醒的信息


```

#### ``2、@DeleteRefCheck``

作用：通过id删除的时候查询是否已经被用作其他表的外键使用。如果被用了，则不允许删除，需要先删除其他数据。

实现类:  

```
cn.siwei.fubin.swmybatisenhance.aspect.DeleteRefCheckAspect

cn.siwei.fubin.swmybatisenhance.helper.CheckRefHelper
```

使用方法

注解在service方法上面


```
@DeleteRefCheck(mapperClazz = OriganizationMapper.class, refFieldName = "pwId")
@Override
public Integer delPartWork(String ID) {


    List<PartWork> partWorks = partWorkMapper.selectList(new LambdaQueryWrapper<PartWork>().eq(PartWork::getFId, ID));
    if(!ObjectUtils.isEmpty(partWorks)){
        throw new BaseException("请先删除子节点:"+partWorks.get(0).getName());
    }

    int i = partWorkMapper.deleteById(ID);

    return i;

}
```

参数解析



```
public @interface DeleteRefCheck {
    Class mapperClazz(); //mapper的类名
    String refParamName(); //引用的参数的名称。也就是别的表外键指向我们的字段的名称
    String selfIdFieldName() default "id"; //本身的id字段的名称，名人为id
}

```

#### ``3、@DeleteRefCheckList``

说明：列表实现DeleteRefCheck

使用方法：
```
    @DeleteRefCheckList(list = {
            @DeleteRefCheck(mapperClazz = MenuMapper.class, refFieldName = "fId",meaasge = "存在子菜单，无法删除"),
        }
    )
    @Transactional
    @Override
    public Integer delMenu(Long id) {
        int i = menuMapper.deleteById(id);
        return i;
    }

```

#### ``4、@FilterFiled``

说明:在model里面增加此注解，自动生成查询条件。

实现类：
```
cn.siwei.fubin.swmybatisenhance.helper.FilterWrapperHelper
```

使用方法:在model内的字段上面注解，然后再service里面查询的时候使用FilterWrapperHelper生成查询条件


```

/**
* 流程名称
*/
@FilterFiled(type = FilterTypeEnum.LIKE)
@NotBlank(message = "流程名称不能为空", groups = { AddGroup.class, EditGroup.class })
private String processName;



@Autowired
FilterWrapperHelper<WfCopy> wfFilterWrapperHelper;


@Override
public PageData<WfCopyVo> selectPageList(WfCopyBo bo, PageFilterModel pageQuery) {

    LambdaQueryWrapper<WfCopy> lqw = wfFilterWrapperHelper.getLambdaWrapperbyFiledAnnotion(bo);
    lqw.orderByDesc(WfCopy::getCreateTime);
    Page<WfCopy> page = wfFilterWrapperHelper.getPage(pageQuery);
    Page<WfCopy> wfCopyPage = wfCopyMapper.selectPage(page, lqw);
    PageData<WfCopyVo> extendNamePageData = ForeignKeyVoExtendUtil.getExtendNamePageData(wfCopyPage, WfCopyVo.class);
    return extendNamePageData;
}

    
```


参数说明:

```
FilterTypeEnum type() default FilterTypeEnum.EQ; #    EQ:等于;LIKE:模糊;LEFT:左批评;RIGHT:右批评

```

#### ``5、@Updatable和UpdateMethod``

说明:两个注解需要配合使用，字段是否可以被更新，只有添加注解的才可以被更新


实现类:

```
cn.siwei.fubin.swmybatisenhance.aspect.UpdataMethodAspect
```

使用方法:
```

#model
@NotBlank(message = "组织名称不可为空")
@Updatable
String name;


#service
@UpdateMethod(paramEntityClass = Application.class)
@Override
public Integer updateApplication(Application app) {
    int i = applicationMapper.updateById(app);
    return i;
}


```

参数说明:

```

# Updatable
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Updatable {
    boolean throwExpect() default false; #传了不被允许更新的是否抛出异常，默认忽略
}


#UpdateMethod

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UpdateMethod {
    Class modelClazz();  #模型类
}


```


#### ``6、@VoExtend和VoExtendList和@SysDictVoExtend``

说明:用于返回字段附带名称或者实体信息，一般用于vo,需要配合 `ForeignKeyVoExtendUtil`使用


@VoExtend主要应用于多一对一，例如，也就是一个学生只会有一个班级，取学生所在班级的名称或者学生所在班级的班级信息。

@VoExtendList一般用于一对多,例如，取班级内部所有的学生的姓名或者学生的信息


使用方法
```

#model
# SysDictVoExtend-------------------------------------------------------
/**
    * @description: 权限分类
**/        
@VoExtend(sIdField = "fId",mapperName = MenuMapper.class, eNameField = "name")
String fName;

# SysDictVoExtend ------------------------------------------------------
@Data
public class PartWorkVo extends PartWork {

    @SysDictVoExtend(mapperName = SysDictMapper.class, selfModelFileName ="type",sysDictTypeFiedName = "dic_type", sysDictTypeName = "partType")
    String typeName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:SS",timezone = "GMT+8")
    Date createTime;

}


# VoExtendList-------------------------------------------------------



#最后使用
List<TeamVo> extendNameList = ForeignKeyVoExtendUtil.getExtendNameList(teams, TeamVo.class);

```




```
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface VoExtend {

    //mapper类
    Class<? extends BaseMapper> mapperName();
    //根据原始的哪个字段查
    String selfIdField() default "id";
    //拓展字段，要显示的名称
    String extNameField() default "name";
    //数据库的字段名称
    String extDbNameField() default "";
    //拓展字段，在拓展对象的id
    String extIdField() default "id";
    //数据库的字段名称
    String extDbIdField() default "";
    //拓展的类型。是一个字段还是一个对象。当type=OBJECT的时候是对象
    VoExtendType type() default VoExtendType.Str;


}



public @interface VoExtendList {

    //mapper类
    Class<? extends BaseMapper> mapperName();
    //根据原始的哪个字段查
    String selfModelIdField() default "id";
    //拓展字段，要显示的名称
    String extNameField() default "name";
    //拓展字段，在拓展对象的id
    String extIdField() default "id";
    //拓展的类型。是一个字段还是一个对象。当type=OBJECT的时候是对象
    VoExtendType type() default VoExtendType.Str;
    
}



@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SysDictVoExtend {

    //字典的数据库的mapper
    Class<? extends BaseMapper>  mapperName() ;
    //原数据的id字段名称
    String selfModelFileName() ;
    //对象的字典类型的名称
    String sysDictTypeFiedName();
    //字典类型
    String sysDictTypeName() default  "dic_type";
    //要显示的字段的名称
    String sysDictValueFiedName() default "label";
    //字典库的对应id字段
    String sysDictKeyFiedName() default "code";
}





```