package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.*;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 菜品管理
 */
@Slf4j
@RestController
@RequestMapping("/dish")
public class DishController {

    @Autowired
    private DishService dishService;

    @Autowired
    private DishFlavorService dishFlavorService; //菜品口味

    @Autowired
    private CategoryService categoryService; //分类

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 菜品新增
     * @param dishDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto){
        log.info("菜品新增:  {}",dishDto.toString());

        dishService.saveWithFlavor(dishDto);

        //局部删除
        String key = "dish_"+dishDto.getCategoryId()+"_1";
        redisTemplate.delete(key);

        return R.success("新增菜品成功");
    }

    /**
     * 菜品信息分页
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name){
        log.info("page = {},pagesize = {},name = {}",page,pageSize,name);
        //构造分页构造器
        Page<Dish> pageInfo = new Page<>(page,pageSize);
        //1.
        Page<DishDto> dtoPage = new Page<>();

        //构造条件查询器
        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        //添加过滤条件 判断name 是否为空 查询条件
        lambdaQueryWrapper.like(StringUtils.hasText(name),Dish::getName,name);  //返回null  org.springframework.util.StringUtils
        //添加排序条件
        lambdaQueryWrapper.orderByDesc(Dish::getUpdateTime);


        //执行查询
        dishService.page(pageInfo, lambdaQueryWrapper);

        //2.对象拷贝
        BeanUtils.copyProperties(pageInfo,dtoPage,"records"); //records不拷贝 因为 records是list<Dish> 的内容

        //3.自己处理records ===> 6.因为list 为空  要基于records 进行处理
        List<Dish> records = pageInfo.getRecords();

        //7. 最终
        List<DishDto> list =
                records.stream().map((item) -> {  //6.1 item => Dish  list集合中的每一个菜品对象

            DishDto dishDto = new DishDto(); //6.5 DishDto对象 => 给对象赋值

            BeanUtils.copyProperties(item,dishDto); // 6.7 copy操作 普通属性已copy

            Long categoryId = item.getCategoryId(); //6.2 分类id  通过分类id 得到分类表
            Category category = categoryService.getById(categoryId); // 6.3 分类对象 目的:要分类的名称

            if (category != null){
                String categoryName = category.getName();  //6.4 分类名称 => 需要一个DishDto对象 存储
                dishDto.setCategoryName(categoryName); // 6.6 赋值 => 对象里面只有categoryName属性,还需把item里的值copy到dishDto对象
            }

            return dishDto; //6.8 返回dto对象 然后将这组方法 返回给 4.
        }).collect(Collectors.toList());

        //4.处理成下面类型
        //List<DishDto> list = null;

        //5.再给 dtoPage对象赋值
        dtoPage.setRecords(list);


        return R.success(dtoPage);
    }


    /**
     * 根据id  查询
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<DishDto> queryDishById(@PathVariable Long id){

        DishDto dishDto = dishService.getByIdWithFlavor(id);

        return R.success(dishDto);
    }

    /**
     * 修改菜品 和菜品口味
     * @param dishDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto){

        dishService.updateWithFlavor(dishDto);

        //全局清理
        Set keys = redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);

        return R.success("修改菜品成功");
    }

    /**
     * 查菜品列表的接口  根据名字 id
     * @param dish
     * @return
     */
    /*@GetMapping("/list")
    public R<List<Dish>> queryDishList(Dish dish){

        log.info("根据条件查询菜品数据.....");

        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(dish.getName() != null,Dish::getName,dish.getName()); // By name

        queryWrapper.eq(dish.getCategoryId() != null,Dish::getCategoryId,dish.getCategoryId()); // By CategoryId
        //排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        //菜品状态为起售的
        queryWrapper.eq(Dish::getStatus,1);

        List<Dish> list = dishService.list(queryWrapper);

        return R.success(list);
    }*/
    @GetMapping("/list")
    public R<List<DishDto>> queryDishList(Dish dish){
        log.info("根据条件查询菜品数据.....");
        List<DishDto> dishDtoList = null;

        String key = "dish_"+dish.getCategoryId()+"_"+dish.getStatus(); // dish_122312321548494_1
        //先从Redis中获取缓存数据 直接返回  key 动态获取
        dishDtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);
        if (dishDtoList != null){
            return R.success(dishDtoList);
        }
        // 不存在 再查询数据库 返回

        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(dish.getName() != null,Dish::getName,dish.getName()); // By name

        queryWrapper.eq(dish.getCategoryId() != null,Dish::getCategoryId,dish.getCategoryId()); // By CategoryId
        //排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        //菜品状态为起售的
        queryWrapper.eq(Dish::getStatus,1);

        List<Dish> list = dishService.list(queryWrapper);

         dishDtoList = list.stream().map((item) -> {
            DishDto dishDto = new DishDto(); //6.5 DishDto对象 => 给对象赋值

            BeanUtils.copyProperties(item,dishDto); // 6.7 copy操作 普通属性已copy

            Long categoryId = item.getCategoryId(); //6.2 分类id  通过分类id 得到分类表
            Category category = categoryService.getById(categoryId); // 6.3 分类对象 目的:要分类的名称

            if (category != null){
                String categoryName = category.getName();  //6.4 分类名称 => 需要一个DishDto对象 存储
                dishDto.setCategoryName(categoryName); // 6.6 赋值 => 对象里面只有categoryName属性,还需把item里的值copy到dishDto对象
            }

            //获取falvors  对象
            Long dishId = item.getId();
            LambdaQueryWrapper<DishFlavor> qw = new LambdaQueryWrapper<>();
            qw.eq(DishFlavor::getDishId,dishId);
            List<DishFlavor> flavors = dishFlavorService.list(qw);
            dishDto.setFlavors(flavors);


            return dishDto; //6.8 返回dto对象 然后将这组方法 返回给 4.

        }).collect(Collectors.toList());

        redisTemplate.opsForValue().set(key,dishDtoList,60, TimeUnit.MINUTES);

        return R.success(dishDtoList);
    }

    /**
     * 删除菜品
     */
    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids){
        log.info("dish 删除 : {}",ids);
        dishService.removeWithDish(ids);

        //全局清理
        Set keys = redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);

        return R.success("菜品数据删除成功");
    }

    /**
     * 修改或批量修改菜品的停售启售状态
     * @param stu
     * @param list
     * @return
     */
    @PostMapping("status/{stu}")
    public R<String> modify(@PathVariable Integer stu,@RequestParam("ids") List<Long> list){

        for (Long ids:list) {
            Dish dish = dishService.getById(ids);
            dish.setStatus(stu);
            dishService.updateById(dish);
        }

        //全局清理
        Set keys = redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);
        return R.success("套餐状态修改成功");
    }
}
