package com.webmodel.agent.common;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private long total;
    private int page;
    private int size;
    private List<T> list;

    public static <T> PageResult<T> of(long total, int page, int size, List<T> list) {
        PageResult<T> r = new PageResult<>();
        r.total = total;
        r.page = page;
        r.size = size;
        r.list = list;
        return r;
    }
}