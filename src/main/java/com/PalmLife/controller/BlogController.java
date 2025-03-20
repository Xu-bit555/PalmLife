package com.PalmLife.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.PalmLife.dto.Result;
import com.PalmLife.dto.UserDTO;
import com.PalmLife.entity.Blog;
import com.PalmLife.service.IBlogService;
import com.PalmLife.utils.SystemConstants;
import com.PalmLife.utils.UserHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/blog")
public class BlogController {

    @Resource
    private IBlogService blogService;

    /**
     * 保存博客
     *
     * @param blog 博客
     * @return 结果
     */
    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        return blogService.saveBlog(blog);
    }

    /**
     * 点赞和取赞博客
     * @param id
     * @return
     */
    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {
        // 修改点赞数量
        return blogService.likeBlog(id);
    }

    /**
     * 查询热门博客
     *
     * @param current 当前
     * @return {@link Result}
     */
    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogService.queryHotBlog(current);
    }


    /**
     * 通过id查询博客
     *
     * @param id id
     * @return {@link Result}
     */
    @GetMapping("/{id}")
    public Result queryBlogById(@PathVariable("id") Long id) {
        return blogService.queryBlogById(id);
    }

    /**
     * 查询博客点赞排行榜
     *
     * @param id id
     * @return {@link Result}
     */
    @GetMapping("/likes/{id}")
    public Result queryBlogLikesById(@PathVariable("id") Long id) {
        return blogService.queryBlogLikesById(id);
    }


    /**
     * 查询博客浏览量
     *
     * @param max
     * @param offset
     * @return {@link Result}
     */
    @GetMapping("/of/follow")
    public Result queryBlogOfFollow(@RequestParam("lastId")Long max,
                                    @RequestParam(value = "offset" , defaultValue = "0")Integer offset){
        return blogService.queryBlogOfFollow(max,offset);
    }

    /**
     * 查询个人博客
     * @param current
     * @return
     */
    @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .eq("user_id", user.getId())
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }



    /**
     * 通过用户id查询博客
     * @param current
     * @param id
     * @return
     */
    @GetMapping("/of/user")
    public Result queryBlogByUserId(@RequestParam(value = "current",defaultValue = "1")Integer current,
                                    @RequestParam("id")Long id){
        Page<Blog> page = blogService.query()
                .eq("user_id", id)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));

        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }


}
