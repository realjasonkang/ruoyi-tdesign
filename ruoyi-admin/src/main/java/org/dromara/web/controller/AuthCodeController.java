package org.dromara.web.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.core.util.RandomUtil;
import io.github.yixiaco.autoconfigure.CaptchaProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.Constants;
import org.dromara.common.core.constant.GlobalConstants;
import org.dromara.common.core.constant.MessageConstants;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.enums.MessageTypeEnum;
import org.dromara.common.ratelimiter.annotation.RateLimiter;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.system.service.ISysMessageSendService;
import org.dromara.web.domain.vo.CaptchaVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 登录验证码操作处理
 *
 * <p>图形验证码已替换为行为验证码（滑块拼图），下发与校验接口由
 * captcha-spring-boot-starter 自动注册（前缀见 captcha.api-prefix），
 * 验证码类型由后端类型池（captcha.types）决定并随下发结果返回，前端按响应渲染，
 * 前端验证通过后凭一次性票据登录，这里只负责下发验证码开关与短信/邮箱验证码。</p>
 *
 * @author hexm
 */
@SaIgnore
@Validated
@RestController
@RequiredArgsConstructor
public class AuthCodeController {

    private final CaptchaProperties captchaProperties;
    private final ISysMessageSendService messageSendService;

    /**
     * 短信验证码
     *
     * @param phonenumber 用户手机号
     */
    @RateLimiter(key = "#phonenumber", time = 60, count = 1)
    @GetMapping("/resource/sms/code")
    public R<Void> smsCode(@NotBlank(message = "{user.phonenumber.not.blank}") String phonenumber) {
        String key = GlobalConstants.CAPTCHA_CODE_KEY + phonenumber;
        String code = RandomUtil.randomNumbers(4);
        Map<String, Object> map = new HashMap<>(1);
        map.put("code", code);
        // 发送验证码
        messageSendService.send(MessageConstants.LOGIN_CAPTCHA, MessageTypeEnum.SMS, phonenumber, map);
        RedisUtils.setObject(key, code, Duration.ofMinutes(Constants.CAPTCHA_EXPIRATION));
        return R.ok();
    }

    /**
     * 邮箱验证码
     *
     * @param email 邮箱
     */
    @GetMapping("/resource/email/code")
    public R<Void> emailCode(@NotBlank(message = "{user.email.not.blank}") String email) {
        String key = GlobalConstants.CAPTCHA_CODE_KEY + email;
        String code = RandomUtil.randomNumbers(4);
        Map<String, Object> map = new HashMap<>(1);
        map.put("code", code);
        map.put("time", Constants.CAPTCHA_EXPIRATION);
        // 发送验证码
        messageSendService.send(MessageConstants.LOGIN_CAPTCHA, MessageTypeEnum.MAIL, email, map);
        RedisUtils.setObject(key, code, Duration.ofMinutes(Constants.CAPTCHA_EXPIRATION));
        return R.ok();
    }

    /**
     * 获取验证码开关
     *
     * <p>验证码图片与答案由行为验证码组件负责（GET {captcha.api-prefix}），
     * 前端据此开关决定是否渲染验证码组件，验证码类型由后端随下发结果返回。</p>
     */
    @GetMapping("/auth/code")
    public R<CaptchaVo> getCode() {
        CaptchaVo captchaVo = new CaptchaVo();
        captchaVo.setCaptchaEnabled(captchaProperties.isEnabled());
        return R.ok(captchaVo);
    }

}
