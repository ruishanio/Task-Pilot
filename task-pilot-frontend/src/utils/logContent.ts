import DOMPurify from 'dompurify';
/** 保留日志的换行和加粗标记，禁止日志内容创建链接、图片、事件或脚本。 */
export function sanitizeLogContent(content?: string | null): string {
  return DOMPurify.sanitize(content || '', {
    ALLOWED_TAGS: ['br', 'b'],
    ALLOWED_ATTR: [],
  });
}
