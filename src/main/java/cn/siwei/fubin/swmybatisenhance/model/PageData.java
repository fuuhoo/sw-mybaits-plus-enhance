package cn.siwei.fubin.swmybatisenhance.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageData<T> {

    @Builder.Default
    Long current=0L;
    @Builder.Default
    Long size=0L;
    @Builder.Default
    Long totalCount=0L;
    @Builder.Default
    List<T> dataList=new ArrayList<>();

//    public PageData(IPage<T> page) {
//        List<T> list = page.getRecords();
//        setDataList((List<T>) list);
//        setSize((long) list.size());
//        setCurrent(page.getCurrent());
//        setTotalCount(page.getTotal());
//    }

//    public PageData() {
//
//    }

    public Long getCurrent() {
        return current;
    }

    public void setCurrent(Long current) {
        this.current = current;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    public List<T> getDataList() {
        return dataList;
    }

    public void setDataList(List<T> dataList) {
        this.dataList = dataList;
    }

    public void copy(PageData p) {
        setCurrent(p.getCurrent());
        setTotalCount(p.getTotalCount());
        setSize(p.getSize());
    }



}
