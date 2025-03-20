package com.PalmLife.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.PalmLife.dto.LoginFormDTO;
import com.PalmLife.dto.Result;
import com.PalmLife.dto.UserDTO;
import com.PalmLife.entity.User;
import com.PalmLife.mapper.UserMapper;
import com.PalmLife.service.IUserService;
import com.PalmLife.utils.RegexUtils;
import com.PalmLife.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.PalmLife.utils.RedisConstants.*;
import static com.PalmLife.utils.SystemConstants.*;


/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            //手机号不符合
            return Result.fail("手机号格式错误");
        }
        //从redis中获取验证码 校验验证码
        /*  Object cacheCode = session.getAttribute("code");*/
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();
        if (cacheCode == null || !cacheCode.equals(code)) {
            //不一致 报错
            return Result.fail("验证码错误");
        }
        //根据手机号查询用户
        User user = baseMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        //判断用户是否存在
        if (user == null) {
            //不存在 创建新用户
            user = createUserWithPhone(phone);
        }
        /*//保存用户信息到session
        session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));*/
        //生成token
        String token = UUID.randomUUID().toString(true);
        //userDTO转map
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        //将Bean对象userDTO转换为Map对象
        Map<String, Object> map = BeanUtil.beanToMap(
                userDTO
                , new HashMap<>()
                //下列为转换选项
                , CopyOptions.create()
                        .setIgnoreNullValue(true) //忽略为null的字段
                        .setFieldValueEditor((name, value) -> value.toString()));   //将Value转换为String

        //Token保存在Redis中，存储用户信息
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token, map);
        //设置过期时间
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.MINUTES);
        return Result.ok(token);
    }

    /**
     * 登录辅助方法：发送手机验证码
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        //校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            //手机号不符合
            return Result.fail("手机号格式错误");
        }
        //手机号符合,生成验证码
        String code = RandomUtil.randomNumbers(6);
        /*//保存验证码到session
        session.setAttribute("code", code);*/
        //保存验证码到redis
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //发送验证码
        log.debug("发送验证码成功，验证码：{}", code);
        //返回ok
        return Result.ok();
    }

    /**
     * 登录辅助方法：根据手机号创建用户
     * @param phone
     * @return
     */
    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        //生成随机昵称
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        baseMapper.insert(user);
        return user;
    }


    /**
     * 签到
     * @return
     */
    @Override
    public Result sign() {
        //获取当前登陆用户
        Long id = UserHolder.getUser().getId();
        //获取日期
        LocalDateTime now = LocalDateTime.now();
        //拼接key
        String yyyyMM = now.format(DateTimeFormatter.ofPattern("yyyy:MM:"));
        String key = USER_SIGN_KEY +yyyyMM+ id;
        //获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        //年月作为Key，日期作为offset，从0开始，true表示在对应的位置上设置为1
        stringRedisTemplate.opsForValue().setBit(key,dayOfMonth-1,true);
        return Result.ok();
    }

    /**
     * 查询签到次数
     * @return
     */
    @Override
    public Result signCount() {
        //获取当前登陆用户
        Long id = UserHolder.getUser().getId();
        //获取日期
        LocalDateTime now = LocalDateTime.now();
        //拼接key
        String yyyyMM = now.format(DateTimeFormatter.ofPattern("yyyy:MM:"));
        String key = USER_SIGN_KEY +yyyyMM+ id;
        //获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        //获取截至本月今天的所有签到记录
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key //对Key进行操作
                ,BitFieldSubCommands.create() //创建BitFieldSubCommands对象
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)) //获取无符号且日期大小的位字段
                        .valueAt(0) //从0开始读取
        );

        if (result == null || result.isEmpty()){
            return Result.ok(0);
        }

        Long num = result.get(0);
        if (num == null|| num == 0){
            return Result.ok(0);
        }

        //转二进制字符串
        String binaryString = Long.toBinaryString(num);
        //计算连续签到天数
        int count=0;
        for (int i = binaryString.length()-1; i >=0; i--) {
            if (binaryString.charAt(i)=='1'){
                count++;
            }
            else {
                break;
            }
        }
        //返回
        return Result.ok(count);
    }

    /**
     * 退出登录
     * @return
     */
    @Override
    public Result logout() {
        UserHolder.removeUser();
        if(UserHolder.getUser() == null){
            return Result.ok("退出成功");
        }
        return Result.fail("退出失败");
    }
}
