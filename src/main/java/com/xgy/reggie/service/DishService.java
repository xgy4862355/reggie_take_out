package com.xgy.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xgy.reggie.dto.DishDto;
import com.xgy.reggie.entity.Dish;

public interface DishService extends IService<Dish> {

    //新增菜品，同时插入菜品对应的口味数据，需要操作两张表，dish、dish_flavor
    public void saveWithFlavor(DishDto dishDto);

    //根据id查询菜品信息，同时加上口味信息
    public DishDto getByIdWithFlavor(Long id);

    //修改菜品，同时修改菜品对应的口味数据，需要操作两张表，dish、dish_flavor
    public void updateWithFlavor(DishDto dishDto);

}
