package com.musa.chat.exception;
public class GroupPermissionException extends RuntimeException {
    public GroupPermissionException() { super("Solo la persona administradora puede realizar esta operación."); }
}
