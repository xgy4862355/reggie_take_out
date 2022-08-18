package com.xgy.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xgy.reggie.entity.Orders;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<Orders> {
}
