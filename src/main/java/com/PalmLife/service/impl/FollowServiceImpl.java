package com.PalmLife.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.PalmLife.dto.Result;
import com.PalmLife.dto.UserDTO;
import com.PalmLife.entity.Follow;
import com.PalmLife.entity.User;
import com.PalmLife.mapper.FollowMapper;
import com.PalmLife.service.IFollowService;
import com.PalmLife.service.IUserService;
import com.PalmLife.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IUserService userService;

    /**
     * 关注或者取关
     *
     * @param followUserId 遵循用户id
     * @param isFollow     是遵循
     * @return {@link Result}
     */
    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        //获取登陆用户
        Long id = UserHolder.getUser().getId();
        //如果是关注
        if (isFollow) {
            //关注 新增数据
            Follow follow = new Follow();
            follow.setFollowUserId(followUserId);
            follow.setUserId(id);
            boolean isSuccess = save(follow);
            if (isSuccess){
                String key="follows:" + id;
                //Redis操作Set集合，因为关注的用户id是唯一的
                stringRedisTemplate.opsForSet().add(key , followUserId.toString());
            }
        }else{
            //取关操作
            boolean isSuccess = remove(new LambdaQueryWrapper<Follow>()
                    .eq(Follow::getUserId, id)  //查找操作用户
                    .eq(Follow::getFollowUserId, followUserId)  //查找取关用户
            );
            //如果存在，将取关用户id从Redis中删除
            if (isSuccess) {
                String key = "follows:"+id;
                stringRedisTemplate.opsForSet().remove(key,followUserId);
            }
        }
        return Result.ok();
    }

    /**
     * 是否关注
     *
     * @param followUserId 遵循用户id
     * @return {@link Result}
     */
    @Override
    public Result isFollow(Long followUserId) {
        //获取登陆用户
        Long id = UserHolder.getUser().getId();
        //查询是否关注
        Long count = lambdaQuery()
                .eq(Follow::getUserId, id)
                .eq(Follow::getFollowUserId, followUserId)
                .count();
        return Result.ok(count>0);
    }

    /**
     * 共同关注
     *
     * @param id id
     * @return {@link Result}
     */
    @Override
    public Result followCommons(Long id) {
        //获取登陆用户的关注列表
        Long userId = UserHolder.getUser().getId();
        String key1="follows:" + userId;
        //获取id用户的关注列表
        String key2="follows:" + id;

        //获取两个用户的交集
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key1, key2);
        //如果没有共同关注
        if (intersect == null || intersect.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        //有共同关注，使用stream流处理：将Map中的value转换为Long类型，使用List集合存储
        List<Long> ids = intersect.stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());
        //查询用户
        List<User> users = userService.listByIds(ids);
        List<UserDTO> collect = users.stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))//对每个user转换为UserDTO类型
                .collect(Collectors.toList());
        return Result.ok(collect);
    }
}
