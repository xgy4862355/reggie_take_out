package com.xgy.reggie.common;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;


//通用的返回结果类，我们后端响应的数据都封装成此对象
@Data
public class R<T> {

    private Integer code; //编码：1成功，0和其它数字为失败

    private String msg; //错误信息

    private T data; //数据，不同的对象数据不同，故用泛型来复用，比如 T可能等于 employee 可能等于admin...

    private Map map = new HashMap(); //动态数据

    //成功的话，就把成功对应的R对象造出来，并返回给前端
    public static <T> R<T> success(T object) {
        R<T> r = new R<T>();
        r.data = object;
        r.code = 1;
        return r;
    }

    //失败的话，就把失败对应的R对象造出来，并返回给前端
    public static <T> R<T> error(String msg) {
        R r = new R();
        r.msg = msg;
        r.code = 0;
        return r;
    }

    public R<T> add(String key, Object value) {
        this.map.put(key, value);
        return this;
    }

}
