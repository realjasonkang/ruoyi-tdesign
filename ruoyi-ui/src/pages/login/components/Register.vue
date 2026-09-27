<template>
  <t-form
    ref="form"
    class="item-container"
    :class="[`register-${type}`]"
    :data="formData"
    :rules="FORM_RULES"
    label-width="0"
    @submit="onSubmit"
  >
    <t-form-item v-if="tenantEnabled" name="tenantId">
      <t-select v-model="formData.tenantId" size="large" filterable placeholder="请选择/输入公司名称">
        <template #prefixIcon>
          <company class="t-icon" />
        </template>
        <t-option v-for="item in tenantList" :key="item.tenantId" :label="item.companyName" :value="item.tenantId" />
      </t-select>
    </t-form-item>
    <template v-if="type === 'phone'">
      <t-form-item name="phone">
        <t-input
          v-model="formData.phone"
          :maxlength="11"
          size="large"
          :placeholder="t('pages.login.register.phonePlaceholder')"
        >
          <template #prefix-icon>
            <user-icon />
          </template>
        </t-input>
      </t-form-item>
    </template>

    <template v-if="type === 'email'">
      <t-form-item name="email">
        <t-input
          v-model="formData.email"
          type="text"
          size="large"
          :placeholder="t('pages.login.register.emailPlaceholder')"
        >
          <template #prefix-icon>
            <mail-icon />
          </template>
        </t-input>
      </t-form-item>
    </template>

    <t-form-item name="password">
      <t-input
        v-model="formData.password"
        size="large"
        :type="showPsw ? 'text' : 'password'"
        clearable
        :placeholder="t('pages.login.register.passwordPlaceholder')"
      >
        <template #prefix-icon>
          <lock-on-icon />
        </template>
        <template #suffix-icon>
          <browse-icon v-if="showPsw" @click="showPsw = !showPsw" />
          <browse-off-icon v-else @click="showPsw = !showPsw" />
        </template>
      </t-input>
    </t-form-item>

    <template v-if="type === 'phone' || type === 'email'">
      <t-form-item class="verification-code" name="code">
        <t-input v-model="formData.code" size="large" :placeholder="t('pages.login.register.verifyCodePlaceholder')" />
        <t-button variant="outline" :disabled="countDown > 0" @click="handleCounter">
          {{
            countDown === 0
              ? t('pages.login.register.sendVerifyCode')
              : t('pages.login.register.resendCountdown', { count: countDown })
          }}
        </t-button>
      </t-form-item>
    </template>

    <t-form-item class="check-container" name="checked">
      <t-checkbox v-model="formData.checked">{{ t('pages.login.register.agreeTerms') }} </t-checkbox>
      <span>{{ t('pages.login.register.serviceTerms') }}</span>
      {{ t('common.conjunction') }}
      <span>{{ t('pages.login.register.privacyStatement') }}</span>
    </t-form-item>

    <t-form-item>
      <t-button :loading="loading" block size="large" type="submit">
        {{ loading ? '注册中...' : t('pages.login.register.registerBtn') }}
      </t-button>
    </t-form-item>

    <div class="switch-container">
      <span class="tip" @click="switchType(type === 'phone' ? 'email' : 'phone')">
        {{ type === 'phone' ? t('pages.login.register.useEmailRegister') : t('pages.login.register.usePhoneRegister') }}
      </span>
    </div>
  </t-form>

  <!-- 行为验证码：点击注册后弹窗验证，通过后凭一次性票据继续注册 -->
  <captcha
    display="modal"
    :visible="captchaVisible"
    :base-url="captchaBaseUrl"
    @success="onCaptchaSuccess"
    @close="onCaptchaClose"
  />
</template>
<script lang="ts" setup>
import '@/components/CaptchaToolkit/style.css';

import { BrowseIcon, BrowseOffIcon, LockOnIcon, MailIcon, UserIcon } from 'tdesign-icons-vue-next';
import type { FormRule, SubmitContext } from 'tdesign-vue-next';
import { MessagePlugin } from 'tdesign-vue-next';
import { computed, getCurrentInstance, ref } from 'vue';

import { getCodeImg, getTenantList, register } from '@/api/login';
import type { RegisterBody, TenantListVo } from '@/api/model/loginModel';
import Company from '@/assets/icons/svg/company.svg?component';
import type { VerifyResult } from '@/components/CaptchaToolkit/api';
import Captcha from '@/components/CaptchaToolkit/Captcha.vue';
import { useCounter } from '@/hooks';
import { t } from '@/locales';

const emit = defineEmits(['register-success']);

const equalToPassword = (value: string) => {
  return formData.value.password === value;
};

const FORM_RULES = computed<Record<string, FormRule[]>>(() => ({
  tenantId: [{ required: true, message: '请输入您的租户编号', type: 'error' }],
  phone: [{ required: true, message: t('pages.login.register.validation.phone'), type: 'error' }],
  email: [
    { required: true, message: t('pages.login.register.validation.email'), type: 'error' },
    { email: true, message: t('pages.login.register.validation.emailFormat'), type: 'warning' },
  ],
  password: [{ required: true, message: t('pages.login.register.validation.password'), type: 'error' }],
  confirmPassword: [
    { required: true, message: '请再次输入您的密码' },
    { validator: equalToPassword, message: '两次输入的密码不一致' },
  ],
  code: [{ required: true, message: t('pages.login.register.validation.verifyCode') }],
}));

const loading = ref(false);
const type = ref('phone');
const form = ref();
const formData = ref({
  tenantId: '',
  phone: '',
  email: '',
  account: '',
  password: '',
  confirmPassword: '',
  // 手机号/邮箱注册的验证码
  code: '',
  ticket: '',
  userType: 'sys_user',
  checked: false,
});
// 租户列表
const tenantList = ref<TenantListVo[]>([]);
// 租户开关
const tenantEnabled = ref(true);
// 验证码开关
const captchaEnabled = ref(true);
// 行为验证码弹窗开关（点击注册后弹出，验证通过再提交注册）
const captchaVisible = ref(false);
// 行为验证码接口前缀（跟随项目 api 前缀，开发环境由代理转发）
const captchaBaseUrl = `${import.meta.env.VITE_APP_BASE_API}/api/captcha`;
const showPsw = ref(false);

const [countDown, handleCounter] = useCounter();

const { proxy } = getCurrentInstance();

/**
 * 提交注册
 */
function doRegister() {
  loading.value = true;
  const registerForm: RegisterBody = {
    username: formData.value.account,
    password: formData.value.password,
    userType: 'sys_user',
    ticket: formData.value.ticket,
  };
  register(registerForm)
    .then(() => {
      MessagePlugin.success(t('pages.login.register.messages.registerSuccess'));
      emit('register-success');
      const username = registerForm.username;
      proxy.$modal.alert({
        header: `系统提示`,
        body: `恭喜你，您的账号 ${username} 注册成功！`,
        theme: 'success',
      });
    })
    .catch(() => {
      loading.value = false;
      // 票据一次性使用，注册失败后需要重新验证
      formData.value.ticket = '';
    });
}

const onSubmit = async (ctx: SubmitContext) => {
  if (ctx.validateResult !== true) {
    return;
  }
  if (type.value === 'phone' || type.value === 'email') {
    MessagePlugin.warning('暂不支持手机号与邮箱注册');
    return;
  }
  if (!formData.value.checked) {
    MessagePlugin.error(t('pages.login.register.validation.agreeTerms'));
    return;
  }
  // 账号信息先本地校验，再弹窗完成验证码，验证通过后才提交注册
  if (!formData.value.ticket) {
    // 先取本次的开关与类型，再按该类型渲染验证码弹窗
    if (await prepareCaptcha()) {
      captchaVisible.value = true;
      return;
    }
  }
  doRegister();
};

const switchType = (val: string) => {
  form.value?.reset();
  type.value = val;
};

/**
 * 拉取本次验证码开关与类型（后端 captcha.mode 可配成 random，每次随机一种类型）
 *
 * @return 本次注册是否需要验证码
 */
async function prepareCaptcha(): Promise<boolean> {
  try {
    const res = await getCodeImg();
    captchaEnabled.value = res.data.captchaEnabled === undefined ? true : res.data.captchaEnabled;
  } catch {
    // 拉取失败时沿用上一次的开关与类型，避免网络抖动直接跳过验证
  }
  // 票据是一次性的，每次验证前清空
  formData.value.ticket = '';
  return captchaEnabled.value;
}

/**
 * 行为验证码弹窗验证通过：保存一次性票据并继续注册
 */
function onCaptchaSuccess(result: VerifyResult) {
  formData.value.ticket = result?.ticket || '';
  captchaVisible.value = false;
  if (formData.value.ticket) {
    doRegister();
  }
}

/**
 * 行为验证码弹窗关闭（用户未验证或主动取消）
 */
function onCaptchaClose() {
  captchaVisible.value = false;
}

/**
 * 获取租户列表
 */
function initTenantList() {
  getTenantList(false).then((res) => {
    const vo = res.data;
    tenantEnabled.value = !!vo.tenantEnabled;
    if (tenantEnabled.value) {
      tenantList.value = vo.voList;
      if (tenantList.value != null && tenantList.value.length !== 0) {
        formData.value.tenantId = tenantList.value[0].tenantId;
      }
    }
  });
}

initTenantList();
</script>
<style lang="less" scoped>
@import '../index.less';
</style>
