package com.beeran.backend.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class pageResponse implements Serializable {
    /**
     * 页面大小
     */
    protected  int pageSize = 10;
    /**
     * 显示第几页
     */
    protected int pageNum = 1;
}
