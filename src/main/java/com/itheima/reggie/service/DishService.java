package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;

import java.util.List;

public interface DishService extends IService<Dish> {

    //新增菜品 关联口味数据 操作两张表
    void saveWithFlavor(DishDto dishDto);

    //根据id 查询菜品和口味信息
    DishDto getByIdWithFlavor(Long id);

    //更新菜品信息 同时更新口味
    void  updateWithFlavor(DishDto dishDto);

    //删除菜品
    void removeWithDish(List<Long> ids);
}
