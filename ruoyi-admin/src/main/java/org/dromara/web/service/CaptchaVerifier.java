package org.dromara.web.service;

import io.github.yixiaco.CaptchaEngine;
import io.github.yixiaco.autoconfigure.CaptchaProperties;
import io.github.yixiaco.model.VerifyResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.Constants;
import org.dromara.common.core.exception.user.CaptchaException;
import org.dromara.common.core.exception.user.CaptchaExpireException;
import org.dromara.common.core.utils.MessageUtils;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 行为验证码票据校验
 *
 * <p>前端滑块验证通过后由 {@code POST {captcha.api-prefix}/verify} 下发一次性票据，
 * 登录、注册等业务接口携带该票据调用 {@link CaptchaEngine#consumeTicket(String)} 校验，
 * 校验成功即消费，同一票据不能重复使用。</p>
 *
 * @author YiXia
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaVerifier {

    /**
     * 票据不存在、已过期或已被使用的业务码
     */
    private static final String CODE_INVALID_TICKET = "INVALID_TICKET";

    private final CaptchaEngine captchaEngine;
    private final CaptchaProperties captchaProperties;

    /**
     * 是否启用验证码校验
     */
    public boolean isEnabled() {
        return captchaProperties.isEnabled();
    }

    /**
     * 校验验证码票据，票据无效时记录登录日志并抛出对应业务异常；
     * 验证码未启用（{@code captcha.enabled=false}）时直接放行
     *
     * @param tenantId 租户id
     * @param userId   用户id
     * @param username 用户名
     * @param ticket   验证码票据（前端验证通过后下发）
     * @param recorder 登录日志记录器
     */
    public void verify(String tenantId, Long userId, String username, String ticket, LogininforRecorder recorder) {
        if (!isEnabled()) {
            return;
        }
        VerifyResult result = captchaEngine.consumeTicket(StringUtils.trimToEmpty(ticket));
        if (result.isSuccess()) {
            return;
        }
        if (CODE_INVALID_TICKET.equals(result.getCode())) {
            recorder.record(tenantId, userId, username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire"));
            throw new CaptchaExpireException();
        }
        log.warn("验证码票据校验失败: {}", result.getMessage());
        recorder.record(tenantId, userId, username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.error"));
        throw new CaptchaException();
    }

    /**
     * 登录日志记录器，签名与 {@code SysLoginService#recordLogininfor} 保持一致
     */
    @FunctionalInterface
    public interface LogininforRecorder {

        /**
         * 记录登录日志
         *
         * @param tenantId 租户id
         * @param userId   用户id
         * @param username 用户名
         * @param status   状态
         * @param message  消息内容
         */
        void record(String tenantId, Long userId, String username, String status, String message);
    }
}
