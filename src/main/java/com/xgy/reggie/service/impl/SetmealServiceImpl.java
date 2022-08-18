package com.xgy.reggie.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xgy.reggie.common.CustomException;
import com.xgy.reggie.common.R;
import com.xgy.reggie.dto.SetmealDto;
import com.xgy.reggie.entity.Dish;
import com.xgy.reggie.entity.Setmeal;
import com.xgy.reggie.entity.SetmealDish;
import com.xgy.reggie.mapper.SetmealMapper;
import com.xgy.reggie.service.SetmealDishService;
import com.xgy.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private SetmealService setmealService;
    /**
     * 新增套餐，同时需要保存套餐跟菜品的关联关系(多表)
     * @param setmealDto
     */
    @Transactional
    @Override
    public void saveWithDish(SetmealDto setmealDto) {
        //保存套餐的基本信息，插入setmeal表，
        //原本setmealDto中没有套餐id：SetmealId，因为只有插入完成，雪花算法才会生成套餐id
        this.save(setmealDto);  //但是这里save完之后会更新setmealDto，完成赋值SetmealId

        //取出套餐和菜品的关联信息，扩展加上套餐id：SetmealId
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        setmealDishes.stream().map((item) ->{
            item.setSetmealId(setmealDto.getId());  //此时已经赋值SetmealId了
            return item;
        }).collect(Collectors.toList());

        //保存套餐和菜品的关联信息，插入setmeal_dish表
        setmealDishService.saveBatch(setmealDishes);

    }


    /**
     * 删除套餐前，及套餐中关联的菜品
     * @param ids
     */
    @Override
    @Transactional
    public void removeWithDish(List<Long> ids) {
        //查询套餐状态，确定是否可以删除
        LambdaQueryWrapper<Setmeal> lqw = new LambdaQueryWrapper<>();
        lqw.in(Setmeal::getId,ids);
        lqw.eq(Setmeal::getStatus,1);

        //得到ids集合中的、处于起售状态下的 套餐数
        int count = this.count(lqw);
        if(count > 0){
            //如果不能删除，抛出一个业务异常
            throw new CustomException("套餐正在售卖中，不能删除！");
        }


        //如果可以删除，先删除套餐表的数据  setmeal
        this.removeByIds(ids);

        //再删除套餐菜品关联表中的数据     setmeal_dish
        LambdaQueryWrapper<SetmealDish> lqw2 = new LambdaQueryWrapper<>();
        lqw2.in(SetmealDish::getSetmealId,ids);//这里的ids中的id是套餐id：SetmealId

        setmealDishService.remove(lqw2);
    }
}
