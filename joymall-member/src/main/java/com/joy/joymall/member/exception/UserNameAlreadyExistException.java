package com.joy.joymall.member.exception;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/6/6 19:39
 */
public class UserNameAlreadyExistException extends RuntimeException{

    /**
     * Constructs a new runtime exception with {@code null} as its
     * detail message.  The cause is not initialized, and may subsequently be
     * initialized by a call to {@link #initCause}.
     */
    public UserNameAlreadyExistException() {
        super("用户名已存在！");
    }
}
