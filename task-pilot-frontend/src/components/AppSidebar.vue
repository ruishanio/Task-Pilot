<script setup lang="ts">
import { computed, watch } from 'vue';
import { useRoute } from 'vue-router';
import {
  Clock3,
  Logs,
  LayoutGrid,
  LayoutDashboard,
  Server,
  Users,
  UserRound,
  ChevronsUpDown,
  KeyRound,
  LogOut,
} from '@lucide/vue';
import {
  Sidebar,
  SidebarHeader,
  SidebarContent,
  SidebarGroup,
  SidebarGroupLabel,
  SidebarGroupContent,
  SidebarMenu,
  SidebarMenuItem,
  SidebarMenuButton,
  SidebarFooter,
  SidebarRail,
  useSidebar,
} from '@/components/ui/sidebar';
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuSeparator,
  DropdownMenuItem,
} from '@/components/ui/dropdown-menu';
import { auth } from '@/stores/auth';
import BrandLogo from '@/components/BrandLogo.vue';
defineEmits<{ password: []; logout: [] }>();
const route = useRoute();
const { setOpenMobile } = useSidebar();
const iconMap = {
  'fa-home': LayoutDashboard,
  'fa-clock-o': Clock3,
  'fa-database': Logs,
  'fa-cloud': Server,
  'fa-users': Users,
};
// 服务端决定菜单权限，前端只负责分组和展示。
const groups = computed(() => {
  const items = auth.menus.map((item) => ({
    to: item.url,
    title: item.url === '/dashboard' ? '工作台' : item.name || item.url,
    icon: iconMap[item.icon as keyof typeof iconMap] || LayoutGrid,
  }));
  return [
    {
      label: '工作空间',
      items: items.filter((item) => !['/executor', '/user'].includes(item.to)),
    },
    {
      label: '系统管理',
      items: items.filter((item) => ['/executor', '/user'].includes(item.to)),
    },
  ].filter((group) => group.items.length);
});
watch(
  () => route.fullPath,
  () => setOpenMobile(false),
);
function active(path: string) {
  return (
    route.path.startsWith(path) ||
    (path === '/task_info' && route.path === '/task_code')
  );
}
</script>
<template>
  <Sidebar collapsible="icon" variant="inset">
    <SidebarHeader class="px-3 py-4">
      <SidebarMenu>
        <SidebarMenuItem>
          <SidebarMenuButton size="lg" as-child>
            <RouterLink to="/dashboard">
              <BrandLogo class="group-data-[collapsible=icon]:size-8" />
              <span class="grid gap-0.5 text-left">
                <span class="text-sm font-semibold tracking-tight">
                  Task Pilot
                </span>
                <span class="text-xs text-muted-foreground">任务调度平台</span>
              </span>
            </RouterLink>
          </SidebarMenuButton>
        </SidebarMenuItem>
      </SidebarMenu>
    </SidebarHeader>
    <SidebarContent class="gap-4 px-1">
      <SidebarGroup v-for="group in groups" :key="group.label">
        <SidebarGroupLabel>{{ group.label }}</SidebarGroupLabel>
        <SidebarGroupContent>
          <SidebarMenu class="gap-1">
            <SidebarMenuItem v-for="item in group.items" :key="item.to">
              <SidebarMenuButton
                class="h-9"
                :is-active="active(item.to)"
                :tooltip="item.title"
                as-child
              >
                <RouterLink
                  :to="item.to"
                  :aria-current="active(item.to) ? 'page' : undefined"
                >
                  <component :is="item.icon" />
                  <span>{{ item.title }}</span>
                </RouterLink>
              </SidebarMenuButton>
            </SidebarMenuItem>
          </SidebarMenu>
        </SidebarGroupContent>
      </SidebarGroup>
    </SidebarContent>
    <SidebarFooter class="p-3">
      <SidebarMenu>
        <SidebarMenuItem>
          <DropdownMenu>
            <DropdownMenuTrigger as-child>
              <SidebarMenuButton
                size="lg"
                class="data-[state=open]:bg-sidebar-accent"
                aria-label="账户菜单"
              >
                <span
                  class="flex size-8 shrink-0 items-center justify-center rounded-lg border bg-background"
                >
                  <UserRound class="size-4" />
                </span>
                <!-- 两行文字共 32px，与头像等高，避免默认行高造成视觉错位。 -->
                <span class="grid min-w-0 flex-1 text-left">
                  <span class="truncate font-medium leading-4">
                    {{ auth.user?.userName }}
                  </span>
                  <span class="truncate text-xs leading-4 text-muted-foreground">
                    {{ auth.user?.isAdmin ? '管理员' : '普通用户' }}
                  </span>
                </span>
                <ChevronsUpDown class="ml-auto size-4" />
              </SidebarMenuButton>
            </DropdownMenuTrigger>
            <!-- 菜单向上展开并跟随账户入口宽度；导航折叠时仍保留足够的操作空间。 -->
            <DropdownMenuContent
              side="top"
              align="start"
              :side-offset="8"
              :collision-padding="8"
              class="min-w-48 max-w-[calc(100vw-2rem)] rounded-xl p-1.5"
            >
              <DropdownMenuItem class="gap-2 px-2 py-1.5" @select="$emit('password')">
                <KeyRound class="text-muted-foreground" />
                修改密码
              </DropdownMenuItem>
              <DropdownMenuSeparator class="mx-1" />
              <DropdownMenuItem class="gap-2 px-2 py-1.5" @select="$emit('logout')">
                <LogOut class="text-muted-foreground" />
                退出登录
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </SidebarMenuItem>
      </SidebarMenu>
    </SidebarFooter>
    <SidebarRail />
  </Sidebar>
</template>
