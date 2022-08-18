package com.xgy.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xgy.reggie.dto.DishDto;
import com.xgy.reggie.entity.Dish;
import com.xgy.reggie.entity.DishFlavor;
import com.xgy.reggie.mapper.DishMapper;
import com.xgy.reggie.service.DishFlavorService;
import com.xgy.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService{

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private DishService dishService;

    /**
     * //新增菜品，同时插入菜品对应的口味数据，需要操作两张表，dish、dish_flavor
     * @param dishDto
     */
    @Override
    @Transactional  //多表操作，开启事务， 配合启动类上注解@EnableTransactionManagement生效
    public void saveWithFlavor(DishDto dishDto) {
        //保存菜品的基本信息到菜品表dish中去，此时雪花算法才会生成一个dish_id存入数据库
        this.save(dishDto);//虽然dishDto实体类属性比dish表中字段多，但是会自动匹配对应字段插入

        //而我们的dish_flavor表中的口味信息需要关联到dish_id，故需要在保存完菜品后再查询一次得到
        Long dishId = dishDto.getId();

        //口味信息，此时还未关联dish_id
        List<DishFlavor> flavors = dishDto.getFlavors();
        //遍历每一条flavor，关联上dish_id
        flavors.stream().map((item) ->{
            item.setDishId(dishId);
            return item;
        }).collect(Collectors.toList());

        //保存菜品口味信息到菜品口味表dish_flavor中去
        dishFlavorService.saveBatch(flavors);  //存集合
    }


    /**
     * 根据id查询菜品信息，同时加上口味信息
     * @param id   //传入的是dish_id 不是DishFlavor表的主键id
     * @return
     */
    @Override
    public DishDto getByIdWithFlavor(Long id) {
        DishDto dishDto = new DishDto();

        //查询菜品基本信息，从dish表查询
        Dish dish = dishService.getById(id);

        //查询当前菜品对应的口味信息，从dish_flavor表查询
        LambdaQueryWrapper<DishFlavor> lqw = new LambdaQueryWrapper<>();
        lqw.eq(DishFlavor::getDishId,id);
        List<DishFlavor> flavors = dishFlavorService.list(lqw);

        //拷贝上基本信息
        BeanUtils.copyProperties(dish,dishDto);
        //扩展上菜品信息
        dishDto.setFlavors(flavors);

        return dishDto;
    }


    /**
     * //更新菜品，同时修改菜品对应的口味数据，需要操作两张表，dish、dish_flavor
     * @param dishDto
     */
    @Override
    @Transactional  //多表操作，开启事务， 配合启动类上注解@EnableTransactionManagement生效
    public void updateWithFlavor(DishDto dishDto) {
        //更新dish表基本信息  这一句this.updateById(dishDto);等于下面三行
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDto,dish);
        dishService.updateById(dish);

        //先根据dishId清理当前菜品对应口味数据
        //dishFlavorService.removeById(dish.getId()); 这样删是把dish_id当做为主键dishFlavor_id去删数据了
        LambdaQueryWrapper<DishFlavor> lqw = new LambdaQueryWrapper<>();
        lqw.eq(DishFlavor::getDishId,dishDto.getId());
        dishFlavorService.remove(lqw);

        //不要忘了我们的dish_flavor表中的口味信息需要关联到dish_id
        //口味信息，此时还未关联dish_id
        List<DishFlavor> flavors = dishDto.getFlavors();
        //遍历每一条flavor，关联上dish_id (其实主要是给新增的口味加上，原来已存在的口味是带dishId的)
        flavors.stream().map((item) ->{
            item.setDishId(dish.getId());
            return item;
        }).collect(Collectors.toList());

        //添加新提交过来的口味数据
        dishFlavorService.saveBatch(flavors);
    }
}
