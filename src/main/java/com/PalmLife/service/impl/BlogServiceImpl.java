package com.PalmLife.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.PalmLife.dto.Result;
import com.PalmLife.dto.ScrollResult;
import com.PalmLife.dto.UserDTO;
import com.PalmLife.entity.Blog;
import com.PalmLife.entity.Follow;
import com.PalmLife.entity.User;
import com.PalmLife.mapper.BlogMapper;
import com.PalmLife.service.IBlogService;
import com.PalmLife.service.IFollowService;
import com.PalmLife.service.IUserService;
import com.PalmLife.utils.RedisConstants;
import com.PalmLife.utils.SystemConstants;
import com.PalmLife.utils.UserHolder;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.util.concurrent.RateLimiter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Resource
    private IUserService userService;
    @Resource
    private IFollowService followService;
    @Resource
    private RedisTemplate redisTemplate;

    //引入令牌桶算法，每秒最多处理 5 次请求
    private static final RateLimiter rateLimiter = RateLimiter.create(5);


    /**
     * 保存博客
     *
     * @param blog 博客
     * @return 结果
     */
    @Override
    public Result saveBlog(Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        boolean isSuccess = save(blog);
        if (!isSuccess){
            return Result.fail("新增笔记失败");
        }
        //查询笔记作者的所有粉丝
        List<Follow> follows = followService.lambdaQuery()
                .eq(Follow::getFollowUserId, user.getId())
                .list();
        //推送笔记给所有粉丝
        for (Follow follow : follows) {
            Long userId = follow.getUserId();
            //推送
            String key=" feed:" + userId;
            redisTemplate.opsForZSet().add(key , blog.getId().toString() , System.currentTimeMillis());
        }
        // 返回id
        return Result.ok(blog.getId());
    }


    /**
     * 点赞和取赞博客
     * @param id
     * @return
     */
    @Override
    public Result likeBlog(Long id) {
        //获取当前登陆用户
        Long userId = UserHolder.getUser().getId();
        //判断当前用户是否点赞
        String key = RedisConstants.BLOG_LIKED_KEY + id;
        Double score = redisTemplate.opsForZSet().score(key , userId.toString() );
        if (score == null) {
            //如果未点赞 可以点赞
            //写数据库
            boolean isSuccess = update()
                    .setSql("liked = liked+1")
                    .eq("id", id).update();
            //保存数据到redis
            if (isSuccess){
                redisTemplate.opsForZSet().add(key , userId.toString() , System.currentTimeMillis());
            }
        } else {
            //如果已经点赞 取消点赞
            boolean isSuccess = update().setSql("liked=liked-1").eq("id", id).update();
            //数据库-1
            if (isSuccess){
                redisTemplate.opsForZSet().remove(key , userId.toString());
            }
            //redis删除数据
        }
        return Result.ok();
    }


    /**
     * 查询热门博客
     *
     * @param current 当前
     * @return {@link Result}
     */
    @Override
    public Result queryHotBlog(Integer current) {
        // 使用 RateLimiter 进行限流
        if (!rateLimiter.tryAcquire()) {
            // 如果没有获取到令牌，返回限流提示
            return Result.fail("请求过于频繁，请稍后再试");
        }

        // 根据点赞量进行排序，根据页码进行数据查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog -> {
            queryBlogUser(blog);    //填充用户信息
            isBlogLiked(blog);  //填充是否点赞
        });
        return Result.ok(records);
    }



    /**
     * 通过id查询博客
     *
     * @param id id
     * @return {@link Result}
     */
    @Override
    public Result queryBlogById(Long id) {
        //查询blog
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("博客不存在");
        }
        queryBlogUser(blog);
        isBlogLiked(blog);
        return Result.ok(blog);
    }



    /**
     * 查询博客点赞排行榜
     *
     * @param id id
     * @return {@link Result}
     */
    @Override
    public Result queryBlogLikesById(Long id) {


        String key = RedisConstants.BLOG_LIKED_KEY + id;
        //查询top5的点赞用户
        Set<String> top5 = redisTemplate.opsForZSet().range(key, 0, 6);
        if (top5 == null || top5.isEmpty()){
            return Result.ok(Collections.emptyList());
        }
        //解析出用户id
        List<Long> userIds = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        String join = StrUtil.join(",", userIds);
        //根据id查询用户
        List<UserDTO> userDTOS = userService.lambdaQuery()
                .in(User::getId,userIds)
                .last("order by field(id,"+join+")")
                .list()
                .stream().map(user -> BeanUtil.copyProperties(user, UserDTO.class)
        ).collect(Collectors.toList());
        //返回
        return Result.ok(userDTOS);
    }


    /**
     * 查询博客浏览量
     *
     * @param max
     * @param offset
     * @return {@link Result}
     */
    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        //获取当前用户
        UserDTO user = UserHolder.getUser();
        //查询收件箱
        String key="feed:" + user.getId();
        Set<ZSetOperations.TypedTuple<String>> typedTuples = redisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, 2);
        //非空判断
        if (typedTuples==null||typedTuples.isEmpty()){
            return Result.ok();
        }
        //解析数据 blogId minTime offset
        List<Long> ids = new ArrayList<>(typedTuples.size());
        long minTime =0;
        int os = 1;
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            //获取id
            String blogId = typedTuple.getValue();
            ids.add(Long.valueOf(blogId));
            long time = typedTuple.getScore().longValue();
            if (time == minTime){
                os++;
            }else {
                minTime = time;
                os = 1;
            }
        }
        //根据 查询blog
        List<Blog> blogs = new ArrayList<>(ids.size());
        for (Long id : ids) {
            Blog blog = getById(id);
            blogs.add(blog);
        }
        blogs.forEach(this::isBlogLiked);
        //封装 返回
        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setList(blogs);
        scrollResult.setOffset(os);
        scrollResult.setMinTime(minTime);
        return Result.ok(scrollResult);
    }


    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }

    private void isBlogLiked(Blog blog) {
        //获取当前登陆用户
        UserDTO user = UserHolder.getUser();
        if (user==null){
            return;
        }
        Long userId = user.getId();
        //判断当前用户时候点赞
        String key = RedisConstants.BLOG_LIKED_KEY + blog.getId();
        Double score = redisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score != null);
    }

}
