package org.dromara.web.service.captcha;

import io.github.yixiaco.model.CaptchaTicket;
import io.github.yixiaco.store.CaptchaTicketStore;
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
 * 行为验证码票据的 Redis 共享存储
 *
 * <p>验证通过后下发的一次性票据写入 Redis（TTL 即票据有效期），登录、注册等业务
 * 接口在任意实例上都能校验；票据读取即消费：{@code get} 使用 Redis 的原子
 * {@code getAndDelete}，多实例并发提交同一票据时也只会成功一次。</p>
 *
 * <p>组件库的 {@link CaptchaTicket} 由构造方法直接生成时间戳、无法直接反序列化，
 * 因此与会话一样使用 key 字符串 + value 二进制（Kryo5）的编解码原样存取。</p>
 *
 * @author YiXia
 */
@Component
public class RedisCaptchaTicketStore implements CaptchaTicketStore {

    /**
     * 票据 Redis key 前缀
     */
    private static final String TICKET_KEY = GlobalConstants.GLOBAL_REDIS_KEY + "captcha:ticket:";

    /**
     * 票据编解码：key 使用字符串，value 使用 Kryo5 二进制
     */
    private static final Codec TICKET_CODEC = new CompositeCodec(StringCodec.INSTANCE,
        new Kryo5Codec(RedisCaptchaTicketStore.class.getClassLoader()),
        new Kryo5Codec(RedisCaptchaTicketStore.class.getClassLoader()));

    @Override
    public void put(CaptchaTicket ticket) {
        if (ticket == null) {
            return;
        }
        long ttlMillis = ticket.getExpiresAt() - System.currentTimeMillis();
        if (ttlMillis <= 0) {
            return;
        }
        bucket(ticket.getTicket()).set(ticket, Duration.ofMillis(ttlMillis));
    }

    @Override
    public CaptchaTicket get(String ticket) {
        if (StringUtils.isBlank(ticket)) {
            return null;
        }
        // 读取即消费：getAndDelete 由 Redis 原子执行，保证票据一次性使用
        return bucket(ticket).getAndDelete();
    }

    @Override
    public void remove(String ticket) {
        if (StringUtils.isNotBlank(ticket)) {
            bucket(ticket).delete();
        }
    }

    @Override
    public void clearExpired() {
        // 写入时已设置 Redis TTL，过期由 Redis 自动清理，无需主动扫描
    }

    /**
     * 票据值对应的 Redis 桶
     *
     * @param ticket 票据唯一标识
     * @return 票据桶
     */
    private RBucket<CaptchaTicket> bucket(String ticket) {
        return RedisUtils.getClient().getBucket(TICKET_KEY + ticket, TICKET_CODEC);
    }
}
