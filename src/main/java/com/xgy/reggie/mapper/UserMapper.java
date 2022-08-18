package com.xgy.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xgy.reggie.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
