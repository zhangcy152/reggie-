package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Employee;
import com.itheima.reggie.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
//import org.apache.commons.lang.StringUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/employee")
public class EmployeeController {
    @Autowired
    private EmployeeService employeeService;

    /**
     * 员工登录操作登录操作
     * @param request 如果登录成功,将数据返回session一份
     * @param employee
     * @return
     */
    @PostMapping("/login")
    public R<Employee> login(HttpServletRequest request, @RequestBody Employee employee){

        //1.将页面提交的密码password进行md5加密处理
        String password = employee.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        //2.根据页面提交的用户名username查询数据库
        String username = employee.getUsername();
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Employee::getUsername, username);
        Employee emp = employeeService.getOne(wrapper);

        //3.如果没有查询到则返回登录失败结果
        if (emp == null){
            return R.error("登陆失败");
        }

        //4.密码对比,如果不一致则返回登录失败结果
        //if (!password.equals(emp.getPassword())){  //这样写不确定 password 是否为空 equals方法对象不能为空或null
        if (!emp.getPassword().equals(password)){
                return R.error("登录失败");
        }

        //5.查看员工状态,如果为已禁用状态,则返回员工已禁用结果
        if (emp.getStatus() == 0){
            return R.error("员工已禁用");
        }

        //6.登录成功,将员工id存入session并返回登录成功结果
        request.getSession().setAttribute("employee",emp.getId());

        return R.success(emp);
    }

    /**
     *  员工退出
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public R<String> logout(HttpServletRequest request){
        //1.清理session中的用户id
        request.getSession().removeAttribute("employee");
        //2.返回结果
        return R.success("退出成功");
    }

    /**
     * 新增员工
     * @param employee
     * @return
     */
    @PostMapping
    public R<String> save(HttpServletRequest request,@RequestBody Employee employee){
        log.info("新增员工,员工信息 :  {}",employee.toString());
        //设置初始密码123456  需要进行md5加密
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));

        //设置创建时间 创建人
//        employee.setCreateTime(LocalDateTime.now());
//        employee.setUpdateTime(LocalDateTime.now());
//
//        Long empId = (Long) request.getSession().getAttribute("employee");
//        employee.setCreateUser(empId);
//        employee.setUpdateUser(empId);

        //调用service层方法
        employeeService.save(employee);

        return R.success("新增员工成功");
    }

    /**
     * 员工信息分页
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page,int pageSize,String name){
        log.info("page = {},pagesize = {},name = {}",page,pageSize,name);
        //构造分页构造器
        Page pageInfo = new Page(page,pageSize);
        //构造条件查询器
        LambdaQueryWrapper<Employee> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        //添加过滤条件 判断name 是否为空 查询条件
        // 注意导的包为 import org.apache.commons.lang.StringUtils;
        //lambdaQueryWrapper.like(StringUtils.isNotEmpty(name),Employee::getName,name);
        lambdaQueryWrapper.like(StringUtils.hasText(name),Employee::getName,name);  //返回null  org.springframework.util.StringUtils
        //添加排序条件
        lambdaQueryWrapper.orderByDesc(Employee::getUpdateTime);

        //执行查询
         employeeService.page(pageInfo, lambdaQueryWrapper);

        return R.success(pageInfo);
    }

    /**
     * 修改员工状态
     * @param employee
     * @return
     */
    @PutMapping
    public R<String> update(HttpServletRequest request,@RequestBody Employee employee){
        log.info(employee.toString());

//        employee.setUpdateTime(LocalDateTime.now());
//
//        Long empId = (Long)request.getSession().getAttribute("employee");
//        employee.setUpdateUser(empId);

        //执行方法
        employeeService.updateById(employee);

        return R.success("员工信息修改成功");
    }

    /**
     * 根据id 查询员工信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<Employee> queryEmployeeById(@PathVariable Long id){
       log.info("根据 id 查询员工信息...");

        Employee empById = employeeService.getById(id);

        /*if (empById != null){
        return R.success(empById);
        }

        return R.error("没有查询到该员工信息");*/
        return empById != null ? R.success(empById) : R.error("没有查询到该员工信息");
    }
}
