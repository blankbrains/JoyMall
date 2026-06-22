package com.joy.common.constant;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/5/17 22:36
 */
public class ProductConstant {

    public enum AttrEnum {
        BASE_ATTR_TYPE(1, "基本属性"),
        SALE_ATTR_TYPE(0, "销售属性");

        private int code;
        private String msg;


        AttrEnum(int code, String msg) {
            this.code = code;
            this.msg = msg;
        }

        public int getCode() {
            return code;
        }

        public String getMsg() {
            return msg;
        }
    }


    public enum StatusEnum {
        NEW(0, "新建"),
        UP(1, "上架"),
        DOWN(2, "下架");

        private int code;
        private String msg;


        StatusEnum(int code, String msg) {
            this.code = code;
            this.msg = msg;
        }

        public int getCode() {
            return code;
        }

        public String getMsg() {
            return msg;
        }
    }
}
