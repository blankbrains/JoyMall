package com.joy.common.constant;

/**
 * @Description:
 * @Author:joymall
 * @Date:2022/5/21 16:07
 */
public class WareConstant {

    public enum PurchaseEnum {
        CREATED(0, "新建"),
        ASSIGNED(1, "已分配"),
        RECEIVED(2, "已领取"),
        COMPLETED(3, "已完成"),
        HASHERROR(4, "有异常");

        private int status;
        private String content;


        PurchaseEnum(int status, String content) {
            this.status = status;
            this.content = content;
        }

        public int getCode() {
            return status;
        }

        public String getMsg() {
            return content;
        }
    }

    public enum PurchaseDetailEnum {
        CREATED(0, "新建"),
        ASSIGNED(1, "已分配"),
        BUYING(2, "正在采购"),
        COMPLETED(3, "已完成"),
        PURCHASEFAIL(4, "采购失败");

        private int status;
        private String content;


        PurchaseDetailEnum(int status, String content) {
            this.status = status;
            this.content = content;
        }

        public int getCode() {
            return status;
        }

        public String getMsg() {
            return content;
        }
    }
}
