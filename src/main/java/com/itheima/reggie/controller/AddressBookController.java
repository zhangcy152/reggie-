package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.AddressBook;
import com.itheima.reggie.service.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 地址簿
 */
@RestController
@RequestMapping("/addressBook")
@Slf4j
public class AddressBookController {

    @Autowired
    private AddressBookService addressBookService;

    /**
     * 新增地址
     */
    @PostMapping
    public R<AddressBook> saveAddress(@RequestBody AddressBook addressBook){
        addressBook.setUserId(BaseContext.getCurrentId()); //获取本地存储的userID;
        log.info("addressBook: {}",addressBook);
        addressBookService.save(addressBook);
        return R.success(addressBook);
    }

    /**
     * 设置默认地址
     *  有唯一性 所以修改之前 现将当前数据的isDefault 改为 0 在单独修改选中数据
     */
    @PutMapping("default")
    public R<AddressBook> setDefault(@RequestBody AddressBook addressBook){
        log.info("设置默认地址: {}",addressBook.getUserId());
        //现将当前用户的默认地址清空
        LambdaUpdateWrapper<AddressBook> wrapper = new LambdaUpdateWrapper<>();  //条件构造器 为 update
        wrapper.eq(AddressBook::getUserId,addressBook.getUserId());
        wrapper.set(AddressBook::getIsDefault,0);

        addressBookService.update(wrapper);

        //设置选中数据为默认地址
        addressBook.setIsDefault(1);
        addressBookService.updateById(addressBook);

        return R.success(addressBook);
    }

    /**
     * 获取默认地址  add-order.html
     */
    @GetMapping("default")
    public R<AddressBook> getDefault(){
        log.info("获取默认地址: ");
        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AddressBook::getUserId,BaseContext.getCurrentId());
        queryWrapper.eq(AddressBook::getIsDefault,1);

        AddressBook defaultAddressBook = addressBookService.getOne(queryWrapper);

        if (defaultAddressBook == null){
            return R.error("没有找到该对象");
        }
        return R.success(defaultAddressBook);
    }

    /**
     * 根据id 查询地址
     */
    @GetMapping("/{id}")
    public R getById(@PathVariable Long id){
        log.info("addressBook 查询根据id: {}",id);
        if (id != null) {
            AddressBook addressBook = addressBookService.getById(id);
            return R.success(addressBook);
        }else {
            return R.error("没有找到该对象");
        }
    }

    /**
     * 删除地址 address-edit.html
     */
    @DeleteMapping
    public R<String> delete(@RequestParam Long ids){
        log.info("删除地址: {}",ids);

        if (ids != null){
            addressBookService.removeById(ids);
            return R.success("地址删除成功");
        }
        return R.error("删除失败");
    }

    /**
     * 修改地址 updateAddressApi address-edit.html
     */
    @PutMapping
    public R<AddressBook> updateAddress (@RequestBody AddressBook addressBook){
        log.info("修改地址: {}",addressBook.toString());

        addressBookService.updateById(addressBook);

        return R.success(addressBook);
    }

    /**
     * 查询指定用户的所有地址 address.html
     */
    @GetMapping("/list")
    public R<List<AddressBook>> list(AddressBook addressBook){
        addressBook.setUserId(BaseContext.getCurrentId());
        log.info("获取所有地址... addressBook: {}",addressBook);

        //条件构造器
        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(addressBook.getUserId() !=  null, AddressBook::getUserId, addressBook.getUserId());
        queryWrapper.orderByDesc(AddressBook::getUpdateTime);

        List<AddressBook> list = addressBookService.list(queryWrapper);
        return R.success(list);
    }

    /**
     * 获取最新地址 addressLastUpdateApi
     */
    @GetMapping("/lastUpdate")
    public R<AddressBook> getLatestAddress(){
        log.info("获取最新的地址");
        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(AddressBook::getUpdateTime);

        List<AddressBook> list = addressBookService.list(queryWrapper);
        AddressBook addressBook = list.stream().findFirst().orElse(null);
        return R.success(addressBook);

    }
}
