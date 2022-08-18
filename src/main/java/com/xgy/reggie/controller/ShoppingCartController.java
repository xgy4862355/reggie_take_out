package com.xgy.reggie.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xgy.reggie.common.BaseContext;
import com.xgy.reggie.common.R;
import com.xgy.reggie.entity.DishFlavor;
import com.xgy.reggie.entity.ShoppingCart;
import com.xgy.reggie.service.DishFlavorService;
import com.xgy.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;


@Slf4j
@RestController
@RequestMapping("/shoppingCart")
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private DishFlavorService dishFlavorService;

    /**
     * 添加购物车
     * @param shoppingCart
     * @return
     */
    @PostMapping("/add")
    public R<ShoppingCart> add(@RequestBody ShoppingCart shoppingCart){
        log.info("购物车数据：{}",shoppingCart);

        //设置用户id，指定当前是哪个用户的购物车数据
        Long currentId = BaseContext.getCurrentId();
        shoppingCart.setUserId(currentId);

        //查询当前菜品或者套餐，是否在购物车中
        Long dishId = shoppingCart.getDishId();
        LambdaQueryWrapper<ShoppingCart> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ShoppingCart::getUserId,currentId);

        if(dishId != null){
            //添加到购物车的是菜品
            lqw.eq(ShoppingCart::getDishId,dishId);
        }else{
            //添加到购物车的是套餐
            lqw.eq(ShoppingCart::getSetmealId,shoppingCart.getSetmealId());
        }
        ShoppingCart cartOne = shoppingCartService.getOne(lqw);

        //如果已经存在，在原来的数量基础上+1
        if(cartOne!=null){
            Integer number = cartOne.getNumber();
            cartOne.setNumber(number+1);
            shoppingCartService.updateById(cartOne);
        }else{
            //如果不存在，则直接添加到购物车，数量默认是1
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartService.save(shoppingCart);
            cartOne = shoppingCart; //次数id已经雪花生成并赋值
        }

        return R.success(cartOne);
    }


    /**
     * 查看购物车
     * @return
     */
    @GetMapping("/list")
    public R<List<ShoppingCart>> list(){
        log.info("查看购物车");
        LambdaQueryWrapper<ShoppingCart> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ShoppingCart::getUserId,BaseContext.getCurrentId());
        lqw.orderByAsc(ShoppingCart::getCreateTime); //根据创建时间升序排

        List<ShoppingCart> list = shoppingCartService.list(lqw);

        return R.success(list);
    }


    /**
     * 清空购物车
     * @return
     */
    @DeleteMapping("/clean")
    public R<String> clean(){
        LambdaQueryWrapper<ShoppingCart> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ShoppingCart::getUserId,BaseContext.getCurrentId());
        shoppingCartService.remove(lqw);

        return R.success("购物车清空成功！");
    }


    /**
     * 减少购物车菜品或套餐(多表)
     * @param shoppingCart
     * @return
     */
    @PostMapping("/sub")
    @Transactional
    public R<String> sub(@RequestBody ShoppingCart shoppingCart){
        Long dishId = shoppingCart.getDishId();
        if(dishId!=null){
            //传进来的是删除菜品，删之前查一下该菜品数量
            LambdaQueryWrapper<ShoppingCart> lqw = new LambdaQueryWrapper<>();
            lqw.eq(ShoppingCart::getUserId,BaseContext.getCurrentId());
            lqw.eq(ShoppingCart::getDishId,dishId);
            ShoppingCart carOne = shoppingCartService.getOne(lqw);
            if(carOne.getNumber() > 1){
                //数量大于1的 减少数量即可
                carOne.setNumber(carOne.getNumber()-1);
                shoppingCartService.updateById(carOne);
            }else{
                //数量小于等于1的直接删除
                shoppingCartService.remove(lqw);
            }
            return R.success("菜品删除成功");
        }else{
            //传进来的是删除套餐
            LambdaQueryWrapper<ShoppingCart> lqw = new LambdaQueryWrapper<>();
            lqw.eq(ShoppingCart::getUserId,BaseContext.getCurrentId());
            lqw.eq(ShoppingCart::getSetmealId,shoppingCart.getSetmealId());
            ShoppingCart carOne = shoppingCartService.getOne(lqw);
            if(carOne.getNumber() > 1){
                //数量大于1的 减少数量即可
                carOne.setNumber(carOne.getNumber()-1);
                shoppingCartService.updateById(carOne);
            }else{
                //数量小于等于1的直接删除
                shoppingCartService.remove(lqw);
            }
            return R.success("套餐删除成功");
        }
    }
}

