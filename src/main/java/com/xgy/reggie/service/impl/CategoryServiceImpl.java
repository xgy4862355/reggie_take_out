package com.xgy.reggie.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xgy.reggie.common.CustomException;
import com.xgy.reggie.entity.Category;
import com.xgy.reggie.entity.Dish;
import com.xgy.reggie.entity.Setmeal;
import com.xgy.reggie.mapper.CategoryMapper;
import com.xgy.reggie.service.CategoryService;
import com.xgy.reggie.service.DishService;
import com.xgy.reggie.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Autowired
    private DishService dishService;

    @Autowired
    private SetmealService setmealService;

    /**
     * 根据分类id删除分类，删除之前需要进行关联判断(类似于外键级联)
     * @param id
     */
    @Override
    public void remove(Long id) {
        //查询当前分类是否关联了菜品，如果已经关联，就抛出一个业务异常
        LambdaQueryWrapper<Dish> lqw = new LambdaQueryWrapper<>();
        //添加查询条件，分类id
        lqw.eq(Dish::getCategoryId,id);
        //得到菜品表中，关联到该分类(id)的数量
        int count = dishService.count(lqw);
        if(count > 0){
            //该分类已关联菜品，不能直接删除，抛出一个业务异常
            throw new CustomException("当前分类下关联了菜品，不能直接删除！");
        }

        //查询当前分类是否关联了套餐，如果已经关联，就抛出一个业务异常
        LambdaQueryWrapper<Setmeal> lqw2 = new LambdaQueryWrapper<>();
        lqw2.eq(Setmeal::getCategoryId,id);
        //得到套餐表中，关联到该分类(id)的数量
        int count2 = setmealService.count(lqw2);
        if(count2 > 0){
            //该分类已关联套餐，不能直接删除，抛出一个业务异常
            throw new CustomException("当前分类下关联了套餐，不能直接删除！");
        }

        //正常删除分类
        super.removeById(id);
    }
}
