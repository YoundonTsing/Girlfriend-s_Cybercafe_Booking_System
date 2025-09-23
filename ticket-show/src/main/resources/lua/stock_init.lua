-- Redis库存初始化Lua脚本
-- 功能：初始化或更新Redis中的库存信息
-- 参数：
--   KEYS[1]: 库存缓存key (格式: stock:ticket:{ticketId})
--   ARGV[1]: 可用库存数量
--   ARGV[2]: 库存过期时间(秒)
--   ARGV[3]: 是否强制更新 (1:强制更新, 0:仅在不存在时设置)
-- 返回值：
--   1: 初始化/更新成功
--   0: 库存已存在且未强制更新

local stockKey = KEYS[1]

-- 先写入调试信息
redis.call('SET', 'debug:argv1', ARGV[1])
redis.call('SET', 'debug:argv2', ARGV[2])
redis.call('SET', 'debug:argv3', ARGV[3])

local availableStock = tonumber(ARGV[1])
local expireTime = tonumber(ARGV[2])
local forceUpdate = tonumber(ARGV[3])

-- 更多调试信息
redis.call('SET', 'debug:availableStock', tostring(availableStock or 'nil'))
redis.call('SET', 'debug:expireTime', tostring(expireTime or 'nil'))
redis.call('SET', 'debug:forceUpdate', tostring(forceUpdate or 'nil'))

-- 参数验证
if not availableStock or availableStock < 0 then
    redis.call('SET', 'debug:error', 'availableStock invalid: ' .. tostring(availableStock))
    return -1  -- 库存数量无效
end

-- 检查库存数量是否为整数且在合理范围内
if availableStock ~= math.floor(availableStock) then
    redis.call('SET', 'debug:error', 'availableStock not integer: ' .. tostring(availableStock))
    return -1
end

-- 防止异常大库存初始化（库存不能超过1000000）
if availableStock > 1000000 then
    redis.call('SET', 'debug:error', 'availableStock too large: ' .. tostring(availableStock))
    return -1
end

if not expireTime or expireTime < 0 then
    redis.call('SET', 'debug:error', 'expireTime invalid: ' .. tostring(expireTime))
    return -2  -- 过期时间无效
end

-- 验证过期时间范围（最大30天）
if expireTime > 86400 * 30 then
    redis.call('SET', 'debug:error', 'expireTime too large: ' .. tostring(expireTime))
    return -2
end

if not forceUpdate then
    forceUpdate = 0  -- 默认不强制更新
end

-- 验证强制更新参数
if forceUpdate ~= 0 and forceUpdate ~= 1 then
    redis.call('SET', 'debug:error', 'forceUpdate invalid: ' .. tostring(forceUpdate))
    return -3  -- 强制更新参数无效
end

-- 检查库存是否已存在
local exists = redis.call('EXISTS', stockKey)

-- 如果库存不存在或者强制更新
if exists == 0 or forceUpdate == 1 then
    -- 设置库存
    redis.call('SET', stockKey, availableStock)
    
    -- 设置过期时间（如果指定）
    if expireTime > 0 then
        redis.call('EXPIRE', stockKey, expireTime)
    end
    
    redis.call('SET', 'debug:success', 'stock set successfully')
    return 1
else
    redis.call('SET', 'debug:success', 'stock already exists, not updated')
    return 0
end