package com.xgy.reggie.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xgy.reggie.common.R;
import com.xgy.reggie.entity.Employee;
import com.xgy.reggie.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/employee")
public class EmployeeController {

    @Autowired
    private EmployeeService emploeeService;

    /**
     * 员工登录
     * @param request
     * @param employee
     * @return
     */
    @PostMapping("/login")
    public R<Employee> login(HttpServletRequest request,@RequestBody Employee employee){
        //1、将页面提交的密码password进行md5加密处理
        String password = employee.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        //2、根据页面提交的用户名 username查询数据库
        LambdaQueryWrapper<Employee> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Employee::getUsername,employee.getUsername());
        Employee emp = emploeeService.getOne(lqw);

        //3、如果没有查询到返回登录失败结果
        if(emp == null){
            return R.error("登录失败");
        }

        //4、若存在此用户，则开始密码比对，如果不一致则返回登录失败结果
        if(!emp.getPassword().equals(password)){
            return R.error("登录失败");
        }

        //5、若比对成功，再查看一下当前登录账号的状态，如果已为禁用状态，则返回账号已禁用结果
        if(emp.getStatus() == 0){
            return R.error("账号已禁用");
        }

        //6、若账号未禁用，则登录成功，将员工id放入Session中并返回登录成功结果
        request.getSession().setAttribute("employee",emp.getId());
        return R.success(emp);
    }

    /**
     * 员工退出
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public R<String> logout(HttpServletRequest request){
        //清理Session中保存的当前登录员工的id
        request.getSession().removeAttribute("employee");
        return R.success("退出成功！");
    }


    /**
     * 新增员工
     * @param employee
     * @return
     */
    @PostMapping
    public R<String> save(HttpServletRequest request, @RequestBody Employee employee){
        log.info("新增员工，员工信息：{}",employee.toString());

        //设置初试密码123456，需要进行md5加密
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));

        //后台记录创建时间
        //employee.setCreateTime(LocalDateTime.now());
        //后台记录更新时间
        //employee.setUpdateTime(LocalDateTime.now());
        //记录当前创建操作发起人id
        //Long empId = (Long)request.getSession().getAttribute("employee");
        //employee.setCreateUser(empId);
        //记录当前更新操作发起人id
        //employee.setUpdateUser(empId);

        //long id = Thread.currentThread().getId();
        //log.info("线程id为："+id);

        //创建员工，添加至数据库
        emploeeService.save(employee);

        return R.success("新增员工成功");
    }


    /**
     * 员工信息分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")  //这个是用普通get方式 如/page/username=xxx&password=xxx这种，所以可以自动封装，区别Rest
    public R<Page> page(int page,int pageSize,String name){
        log.info("page = {},pageSize = {},name = {}",page,pageSize,name);

        //构造分页构造器 (MP) ，配合之前配置的MybatisPlusConfig中的分页插件生效
        Page pageInfo = new Page(page,pageSize);

        //构造条件构造器  (MP)
        LambdaQueryWrapper<Employee> lqw = new LambdaQueryWrapper<>();
        //添加一个过滤条件
        lqw.like(StringUtils.isNotEmpty(name),Employee::getName,name);
        //添加一个排序条件
        lqw.orderByDesc(Employee::getUpdateTime);

        //执行查询
        emploeeService.page(pageInfo, lqw); //MP会将查询结果自动封装到pageInfo中
        return R.success(pageInfo);
    }


    /**
     * 根据id修改员工信息
     * @param employee
     * @return
     */
    @PutMapping
    public R<String> update(HttpServletRequest request, @RequestBody Employee employee){
        log.info("接收到修改对象的信息："+employee.toString());

        //记录更新操作发起时间及发起人
//        employee.setUpdateTime(LocalDateTime.now());
//        Long empId = (Long) request.getSession().getAttribute("employee");
//        employee.setUpdateUser(empId);

        //long id = Thread.currentThread().getId();
        //log.info("线程id为："+id);

        //调用修改
        emploeeService.updateById(employee);
        return R.success("员工信息修改成功！");
    }


    /**
     * 根据id查询员工信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")  //这个是用Rest风格传送参数，直接/page/1 区别普通get方式
    public R<Employee> getById(@PathVariable Long id){
        log.info("根据id查询员工信息");
        Employee employee = emploeeService.getById(id);
        if(employee != null)
            return R.success(employee);
        return R.error("没有查询到此员工的信息！");
    }
}
