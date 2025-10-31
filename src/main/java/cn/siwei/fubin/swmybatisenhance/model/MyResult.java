package cn.siwei.fubin.swmybatisenhance.model;

import java.io.Serializable;

public class MyResult<T> implements Serializable {

    /**
     * 0正常，其他异常
    */
    private int code;

    /**
     * v状态
    */
    private String status;

    /**
     * 返回信息
    */
    private String message;


    /**
     * 返回数据
    */
    private T data;


    public MyResult(){

    }



    public MyResult(int code, String status) {
        this.code = code;
        this.status = status;
    }




    public MyResult(String message,T data) {
        this.code = code;
        this.data = data;
    }


    public MyResult(int code, String status, String message, T data) {
        this.code = code;
        this.status = status;
        this.message = message;
        this.data = data;
    }


    public static MyResult fail(int code,String message) {
        return new MyResult(code,message);
    }



    public static <T> MyResult success(T data) {
        return new MyResult("SUCCESS", data);
    }


    public static <T> MyResult success() {
        return new MyResult();
    }


    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "MyResult{" +
                "code=" + code +
                ", status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}
