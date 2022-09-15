package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.mapper.CategoryMapper;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishService;
import com.itheima.reggie.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Autowired
    private DishService dishService;

    @Autowired
    private SetmealService setmealService;

    /**
     * 根据id 删除分类 ,但删除前要判断 和 dish(菜品表) setmeal(套餐表)  是否关联
     * @param id
     */
    @Override
    public void remove(Long id) {
        //判断两个表中是否含有id
        LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        //添加查询条件 根据id查询
        dishLambdaQueryWrapper.eq(Dish::getCategoryId,id);
        int Ccount = dishService.count(dishLambdaQueryWrapper);

        //查询当前分类是否关联了菜品,如果已经关联,抛出一个业务异常
        if (Ccount > 0){
            //已经关联,抛出一个业务异常
            throw new CustomException("当前分类下关联了菜品,不可删除");
        }

        //查询当前分类是否关联了套餐,如果已经关联,抛出一个业务异常
        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealLambdaQueryWrapper.eq(Setmeal::getCategoryId,id);
        int Scount = setmealService.count(setmealLambdaQueryWrapper);

        if (Scount > 0){
            //已经关联套餐,抛出一个业务异常
            throw new CustomException("当前分类下关联了套餐,不可删除");
        }

        //正常删除分类

        super.removeById(id);


    }
}
