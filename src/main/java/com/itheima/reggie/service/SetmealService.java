package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Setmeal;

import java.util.List;


public interface SetmealService extends IService<Setmeal> {

    /**
     *     保存套餐的基本信息
     *     保存套餐和菜品的关联信息
     * @param setmealDto
     */
    void saveWithDish(SetmealDto setmealDto);

    /**
     * 更新套餐信息 同时更新套餐菜品信息
     * @param setmealDto
     */
    void updateWithDish(SetmealDto setmealDto);

    /**
     * 删除id
     * @param ids
     */
    void removeWithDish(List<Long> ids);
}
