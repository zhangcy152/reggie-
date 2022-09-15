package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.User;
import com.itheima.reggie.service.UserService;
import com.itheima.reggie.utils.SMSUtils;
import com.itheima.reggie.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Map;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 发送验证码
     * @param user
     * @param session
     * @return
     */
    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user, HttpSession session){
        //获取手机号
        String phone = user.getPhone();

        if (StringUtils.isNotEmpty(phone)){
            //生成随机的验证码
            String code = ValidateCodeUtils.generateValidateCode(4).toString();
            log.info("code = {}",code);
            //调用阿里云提供的短信服务api完成发送短信
            //SMSUtils.sendMessage("签名","模板code","手机号","动态验证码");


            //需要将生成的验证码保存到Session
            session.setAttribute(phone,code);

            return R.success("手机验证码发送成功");
        }



        return R.error("手机短信验证码发送失败");
    }


    /**
     * 登录
     * @param map
     * @param session
     * @return
     */
    @PostMapping("/login")
    public R<User> login(@RequestBody Map map, HttpSession session){
        log.info(map.toString());

        //获取手机号 验证码
        String phone = map.get("phone").toString();
        String code = map.get("code").toString();

        //从session中获取保存的验证码
        Object sessionCode = session.getAttribute(phone);

        //进行验证码比对
        if (sessionCode != null && sessionCode.equals(code)){
            //如果对比成功证明可以登录
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getPhone,phone);

            User user = userService.getOne(queryWrapper);

            if (user == null){
                //判断当前手机号对应的用户是否为新用户,如果是新用户自动完成注册
                user = new User();
                user.setPhone(phone);
                user.setStatus(1);

                //保存用户
                userService.save(user);

            }
            //过滤器  过滤 session 中是否有user属性  所以要在登陆时给值
            session.setAttribute("user",user.getId());
            return R.success(user);
        }

         return R.error("登陆失败");
    }
}
