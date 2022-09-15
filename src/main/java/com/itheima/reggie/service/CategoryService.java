package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.entity.Category;

public interface CategoryService extends IService<Category> {

    /**
     * 根据id 删除分类 ,但删除前要判断 和 dish(菜品表) setmeal(套餐表)  是否关联
     * @param id
     */
    void remove(Long id);
}
