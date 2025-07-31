package cn.siwei.fubin.swmybatisenhance.exception;

public class MyDbException  extends RuntimeException{

    private int code;
    private String message;

    public MyDbException(Integer code, String msg) {
        this.code = code;
        this.message = msg;
    }

    public MyDbException(String msg) {
        this.code = 400;
        this.message = msg;
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public MyDbException(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public MyDbException(String message, int code, String message1) {
        super(message);
        this.code = code;
        this.message = message1;
    }

    public MyDbException(String message, Throwable cause, int code, String message1) {
        super(message, cause);
        this.code = code;
        this.message = message1;
    }

    public MyDbException(Throwable cause, int code, String message) {
        super(cause);
        this.code = code;
        this.message = message;
    }

    public MyDbException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, int code, String message1) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.code = code;
        this.message = message1;
    }
}
