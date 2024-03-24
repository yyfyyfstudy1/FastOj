package com.fastoj.common;

/**
 * 自定义错误码
 *
 * @author Shier
 */
public enum ErrorCode {

    SUCCESS(0, "ok"),
    PARAMS_ERROR(40000, "Request parameter error"),
    NOT_LOGIN_ERROR(40100, "Not logged in"),
    NO_AUTH_ERROR(40101, "No permission"),
    NOT_FOUND_ERROR(40400, "The requested data does not exist"),

    /**
     * 40001 数据为空
     */
    NULL_ERROR(40001, "Request data is empty"),

    TOO_MANY_REQUEST(42900, "Requests are too frequent"),
    API_REQUEST_ERROR(50010, "Interface call error"),
    FORBIDDEN_ERROR(40300, "No Access"),
    SYSTEM_ERROR(50000, "System internal exception"),
    OPERATION_ERROR(50001, "operation failed");


    /**
     * 状态码
     */
    private final int code;

    /**
     * 信息
     */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

}
