// 通用验证码组件库入口：Vue 3 插件 + 具名导出

import './style.css';

import type { App } from 'vue';

import AngleCaptcha from './AngleCaptcha.vue';
import type {
  AngleChallengeData,
  CaptchaApi,
  CaptchaChallenge,
  ChallengePoint,
  ClickChallengeData,
  CurveChallengeData,
  RequestFunction,
  RequestOptions,
  RotateChallengeData,
  ScratchChallengeData,
  ScratchDebugPattern,
  SlideCurveChallengeData,
  SliderChallengeData,
  SwingTileChallengeData,
  VerifyResult,
} from './api';
import { createCaptchaApi, defaultRequest } from './api';
import AutoCaptcha from './AutoCaptcha.vue';
import Captcha from './Captcha.vue';
import CaptchaModal from './CaptchaModal.vue';
import ClickCaptcha from './ClickCaptcha.vue';
import CurveCaptcha from './CurveCaptcha.vue';
import FloatingCaptcha from './FloatingCaptcha.vue';
import type { CaptchaLocale, CaptchaMessages } from './i18n';
import { defaultMessagesFor, resolveCaptchaMessages } from './i18n';
import type { CaptchaOptions } from './options';
import {
  CaptchaOptionsKey,
  defaultCaptchaOptions,
  provideCaptchaOptions,
  resolveProvidedCaptchaOptions,
} from './options';
import RotateCaptcha from './RotateCaptcha.vue';
import ScratchCaptcha from './ScratchCaptcha.vue';
import SlideCurveCaptcha from './SlideCurveCaptcha.vue';
import SliderCaptcha from './SliderCaptcha.vue';
import SwingTileCaptcha from './SwingTileCaptcha.vue';
import type { CaptchaMode, CaptchaStatus, ClientType } from './types';

export {
  AngleCaptcha,
  AutoCaptcha,
  Captcha,
  CaptchaModal,
  CaptchaOptionsKey,
  ClickCaptcha,
  createCaptchaApi,
  CurveCaptcha,
  defaultCaptchaOptions,
  defaultMessagesFor,
  defaultRequest,
  FloatingCaptcha,
  provideCaptchaOptions,
  resolveCaptchaMessages,
  resolveProvidedCaptchaOptions,
  RotateCaptcha,
  ScratchCaptcha,
  SlideCurveCaptcha,
  SliderCaptcha,
  SwingTileCaptcha,
};

export type {
  AngleChallengeData,
  CaptchaApi,
  CaptchaChallenge,
  CaptchaLocale,
  CaptchaMessages,
  CaptchaMode,
  CaptchaOptions,
  CaptchaStatus,
  ChallengePoint,
  ClickChallengeData,
  ClientType,
  CurveChallengeData,
  RequestFunction,
  RequestOptions,
  RotateChallengeData,
  ScratchChallengeData,
  ScratchDebugPattern,
  SlideCurveChallengeData,
  SliderChallengeData,
  SwingTileChallengeData,
  VerifyResult,
};

const CaptchaToolkit = {
  install(app: App, options: Partial<CaptchaOptions> = {}) {
    app.provide(CaptchaOptionsKey, resolveProvidedCaptchaOptions(options));
  },
};

export default CaptchaToolkit;
