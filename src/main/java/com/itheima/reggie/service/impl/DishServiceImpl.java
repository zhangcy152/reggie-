package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.mapper.DishMapper;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private CategoryService categoryService;

    /**
     * 新增菜品,同时保存对应的口味数据
     * @param dishDto
     */
    @Override
    @Transactional
    public void saveWithFlavor(DishDto dishDto) {
        //保存菜品的基本信息到 dish表
        this.save(dishDto);

        Long dishId = dishDto.getId(); //菜品id

        //菜品口味
        List<DishFlavor> flavors = dishDto.getFlavors(); //口味集合
        flavors.stream().map((item)->{  //集合遍历 stream流 比foreach  更高效
           item.setDishId(dishId);
           return item;
        }).collect(Collectors.toList());

        //保存菜品口味数据到 口味表
        dishFlavorService.saveBatch(flavors);
    }

    /**
     * 根据id 查询菜品和口味信息
     * @param id
     * @return
     */
    @Override
    public DishDto getByIdWithFlavor(Long id) {

        //创建一个数据传输对象
        DishDto dishDto = new DishDto();
        //获取菜品基本信息
        Dish dish = this.getById(id);

        //copy  把dish的属性copy给dishDto   不用写什么set方法
        BeanUtils.copyProperties(dish,dishDto);

        //创建条件查询器  查询口味信息
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId,dish.getId());

        List<DishFlavor> flavors = dishFlavorService.list(queryWrapper);



        //保存数据到 disDto对象
        dishDto.setFlavors(flavors);

        //category name
//        Long categoryId = dish.getCategoryId();
//        Category category = categoryService.getById(categoryId);
//
//        dishDto.setCategoryName(category.getName());


        return dishDto;
    }

    /**
     * 更新菜品信息 同时更新口味
     * @param dishDto
     */
    @Override
    public void updateWithFlavor(DishDto dishDto) {
        //更新dish表基本信息
        this.updateById(dishDto);

        //清理当前菜品对应口味数据  -- dish_flavor表的delete操作
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId,dishDto.getId());

        dishFlavorService.remove(queryWrapper);

        //添加当前提交过来的口味数据 -- dish_flavor表的insert操作
        List<DishFlavor> flavors = dishDto.getFlavors();
        //给dish_flavor表重新 添加 dish_id
        flavors.stream().map((item) -> {
            item.setDishId(dishDto.getId());
            return item;
        }).collect(Collectors.toList());

        dishFlavorService.saveBatch(flavors);
    }

    /**
     * 删除菜品  判断dishFlavor表是否有关联
     * @param ids
     */
    @Override
    @Transactional
    public void removeWithDish(List<Long> ids) {
        //查看状态是否可以删除
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Dish::getId,ids);
        queryWrapper.eq(Dish::getStatus,1);  //起售 不可删除

        int count = super.count(queryWrapper);

        if (count > 0){
            throw new CustomException("菜品正在售卖中，不能删除");
        }

        //可以删除 先删除菜品表数据
        super.removeByIds(ids);

        //再删除 口味表 数据

        LambdaQueryWrapper<DishFlavor> dishFlavorLambdaQueryWrapper = new LambdaQueryWrapper<>();
        dishFlavorLambdaQueryWrapper.in(DishFlavor::getDishId,ids);

        dishFlavorService.remove(dishFlavorLambdaQueryWrapper);

    }

}
