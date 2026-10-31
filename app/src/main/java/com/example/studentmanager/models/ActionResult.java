package com.example.studentmanager.models;

/** Ket qua dung chung cho login va cac thao tac ghi. */
public final class ActionResult {
    public static final String OK = "OK";
    public static final String VALIDATION = "VALIDATION";
    public static final String DUPLICATE = "DUPLICATE";
    public static final String CONFLICT = "CONFLICT";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String IN_USE = "IN_USE";
    public static final String DB_ERROR = "DB_ERROR";
    public static final String NOT_IMPLEMENTED = "NOT_IMPLEMENTED";

    public final boolean ok;
    public final String code;
    public final String message;
    public final long id;

    private ActionResult(boolean ok, String code, String message, long id) {
        this.ok = ok;
        this.code = code;
        this.message = message;
        this.id = id;
    }

    public static ActionResult success(long id) {
        return new ActionResult(true, OK, "Thành công", id);
    }

    public static ActionResult error(String code, String message) {
        return new ActionResult(false, code, message, -1);
    }

    public static ActionResult notImplemented() {
        return error(NOT_IMPLEMENTED, "Chức năng chưa được triển khai");
    }
}
