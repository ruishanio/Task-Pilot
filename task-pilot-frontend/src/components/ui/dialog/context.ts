import type { InjectionKey, Ref } from 'vue';

// 由内容容器控制关闭入口，标题栏负责布局，避免不同内边距下的绝对定位偏移。
export const dialogCloseButtonKey: InjectionKey<Readonly<Ref<boolean>>> =
  Symbol('dialogCloseButton');
