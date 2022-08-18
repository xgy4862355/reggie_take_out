package com.xgy.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xgy.reggie.dto.SetmealDto;
import com.xgy.reggie.entity.Setmeal;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {
    /**
     * 新增套餐，同时需要保存套餐跟菜品的关联关系
     * @param setmealDto
     */
    public void saveWithDish(SetmealDto setmealDto);

    /**
     * 删除套餐前，及套餐中关联的菜品
     * @param ids
     */
    public void removeWithDish(List<Long> ids);
}
