package cn.siwei.fubin.swmybatisenhance.model;




/**
 * 分页组件
*/

public class PageFilterModel {


    //页码
    /**
     * 页码
    */
    Integer pageNum=0;

    //分页大小
    /**
     * 分页大小
    */
    Integer pageSize=100;

    public Integer getPageNum() {
        if (pageNum==null){
            pageNum=1;
        }
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        if(pageNum<1){
            pageNum=1;
        }
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        if(pageSize<=1000) {
            return pageSize;
        }else {
            return 1000;
        }
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }


}
