package com.PalmLife.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.StandardCharsets;

/**
 * 使用 FastJson2 进行 Redis 序列化的自定义序列化器。
 * <p>
 * 该序列化器用于将 Java 对象序列化为 JSON 字符串并存储到 Redis 中，
 * 同时能够从 Redis 中读取 JSON 字符串并反序列化为指定的 Java 对象。
 * <p>
 * 使用 FastJson2 的原因：
 * - 高性能：FastJson2 是一个高性能的 JSON 库，序列化和反序列化速度快。
 * - 跨语言：JSON 格式是跨语言的，其他语言的客户端也可以解析存储在 Redis 中的数据。
 * - 支持复杂对象：可以序列化和反序列化复杂的 Java 对象。
 * <p>
 * 注意事项：
 * - 安全性：虽然使用了 JSONReader.autoTypeFilter 来限制反序列化的类包路径，
 *   但仍需确保 JSON 数据来源可靠，避免反序列化攻击。
 * - 性能：在高并发场景下，需关注序列化和反序列化的性能开销。
 *
 * @param <T> 序列化和反序列化的对象类型
 */
public class FastJson2JsonRedisSerializer<T> implements RedisSerializer<T> {

    /**
     * 目标对象的类类型，用于反序列化时将 JSON 字符串还原为指定类型的对象。
     */
    private final Class<T> clazz;

    /**
     * 构造函数，初始化序列化器并指定目标对象的类类型。
     *
     * @param clazz 目标对象的类类型
     */
    public FastJson2JsonRedisSerializer(Class<T> clazz) {
        super();
        this.clazz = clazz;
    }

    /**
     * 将对象序列化为字节数组。
     * <p>
     * 序列化过程：
     * 1. 使用 FastJson2 的 JSON.toJSONString 方法将对象转换为 JSON 字符串。
     * 2. 使用 JSONWriter.Feature.WriteClassName 特性，将对象的类信息写入 JSON 字符串中，
     *    以便反序列化时能够还原为正确的类。
     * 3. 将 JSON 字符串转换为 UTF-8 编码的字节数组。
     *
     * @param obj 待序列化的对象
     * @return 序列化后的字节数组
     * @throws SerializationException 如果序列化失败
     */
    @Override
    public byte[] serialize(T obj) throws SerializationException {
        if (obj == null) {
            return new byte[0];
        }
        return JSON.toJSONString(obj, JSONWriter.Feature.WriteClassName).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 将字节数组反序列化为指定类型的对象。
     * <p>
     * 反序列化过程：
     * 1. 将字节数组转换为 UTF-8 编码的字符串。
     * 2. 使用 FastJson2 的 JSON.parseObject 方法将 JSON 字符串还原为对象。
     * 3. 使用 JSONReader.autoTypeFilter 方法限制反序列化时允许的类包路径，
     *    避免反序列化攻击（只允许从 org.springframework 和 com.ticknet 包路径下的类被反序列化）。
     *
     * @param bytes 待反序列化的字节数组
     * @return 反序列化后的对象
     * @throws SerializationException 如果反序列化失败
     */
    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        String str = new String(bytes, StandardCharsets.UTF_8);
        String[] packageNames = {"org.springframework", "com.ticknet"};
        return JSON.parseObject(str, clazz, JSONReader.autoTypeFilter(packageNames));
    }
}