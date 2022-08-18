package com.xgy.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xgy.reggie.common.BaseContext;
import com.xgy.reggie.common.R;
import com.xgy.reggie.entity.Employee;
import com.xgy.reggie.entity.Orders;
import com.xgy.reggie.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;


    /**
     * 提交订单
     * @param orders
     * @return
     */
    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders){
        log.info("订单数据：{}",orders);

        orderService.submit(orders);
        return R.success("下单成功！");
    }


    /**
     * 分页查询用户已下订单
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/userPage")
    public R<Page> userPage(Integer page, Integer pageSize){
        //构造分页构造器 (MP) ，配合之前配置的MybatisPlusConfig中的分页插件生效
        Page pageInfo = new Page(page,pageSize);

        //构造条件构造器  (MP)
        LambdaQueryWrapper<Orders> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Orders::getUserId, BaseContext.getCurrentId());
        //添加一个排序条件
        lqw.orderByDesc(Orders::getCheckoutTime);

        //执行查询
        orderService.page(pageInfo, lqw); //MP会将查询结果自动封装到pageInfo中
        return R.success(pageInfo);
    }

}
