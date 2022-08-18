package com.xgy.reggie.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xgy.reggie.common.R;
import com.xgy.reggie.dto.DishDto;
import com.xgy.reggie.dto.SetmealDto;
import com.xgy.reggie.entity.*;
import com.xgy.reggie.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 套餐管理
 */
@Slf4j
@RestController
@RequestMapping("/setmeal")
public class SetmealController {
    @Autowired
    private SetmealService setmealService;

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DishService dishService;

    @Autowired
    private DishFlavorService dishFlavorService;
    /**
     * 新增套餐
     * @param setmealDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody SetmealDto setmealDto){
        log.info("套餐信息：{}",setmealDto);
        setmealService.saveWithDish(setmealDto);
        return R.success("套餐新增成功！");
    }


    /**
     * 套餐分页查询(多表)
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name){
        //构造分页构造器，这里其实是MP帮我们做查询的时候根据参数加了limit而已
        Page<Setmeal> pageInfo = new Page<>(page,pageSize);

        //条件构造器
        LambdaQueryWrapper<Setmeal> lqw = new LambdaQueryWrapper<>();

        //添加过滤条件
        lqw.like(name!=null,Setmeal::getName,name);
        //添加排序条件
        lqw.orderByDesc(Setmeal::getUpdateTime);

        //执行分页查询
        setmealService.page(pageInfo,lqw);

        //原来的分页数据pageInfo中缺少套餐分类属性，扩展创建一个新的分页信息setmealDtoPage
        Page<SetmealDto>  setmealDtoPage= new Page<>(page,pageSize);

        //对象拷贝，先把原来pageInfo中的除records以外的标志属性拷贝一下
        BeanUtils.copyProperties(pageInfo,setmealDtoPage,"records");

        //原来的pageInfoRecords中没有套餐分类属性
        List<Setmeal> pageInfoRecords = pageInfo.getRecords();

        //再把原来的pageInfoRecords取出来，扩展一下，做新的dishDtoPageRecords，赋给dishDtoPage
        List<SetmealDto>  setmealDtoRecords = pageInfoRecords.stream().map((item) -> {
            //创建setmealDto对象，用来扩展好多表数据，统一返回给前端
            SetmealDto setmealDto = new SetmealDto();

            //让setmealDto先拷贝上没有套餐分类category的普通公共属性
            BeanUtils.copyProperties(item,setmealDto);

            //然后根据pageInfoRecords里的分类id得到分类id对应的套餐分类category
            Long categoryId = item.getCategoryId();
            Category category = categoryService.getById(categoryId);

            //再让setmealDto扩展加上菜品分类
            setmealDto.setCategoryName(category.getName());

            //最后返回完整的setmealDto
            return setmealDto;
        }).collect(Collectors.toList()); //最终收集成集合给dishDtoPageRecords

        //前面把原来pageInfo中的除records以外的标志属性拷贝上了，现在把完整的records再装上
        setmealDtoPage.setRecords(setmealDtoRecords);

        return R.success(setmealDtoPage);
    }


    /**
     * 删除套餐(多表)
     * @param ids
     * @return
     */
    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids){  //必须带参数才执行方法加@RequestParam，否则带不带参数都执行
        log.info("ids:{}",ids);

        setmealService.removeWithDish(ids);
        return R.success("套餐删除成功！");
    }


    /**
     * 套餐停售起售 菜品停售套餐不能启售，套餐启售菜品不能停售已经考虑到并完成
     * @param ids
     * @return
     */
    @PostMapping("/status/0")
    public R<String> offSale(Long ids){
        Setmeal setmeal = setmealService.getById(ids);
        setmeal.setStatus(0);
        setmealService.updateById(setmeal);
        return R.success("菜品停售成功！");
    }
    @PostMapping("/status/1")
    public R<String> onSale(Long ids){
        Setmeal setmeal = setmealService.getById(ids);
        setmeal.setStatus(1);
        //起售套餐
        setmealService.updateById(setmeal);

        //根据套餐id得到所有相关联的 套餐菜品记录条
        LambdaQueryWrapper<SetmealDish> lqw = new LambdaQueryWrapper<>();
        lqw.eq(SetmealDish::getSetmealId,ids);
        List<SetmealDish> setmealDishes = setmealDishService.list(lqw);
        //将每条 套餐菜品条 对应的菜品起售
        for(SetmealDish setmealDish : setmealDishes){
            //得到对应菜品id
            Long dishId = setmealDish.getDishId();
            //根据菜品id得到对应菜品
            Dish dish = dishService.getById(dishId);
            //起售对应菜品
            dish.setStatus(1);
            dishService.updateById(dish);
        }
        return R.success("菜品启售成功！");
    }


    /**
     * 查询指定套餐分类下的所有套餐
     * @param setmeal
     * @return
     */
    @GetMapping("/list")
    public R<List<Setmeal>> list(Setmeal setmeal){
        //条件构造器
        LambdaQueryWrapper<Setmeal> lqw = new LambdaQueryWrapper<>();
        //添加查询条件，根据套餐分类id
        lqw.eq(setmeal.getCategoryId()!=null,Setmeal::getCategoryId,setmeal.getCategoryId());//根据菜品分类id
        lqw.eq(Setmeal::getStatus,1);//只查起售状态的
        lqw.orderByDesc(Setmeal::getUpdateTime);

        //查套餐，一个套餐id可能对应多个套餐，如商务套餐下有 商务套餐A、商务套餐B
        List<Setmeal> setmealList = setmealService.list(lqw);

        return R.success(setmealList);
    }


    /**
     * 根据套餐id得到套餐下所有菜品
     * @param setmealId
     * @return
     */
    @GetMapping("/dish/{setmealId}")
    public R<List<DishDto>> openSetmeal(@PathVariable Long setmealId){
        //根据套餐id拿到套餐菜品记录条集合
        LambdaQueryWrapper<SetmealDish> lqw = new LambdaQueryWrapper<>();
        lqw.eq(SetmealDish::getSetmealId,setmealId);
        List<SetmealDish> setmealDishes = setmealDishService.list(lqw);

        //根据套餐菜品记录条拿到关联菜品id集合
        List<Long> dishIds = new ArrayList<>();
        for(SetmealDish setmealDish:setmealDishes){
            dishIds.add(setmealDish.getDishId());
        }

        //根据菜品id集合拿到所有对应菜品集合
        LambdaQueryWrapper<Dish> lqw2 = new LambdaQueryWrapper<>();
        lqw2.in(Dish::getId,dishIds);
        List<Dish> dishList = dishService.list(lqw2);

        List<DishDto>  dishDtoList = dishList.stream().map((item) -> {
            //创建dishDto对象，用来扩展好口味数据，统一返回给前端
            DishDto dishDto = new DishDto();

            //让dishDto先拷贝上没有菜品口味dishFlavor的普通公共属性
            BeanUtils.copyProperties(item,dishDto);

            //然后根据dishList里的菜品id得到菜品id对应的菜品口味dishFlavor
            Long dishId = item.getId();
            LambdaQueryWrapper<DishFlavor> lqw3 = new LambdaQueryWrapper<>();
            lqw3.eq(DishFlavor::getDishId,dishId);
            List<DishFlavor> dishFlavors = dishFlavorService.list(lqw3);


            //再让dishDto扩展加上菜品分类
            dishDto.setFlavors(dishFlavors);

            //最后返回完整的dishDto
            return dishDto;
        }).collect(Collectors.toList()); //最终收集成集合给dishDtoPageRecords
        return R.success(dishDtoList);

    }
}
