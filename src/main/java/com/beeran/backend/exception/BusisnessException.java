package com.beeran.backend.exception;

import com.beeran.backend.common.ErrorCode;

/**
 * 定义全局异常处理
 * @author BeerAn
 */
public class BusisnessException extends RuntimeException {
    private final int code;
    private final String description;

    public BusisnessException(String message, int code, String description) {
        super(message);
        this.code = code;
        this.description = description;
    }

    public BusisnessException(ErrorCode errorCode){
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.description = errorCode.getDescription();
    }
    public BusisnessException(ErrorCode errorCode, String description){
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
