package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/setmeal")
public class SetmealController {

    @Autowired
    private SetmealService setmealService;
    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private CategoryService categoryService;

    /**
     * 新增套餐
     * @param setmealDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody SetmealDto setmealDto){
        log.info("套餐新增: {}",setmealDto.toString());

        setmealService.saveWithDish(setmealDto);

        return R.success("新增套餐成功");
    }

    /**
     * 套餐分页信息
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> list(int page,int pageSize,String name){
        log.info("page = {},pagesize = {},name = {}",page,pageSize,name);
        //1.构造分页构造器
        Page<Setmeal> pageInfo = new Page<>(page,pageSize);
        Page<SetmealDto> dtoPage = new Page<>();

        //构造条件查询器
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        //添加过滤条件 判断name 是否为空 查询条件
        queryWrapper.like(StringUtils.hasText(name),Setmeal::getName,name);
        //排序条件
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        //操作数据库
        setmealService.page(pageInfo,queryWrapper);

        //2.对象copy
        BeanUtils.copyProperties(pageInfo,dtoPage,"records");

        //3.处理records Setmeal 对象集合
        List<Setmeal> records = pageInfo.getRecords();
        //4.处理过程
        List<SetmealDto> list = records.stream().map((item) -> {
            //获取分类id 并得到分类对象  得到分类的名字 将分类名字存到SetmealDto 对象
            // 再把集合中的list(Setmeal)属性值copy给新的集合 最后返回
            SetmealDto setmealDto = new SetmealDto();
            BeanUtils.copyProperties(item,setmealDto);

            Long categoryId = item.getCategoryId();
            Category category = categoryService.getById(categoryId);

            if (category != null){
                String categoryName = category.getName();

                setmealDto.setCategoryName(categoryName);
            }

            return setmealDto;

        }).collect(Collectors.toList());

        //5.给dtoPage对象 赋值 处理过的records
        dtoPage.setRecords(list);

        return R.success(dtoPage);
    }

    /**
     * 修改套餐 套餐菜品  同新增为一页面: add.html
     * @param setmealDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody SetmealDto setmealDto){

        //调用service层自己的写的方法
        setmealService.updateWithDish(setmealDto);

        return R.success("修改套餐成功");
    }

    /**
     * 删除
     * @param ids
     * @return
     */
    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids){
        log.info("ids为：{}",ids);

        setmealService.removeWithDish(ids);

        return R.success("套餐数据删除成功");

    }

    /**
     * 修改或批量修改套餐的停售启售状态
     * @param stu
     * @param list
     * @return
     */
    @PostMapping("status/{stu}")
    public R<String> modify(@PathVariable Integer stu,@RequestParam("ids") List<Long> list){

        for (Long ids:list) {
            Setmeal setmeal = setmealService.getById(ids);
            setmeal.setStatus(stu);
            setmealService.updateById(setmeal);
        }

        return R.success("套餐状态修改成功");
    }


    /**
     * 根据条件查询套餐数据
     * @param setmeal
     * @return
     */
    @GetMapping("/list")
    public R<List<Setmeal>> list(Setmeal setmeal){
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(setmeal.getCategoryId() != null,Setmeal::getCategoryId,setmeal.getCategoryId());
        queryWrapper.eq(setmeal.getStatus() != null,Setmeal::getStatus,setmeal.getStatus());
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        List<Setmeal> list = setmealService.list(queryWrapper);

        return R.success(list);
    }

    /**
     * 获取套餐全部菜品
     */
    @GetMapping("/dish/{id}")
    public R<List<SetmealDish>> setMealDishDetails(@PathVariable Long id){ //id值为setmeal_id
        log.info("获取套餐全部菜品");

        //根据id  查询套餐全部菜品
        List<SetmealDish> setmealDishes = setmealDishService.list(new LambdaQueryWrapper<SetmealDish>()
                .eq(SetmealDish::getSetmealId, id));

        return R.success(setmealDishes);

    }

}
