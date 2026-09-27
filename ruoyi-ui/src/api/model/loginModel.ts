import type { SysUserVo } from '@/api/system/model/userModel';

// 登录用户信息
export interface UserInfo {
  user: SysUserVo;
  roles: Array<string>;
  permissions: Array<string>;
}

// 验证码开关（验证码图片、类型与答案都由行为验证码组件负责）
export interface CaptchaConfig {
  captchaEnabled: boolean;
}

// 登录参数
export interface LoginParam {
  username: string;
  password: string;
  /** 行为验证码票据（验证通过后一次性使用） */
  ticket?: string;
}

/**
 * 登录请求
 */
export interface LoginData {
  tenantId?: string;
  username?: string;
  password?: string;
  rememberMe?: boolean;
  socialCode?: string;
  socialState?: string;
  source?: string;
  /** 行为验证码票据 */
  ticket?: string;
  clientId?: string;
  grantType?: string;
}
/**
 * 登录验证信息
 */
export interface LoginVo {
  /** 授权令牌 */
  access_token?: string;
  /** 刷新令牌 */
  refresh_token?: string;
  /** 授权令牌 access_token 的有效期 */
  expire_in?: number;
  /** 刷新令牌 refresh_token 的有效期 */
  refresh_expire_in?: number;
  /** 应用id */
  client_id?: string;
  /** 令牌权限 */
  scope?: string;
  /** 用户 openid */
  openid?: string;
}

/**
 * 用户登录对象
 */
export interface LoginBody {
  /**
   * 用户名
   */
  username: string;

  /**
   * 用户密码
   */
  password: string;

  /**
   * 行为验证码票据（验证通过后一次性使用）
   */
  ticket?: string;
}

/**
 * 用户注册对象
 */
export interface RegisterBody extends LoginBody {
  userType: string;
}
/**
 * 租户列表
 */
export interface TenantListVo {
  tenantId?: string;
  companyName?: string;
  domain?: string;
}
/**
 * 登录租户对象
 */
export interface LoginTenantVo {
  /** 租户开关 */
  tenantEnabled?: boolean;
  /** 租户对象列表 */
  voList?: Array<TenantListVo>;
}
