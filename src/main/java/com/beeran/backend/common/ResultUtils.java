package com.beeran.backend.common;

/**
 * 返回工具类
 */
public class ResultUtils {
    public static <T> BaseResponse Success(T data){
        return new BaseResponse(0, data, "ok","成功");
    }
    public static BaseResponse Error(ErrorCode errorCode){
        return new BaseResponse<>(errorCode);
    }
    public static BaseResponse Error(ErrorCode errorCode, String description){
        return new BaseResponse<>(errorCode.getCode(), null, errorCode.getMessage(), description);
    }
    public static BaseResponse Error(ErrorCode errorCode, String message,String description){
        return new BaseResponse<>(errorCode.getCode(), null, message, description);
    }
    public static BaseResponse Error(int code, String message,String description){
        return new BaseResponse<>(code, null, message, description);
    }
}
