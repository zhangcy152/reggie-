package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.ShoppingCart;
import com.itheima.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/shoppingCart")
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 购物车中添加商品
     * @param shoppingCart
     * @return
     */
    @PostMapping("/add")
    public R<ShoppingCart> addCart(@RequestBody ShoppingCart shoppingCart){
        log.info("购物车添加: {}",shoppingCart.toString());
        //获取当前userid  获悉当前购物车是谁的
        shoppingCart.setUserId(BaseContext.getCurrentId());

        //查询当前菜品或套餐是否在用户购物车中 如果有 直接number+1 不存在 添加到购物车 number默认1
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(ShoppingCart::getUserId,shoppingCart.getUserId()); //注意: userID 不是 id
        queryWrapper.eq(shoppingCart.getDishId() != null,ShoppingCart::getDishId,shoppingCart.getDishId());
        queryWrapper.eq(shoppingCart.getSetmealId() != null ,ShoppingCart::getSetmealId,shoppingCart.getSetmealId());

        /**
         * 老师写法:
         *  上面老师写 是
         *  if(shoppingCart.getDish() != null){
         *      queryWrapper.eq(ShoppingCart::getDishId,shoppingCart.getDishId());
         *  }else{
         *      queryWrapper.eq(ShoppingCart::getSetmealId,shoppingCart.getSetmealId());
         *  }
         *
         */
        ShoppingCart cartServiceOne = shoppingCartService.getOne(queryWrapper);

        if (cartServiceOne != null){
            //已经存在
            Integer number = cartServiceOne.getNumber();
            cartServiceOne.setNumber(number+1);
            cartServiceOne.setCreateTime(LocalDateTime.now());
            shoppingCartService.updateById(cartServiceOne);
        }else {
            //没存在
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartService.save(shoppingCart);
            cartServiceOne = shoppingCart;
        }

        return R.success(cartServiceOne);

        /**
         * 自己写
         */
       /* int count = shoppingCartService.count(queryWrapper);

        if (count > 0){
            ShoppingCart cartDB = shoppingCartService.getById(shoppingCart.getId());
            shoppingCartService.update(new LambdaUpdateWrapper<ShoppingCart>().set(ShoppingCart::getNumber,cartDB.getNumber()+1));
        }else {
            shoppingCart.setNumber(1);
            shoppingCartService.save(shoppingCart);
        }
        return null;*/
    }

    /**
     * 在购物内减少物品
     */
    @PostMapping("/sub")
    public R<ShoppingCart> sub(@RequestBody ShoppingCart shoppingCart){
        log.info("减少商品....");
        //获取当前用户id
        Long currentId = BaseContext.getCurrentId();
        shoppingCart.setUserId(currentId);

        //查询当前菜品或套餐是否在用户购物车中 如果有 直接number-1 当number = 1 则删除
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId,shoppingCart.getUserId());

        if (shoppingCart.getDishId() == null){
            queryWrapper.eq(ShoppingCart::getSetmealId,shoppingCart.getSetmealId());
        }else {
            queryWrapper.eq(ShoppingCart::getDishId,shoppingCart.getDishId());
        }

        ShoppingCart cartServiceOne = shoppingCartService.getOne(queryWrapper);

        if (cartServiceOne.getNumber() == 1 ){
            //如果数量为一 直接删除
            cartServiceOne.setNumber(0);
            shoppingCartService.remove(queryWrapper);
        }else {
            Integer number = cartServiceOne.getNumber();
            cartServiceOne.setNumber(number-1);
            shoppingCartService.updateById(cartServiceOne);
            cartServiceOne = shoppingCart;
        }


        return R.success(cartServiceOne);
    }


    /**
     * 获取购物车内商品的集合
     * @param
     * @return
     */
    @GetMapping("/list")
    public R<List<ShoppingCart>> list(){
        log.info("shoppingCart list....");

        List<ShoppingCart> shoppingCarts = shoppingCartService.list(new LambdaQueryWrapper<ShoppingCart>()
                                    .eq(ShoppingCart::getUserId,BaseContext.getCurrentId())
                                    .orderByDesc(ShoppingCart::getCreateTime));

        return R.success(shoppingCarts);
    }

    /**
     * 清空购物车
     * @return
     */
    @DeleteMapping("/clean")
    public R<String> delete(){
        log.info("清空购物车...");

        shoppingCartService.remove(new LambdaQueryWrapper<ShoppingCart>()
                .eq(ShoppingCart::getUserId,BaseContext.getCurrentId()));

        return R.success("清空购物车成功");
    }
}
