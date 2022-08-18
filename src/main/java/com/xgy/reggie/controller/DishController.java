package com.xgy.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xgy.reggie.common.R;
import com.xgy.reggie.dto.DishDto;
import com.xgy.reggie.entity.*;
import com.xgy.reggie.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜品管理
 */
@Slf4j
@RestController
@RequestMapping("/dish")
public class DishController {
    @Autowired
    private DishService dishService;

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private SetmealService setmealService;

    /**
     * 新增菜品(多表)
     * @param dishDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto){  //封装成dto类进行结束参数
        log.info(dishDto.toString());
        //调用自己扩展的方法操作多表
        dishService.saveWithFlavor(dishDto);
        return R.success("新增菜品成功！");
    }


    /**
     * 菜品分页查询(多表)
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page,int pageSize,String name){
        //构造分页构造器，这里其实是MP帮我们做查询的时候根据参数加了limit而已
        Page<Dish> pageInfo = new Page<>(page,pageSize);

        //条件构造器
        LambdaQueryWrapper<Dish> lqw = new LambdaQueryWrapper<>();

        //添加过滤条件
        lqw.like(name!=null,Dish::getName,name);
        //添加排序条件
        lqw.orderByDesc(Dish::getUpdateTime);

        //执行分页查询
        dishService.page(pageInfo,lqw);

        //原来的分页数据pageInfo中缺少菜品分类属性，扩展创建一个新的分页信息dishDtoPage
        Page<DishDto> dishDtoPage = new Page<>(page,pageSize);

        //对象拷贝，先把原来pageInfo中的除records以外的标志属性拷贝一下
        BeanUtils.copyProperties(pageInfo,dishDtoPage,"records");

        //原来的pageInfoRecords中没有菜品分类属性
        List<Dish> pageInfoRecords = pageInfo.getRecords();

        //再把原来的pageInfoRecords取出来，扩展一下，做新的dishDtoPageRecords，赋给dishDtoPage
        List<DishDto>  dishDtoPageRecords = pageInfoRecords.stream().map((item) -> {
            //创建dishDto对象，用来扩展好多表数据，统一返回给前端
            DishDto dishDto = new DishDto();

            //让dishDto先拷贝上没有菜品分类category的普通公共属性
            BeanUtils.copyProperties(item,dishDto);

            //然后根据pageInfoRecords里的分类id得到分类id对应的菜品分类category
            Long categoryId = item.getCategoryId();
            Category category = categoryService.getById(categoryId);

            //再让dishDto扩展加上菜品分类
            dishDto.setCategoryName(category.getName());

            //最后返回完整的dishDto
            return dishDto;
        }).collect(Collectors.toList()); //最终收集成集合给dishDtoPageRecords

        //前面把原来pageInfo中的除records以外的标志属性拷贝上了，现在把完整的records再装上
        dishDtoPage.setRecords(dishDtoPageRecords);

        return R.success(dishDtoPage);
    }


    /**
     * 根据id查询菜品信息(多表)
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<DishDto> get(@PathVariable Long id){
        //得到扩展好的dto实体
        DishDto dishDto = dishService.getByIdWithFlavor(id);

        return R.success(dishDto);
    }


    /**
     * 更新菜品信息
     * @param dishDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto){
        dishService.updateWithFlavor(dishDto);
        return R.success("菜品信息更新成功！");
    }


    /**
     * 菜品停售起售  菜品停售套餐不能启售，套餐启售菜品不能停售已经考虑到并完成
     * @param ids
     * @return
     */
    @PostMapping("/status/0")
    public R<String> offSale(Long ids){
        Dish dish = dishService.getById(ids);
        //停售菜品
        dish.setStatus(0);

        //根据当前菜品id 去找当前菜品是否有关联到某套餐，若有，套餐也停售
        LambdaQueryWrapper<SetmealDish> lqw = new LambdaQueryWrapper<>();
        lqw.eq(SetmealDish::getDishId,ids);

        //停售当前菜品
        dishService.updateById(dish);

        //得到所有相关联的 套餐菜品记录条
        List<SetmealDish> setmealDishes = setmealDishService.list(lqw);
        //如果数量>0
        if(setmealDishes.size()>0){
            //把关联到的套餐都给停售
            for(SetmealDish setmealDish : setmealDishes){
                //得到相关联的套餐id
                Long setmealId = setmealDish.getSetmealId();
                //得到相关联的套餐
                Setmeal setmeal = setmealService.getById(setmealId);
                //将关联套餐停售
                setmeal.setStatus(0);
                setmealService.updateById(setmeal);
            }
        }

        return R.success("菜品停售成功！");
    }
    @PostMapping("/status/1")
    public R<String> onSale(Long ids){
        Dish dish = dishService.getById(ids);
        dish.setStatus(1);
        dishService.updateById(dish);
        return R.success("菜品起售成功！");
    }


    /**
     * 根据条件查询对应的菜品数据
     * @param dish
     * @return
     */
    /*@GetMapping("/list")
    public R<List<Dish>> list(Dish dish){
        //条件构造器
        LambdaQueryWrapper<Dish> lqw = new LambdaQueryWrapper<>();
        //添加查询条件
        lqw.eq(dish.getCategoryId()!=null,Dish::getCategoryId,dish.getCategoryId());//根据菜品分类id
        lqw.like(dish.getName()!=null,Dish::getName,dish.getName());//根据菜品名字
        lqw.eq(Dish::getStatus,1);//只查起售状态的
        //添加排序条件
        lqw.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        //查集合
        List<Dish> list = dishService.list(lqw);

        return R.success(list);
    }*/




    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish){
        //条件构造器
        LambdaQueryWrapper<Dish> lqw = new LambdaQueryWrapper<>();
        //添加查询条件
        lqw.eq(dish.getCategoryId()!=null,Dish::getCategoryId,dish.getCategoryId());//根据菜品分类id
        lqw.like(dish.getName()!=null,Dish::getName,dish.getName());//根据菜品名字
        lqw.eq(Dish::getStatus,1);//只查起售状态的
        //添加排序条件
        lqw.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        //查集合
        List<Dish> dishList = dishService.list(lqw);



        List<DishDto>  dishDtoList = dishList.stream().map((item) -> {
            //创建dishDto对象，用来扩展好多表数据，统一返回给前端
            DishDto dishDto = new DishDto();

            //让dishDto先拷贝上没有菜品口味dishFlavor的普通公共属性
            BeanUtils.copyProperties(item,dishDto);

            //然后根据dishList里的菜品id得到菜品id对应的菜品口味dishFlavor
            Long dishId = item.getId();
            LambdaQueryWrapper<DishFlavor> lqw2 = new LambdaQueryWrapper<>();
            lqw2.eq(DishFlavor::getDishId,dishId);
            List<DishFlavor> dishFlavors = dishFlavorService.list(lqw2);


            //再让dishDto扩展加上菜品分类
            dishDto.setFlavors(dishFlavors);

            //最后返回完整的dishDto
            return dishDto;
        }).collect(Collectors.toList()); //最终收集成集合给dishDtoPageRecords
        return R.success(dishDtoList);
    }
}
