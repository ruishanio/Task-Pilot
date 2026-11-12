-- 执行器名称扩展到 64 个字符；保留 V1 校验值，兼容已有数据库的升级。
ALTER TABLE executor ALTER COLUMN title TYPE varchar(64);

-- 已被截断的名称由执行器下次同步时根据完整配置恢复，迁移不猜测或覆盖人工维护的名称。
