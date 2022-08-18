package com.xgy.reggie.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xgy.reggie.common.BaseContext;
import com.xgy.reggie.common.CustomException;
import com.xgy.reggie.entity.*;
import com.xgy.reggie.mapper.OrderMapper;
import com.xgy.reggie.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Orders> implements OrderService {


    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private UserService userService;

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private OrderDetailService orderDetailService;

    /**
     * 用户下单
     * @param orders
     */
    @Override
    @Transactional
    public void submit(Orders orders) {
        //获得当前用户id
        Long currentId = BaseContext.getCurrentId();

        //得到该下单用户的购物车数据
        LambdaQueryWrapper<ShoppingCart> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ShoppingCart::getUserId,currentId);

        List<ShoppingCart> cartList = shoppingCartService.list(lqw);
        if(cartList==null || cartList.size()==0){
            throw new CustomException("购物车为空，无法下单！");
        }

        //查询用户数据
        User user = userService.getById(currentId);

        //查询地址数据
        Long addressBookId = orders.getAddressBookId();
        AddressBook addressBook = addressBookService.getById(addressBookId);
        if(addressBook==null){
            throw new CustomException("收货地址为空，无法下单！");
        }


        long orderId = IdWorker.getId();  //生成订单号

        //原子类整数，保证线程安全
        AtomicInteger amount = new AtomicInteger(0);
        //遍历购物车数据，插入订单明细表，同时计算总金额
        List<OrderDetail> orderDetailList = cartList.stream().map((item) ->{
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(orderId);
            orderDetail.setNumber(item.getNumber());
            orderDetail.setDishFlavor(item.getDishFlavor());
            orderDetail.setDishId(item.getDishId());
            orderDetail.setSetmealId(item.getSetmealId());
            orderDetail.setName(item.getName());
            orderDetail.setImage(item.getImage());
            orderDetail.setAmount(item.getAmount());
            //累加amount += 单份值 * 份数
            amount.addAndGet(item.getAmount().multiply(new BigDecimal(item.getNumber())).intValue());
            return orderDetail;
        }).collect(Collectors.toList());

        orders.setId(orderId);  //订单id
        orders.setOrderTime(LocalDateTime.now());   //下单时间
        orders.setCheckoutTime(LocalDateTime.now());    //付款时间
        orders.setStatus(2);  //待派送
        orders.setAmount(new BigDecimal(amount.get()));//总金额
        orders.setUserId(currentId);    //用户id
        orders.setNumber(String.valueOf(orderId)); //订单号
        orders.setUserName(user.getName());     //下单名字
        orders.setConsignee(addressBook.getConsignee());  //收货人
        orders.setPhone(addressBook.getPhone());
        orders.setAddress((addressBook.getProvinceName() == null ? "" : addressBook.getProvinceName())
                + (addressBook.getCityName() == null ? "" : addressBook.getCityName())
                + (addressBook.getDistrictName() == null ? "" : addressBook.getDistrictName())
                + (addressBook.getDetail() == null ? "" : addressBook.getDetail()));
        //向订单表插入数据，一条数据
        this.save(orders);

        //向订单明细表插入多条数据
        orderDetailService.saveBatch(orderDetailList);

        //下单完成后，清空该用户的购物车数据
        shoppingCartService.remove(lqw);
    }
}
