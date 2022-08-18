package com.xgy.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xgy.reggie.common.R;
import com.xgy.reggie.entity.User;
import com.xgy.reggie.service.UserService;
import com.xgy.reggie.utils.SMSUtils;
import com.xgy.reggie.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;


    /**
     * 发送手机验证码短信
     * @param user
     * @return
     */
    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user, HttpSession session){
        //获取手机号
        String phone = user.getPhone();
        System.out.println("前端传过来的手机号："+user.getPhone());
        if(StringUtils.isNotEmpty(phone)){
            //生成随机的4位验证码
            String code = ValidateCodeUtils.generateValidateCode(4).toString();
            log.info("code={}",code);

            //调用阿里云的短信API发送短信
            SMSUtils.sendMessage("瑞吉外卖","SMS_248740642",phone,code);

            //将生成的验证码保存到session中，方便校验用户输入的验证码
            session.setAttribute(phone,code);

            return R.success("手机验证码短信发送成功！");
        }

        return R.error("手机验证码短信发送失败！");
    }



    /**
     * 移动端用户登录
     * @param user
     * @return
     */
    @PostMapping("/login")
    public R<User> login(@RequestBody Map map, HttpSession session){
        log.info(map.toString());

        //获取手机号
        String phone = map.get("phone").toString();

        //获取验证码
        String code = map.get("code").toString();

        //从session中获取已保存的验证码
        Object codeInSession = session.getAttribute(phone);  //注意这里是根据phone键找，不是"phone"
        System.out.println("codeInSession:"+codeInSession);

        //进行验证码比对(页面提交的 和 session保存的比对)，如果比对成功，说明登录成功
        if(codeInSession!=null && codeInSession.equals(code)){
            //登录成功后，判断当前手机号对应的用户是否为新用户，如果是，就自动完成注册
            LambdaQueryWrapper<User> lqw = new LambdaQueryWrapper<>();
            lqw.eq(User::getPhone,phone);
            User user = userService.getOne(lqw);
            if(user == null){
                user = new User();
                user.setPhone(phone);
                user.setStatus(1);  //设不设都行，数据库有默认值
                userService.save(user);
            }
            //不是新用户就不用注册
            session.setAttribute("user",user.getId());
            return R.success(user);
        }

        return R.error("登录失败！！");
    }


    /**
     * 员工退出
     * @param request
     * @return
     */
    @PostMapping("/loginout")
    public R<String> logout(HttpServletRequest request){
        //清理Session中保存的当前登录员工的id
        request.getSession().removeAttribute("user");
        return R.success("退出成功！");
    }
}
