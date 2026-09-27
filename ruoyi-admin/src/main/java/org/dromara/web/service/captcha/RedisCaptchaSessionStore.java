package org.dromara.web.service.captcha;

import io.github.yixiaco.model.CaptchaSession;
import io.github.yixiaco.store.CaptchaSessionStore;
import org.dromara.common.core.constant.GlobalConstants;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.redis.utils.RedisUtils;
import org.redisson.api.RBucket;
import org.redisson.client.codec.Codec;
import org.redisson.client.codec.StringCodec;
import org.redisson.codec.CompositeCodec;
import org.redisson.codec.Kryo5Codec;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 行为验证码会话的 Redis 共享存储
 *
 * <p>组件默认的内存实现只在单个实例内有效：多实例部署时，A 实例下发的验证码
 * 到 B 实例校验会被判定为“验证码已过期”。改为写 Redis 后，键的 TTL 即会话剩余
 * 有效期（过期由 Redis 自动清理），多实例共享同一份会话。</p>
 *
 * <p>组件库的 {@link CaptchaSession} 没有实现序列化接口、也没有无参构造，JSON 无法
 * 往返，且会话创建时间必须原样保留（最短耗时与行为校验都依赖它），因此这里用
 * key 字符串 + value 二进制（Kryo5）的编解码原样存取。升级组件库若调整了会话模型，
 * 需等存量会话过期（最长 5 分钟）后再切流量。</p>
 *
 * @author YiXia
 */
@Component
public class RedisCaptchaSessionStore implements CaptchaSessionStore {

    /**
     * 会话 Redis key 前缀
     */
    private static final String SESSION_KEY = GlobalConstants.GLOBAL_REDIS_KEY + "captcha:session:";

    /**
     * 会话编解码：key 使用字符串，value 使用 Kryo5 二进制
     */
    private static final Codec SESSION_CODEC = new CompositeCodec(StringCodec.INSTANCE,
        new Kryo5Codec(RedisCaptchaSessionStore.class.getClassLoader()),
        new Kryo5Codec(RedisCaptchaSessionStore.class.getClassLoader()));

    @Override
    public void put(CaptchaSession session) {
        if (session == null) {
            return;
        }
        long ttlMillis = session.getExpiresAt() - System.currentTimeMillis();
        if (ttlMillis <= 0) {
            return;
        }
        bucket(session.getId()).set(session, Duration.ofMillis(ttlMillis));
    }

    @Override
    public CaptchaSession get(String id) {
        if (StringUtils.isBlank(id)) {
            return null;
        }
        CaptchaSession session = bucket(id).get();
        if (session == null) {
            return null;
        }
        // 键理论上已被 Redis TTL 清理，这里再兜底校验一次过期时间
        if (session.isExpired()) {
            remove(id);
            return null;
        }
        return session;
    }

    @Override
    public void remove(String id) {
        if (StringUtils.isNotBlank(id)) {
            bucket(id).delete();
        }
    }

    @Override
    public void clearExpired() {
        // 写入时已设置 Redis TTL，过期由 Redis 自动清理，无需主动扫描
    }

    /**
     * 会话 ID 对应的 Redis 桶
     *
     * @param id 会话唯一标识
     * @return 会话桶
     */
    private RBucket<CaptchaSession> bucket(String id) {
        return RedisUtils.getClient().getBucket(SESSION_KEY + id, SESSION_CODEC);
    }
}
