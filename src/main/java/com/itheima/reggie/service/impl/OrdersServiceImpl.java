package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.entity.*;
import com.itheima.reggie.mapper.OrdersMapper;
import com.itheima.reggie.service.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements OrdersService {

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private UserService userService;

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private OrderDetailService orderDetailService;
    
    /**
     * 用户下单  三张表
     * @param orders
     */
    @Override
    @Transactional
    public void submit(Orders orders) {
        //获取当前用户id
        Long currentId = BaseContext.getCurrentId();


        //查询当前用户购物车数据 根据用户id
        List<ShoppingCart> cartList = shoppingCartService.list(new LambdaQueryWrapper<ShoppingCart>()
                .eq(ShoppingCart::getUserId, currentId));

        if (cartList == null || cartList.size() == 0){
            throw new CustomException("购物车为空,不能下单");
        }

        long orderId = IdWorker.getId();//订单号

        //amount 实收金额
        AtomicInteger amount = new AtomicInteger(0);
        //订单明细集合
        List<OrderDetail> orderDetails = cartList.stream().map((item) -> {
            OrderDetail orderDetail = new OrderDetail();
            //BeanUtils.copyProperties(item,orderDetail);  没试
            orderDetail.setOrderId(orderId);
            orderDetail.setNumber(item.getNumber());
            orderDetail.setDishFlavor(item.getDishFlavor());
            orderDetail.setDishId(item.getDishId());
            orderDetail.setSetmealId(item.getSetmealId());
            orderDetail.setName(item.getName());
            orderDetail.setImage(item.getImage());
            orderDetail.setAmount(item.getAmount());
            amount.addAndGet(item.getAmount().multiply(new BigDecimal(item.getNumber())).intValue());

            //处理金额
            /*BigDecimal bigDecimal = item.getAmount().multiply(BigDecimal.valueOf(item.getNumber()));
            orders.setAmount(bigDecimal);
            amount.add(bigDecimal);*/

            return orderDetail;
        }).collect(Collectors.toList());
        /*for (ShoppingCart shoppingCart : cartList) {
            //单价 * 数量 = 单条数据 金额
            BigDecimal bigDecimal = shoppingCart.getAmount().multiply(BigDecimal.valueOf(shoppingCart.getNumber()));
            amount.add(bigDecimal);
        }*/

        //根据用户id  查询出用户的数据
        User user = userService.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getId, currentId));


        //根据地址id(前端传过来的)  查询出地址的数据
        AddressBook addressBook = addressBookService.getOne(new LambdaQueryWrapper<AddressBook>()
                .eq(AddressBook::getId,orders.getAddressBookId()));
        //地址信息可能为空
        if (addressBook == null){
            throw new CustomException("地址信息为空,不能下单");
        }



        //向订单表插入数据 一条数据'
        orders.setId(orderId);
        orders.setUserId(currentId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setCheckoutTime(LocalDateTime.now());
        orders.setStatus(2); //2 待派送
        orders.setNumber(String.valueOf(orderId));    //订单号
        orders.setAmount(new BigDecimal(amount.get()));
        orders.setUserName(user.getName());
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        // 省 市 区  详细地址
        orders.setAddress((addressBook.getProvinceName() == null ? "" : addressBook.getProvinceName())
                + (addressBook.getCityName() == null ? "" : addressBook.getCityName())
                + (addressBook.getDistrictName() == null ? "" : addressBook.getDistrictName())
                + (addressBook.getDetail() == null ? "" : addressBook.getDetail()));

        super.save(orders);

        //向订单明细表插入数据 , 多条数据
        orderDetailService.saveBatch(orderDetails);
        //清空购物车数据
        shoppingCartService.remove(new LambdaQueryWrapper<ShoppingCart>()
                .eq(ShoppingCart::getUserId,currentId));
    }
}
