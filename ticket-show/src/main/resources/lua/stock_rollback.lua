-- Redis库存回滚Lua脚本
-- 功能：原子性地回滚库存，用于订单取消或支付失败场景
-- 参数：
--   KEYS[1]: 库存缓存key (格式: stock:ticket:{ticketId})
--   ARGV[1]: 回滚数量
--   ARGV[2]: 最大库存限制
--   ARGV[3]: 库存过期时间(秒)
-- 返回值：
--   1: 回滚成功
--   0: 回滚失败（超过最大库存）
--   -1: 库存信息不存在或参数错误

local stockKey = KEYS[1]
local rollbackQuantity = tonumber(ARGV[1])
local maxStock = tonumber(ARGV[2])
local expireTime = tonumber(ARGV[3])

-- 获取当前时间戳用于调试
local timestamp = redis.call('TIME')[1]

-- 写入调试信息
redis.call('HSET', 'debug:stock_operations', stockKey .. ':rollback:' .. timestamp, 
    'rollback=' .. (rollbackQuantity or 'nil') .. ',max=' .. (maxStock or 'nil') .. ',expire=' .. (expireTime or 'nil') .. ',time=' .. timestamp)

-- 参数验证
if not rollbackQuantity or rollbackQuantity <= 0 then
    redis.call('HSET', 'debug:stock_operations', stockKey .. ':rollback_error:' .. timestamp, 
        'error=invalid_rollback_quantity,value=' .. (ARGV[1] or 'nil'))
    return -1
end

-- 检查回滚数量是否为整数且在合理范围内
if rollbackQuantity ~= math.floor(rollbackQuantity) then
    redis.call('HSET', 'debug:stock_operations', stockKey .. ':rollback_error:' .. timestamp, 
        'error=non_integer_rollback_quantity,value=' .. rollbackQuantity)
    return -1
end

-- 防止异常大数量回滚（单次回滚不能超过10000）
if rollbackQuantity > 10000 then
    redis.call('HSET', 'debug:stock_operations', stockKey .. ':rollback_error:' .. timestamp, 
        'error=excessive_rollback_quantity,value=' .. rollbackQuantity)
    return -1
end

-- 验证最大库存限制参数
if maxStock and (maxStock < 0 or maxStock > 1000000) then
    redis.call('HSET', 'debug:stock_operations', stockKey .. ':rollback_error:' .. timestamp, 
        'error=invalid_max_stock,value=' .. maxStock)
    return -1
end

-- 验证过期时间参数
if expireTime and (expireTime < 0 or expireTime > 86400 * 30) then -- 最大30天
    redis.call('HSET', 'debug:stock_operations', stockKey .. ':rollback_error:' .. timestamp, 
        'error=invalid_expire_time,value=' .. expireTime)
    return -1
end

-- 检查库存是否存在
local currentStock = redis.call('GET', stockKey)
if not currentStock then
    redis.call('HSET', 'debug:stock_operations', stockKey .. ':rollback_error:' .. timestamp, 
        'error=stock_not_exists')
    return -1
end

-- 转换为数字并验证
currentStock = tonumber(currentStock)
if not currentStock then
    redis.call('HSET', 'debug:stock_operations', stockKey .. ':rollback_error:' .. timestamp, 
        'error=non_numeric_stock_value,value=' .. (redis.call('GET', stockKey) or 'nil'))
    return -1
end

-- 检查库存值是否为负数
if currentStock < 0 then
    redis.call('HSET', 'debug:stock_operations', stockKey .. ':rollback_error:' .. timestamp, 
        'error=negative_stock_value,value=' .. currentStock)
    return -1
end

-- 检查库存值是否为整数
if currentStock ~= math.floor(currentStock) then
    redis.call('HSET', 'debug:stock_operations', stockKey .. ':rollback_error:' .. timestamp, 
        'error=non_integer_stock_value,value=' .. currentStock)
    return -1
end

-- 防止异常大库存值（库存不能超过1000000）
if currentStock > 1000000 then
    redis.call('HSET', 'debug:stock_operations', stockKey .. ':rollback_error:' .. timestamp, 
        'error=excessive_stock_value,value=' .. currentStock)
    return -1
end

-- 计算回滚后的库存
local newStock = currentStock + rollbackQuantity

-- 防止整数溢出
if newStock < currentStock then -- 检查是否发生溢出
    redis.call('HSET', 'debug:stock_operations', stockKey .. ':rollback_overflow:' .. timestamp, 
        'current=' .. currentStock .. ',rollback=' .. rollbackQuantity .. ',overflow_detected=true')
    return -1
end

-- 检查是否超过最大库存限制
if maxStock and maxStock > 0 and newStock > maxStock then
    redis.call('HSET', 'debug:stock_operations', stockKey .. ':rollback_exceed:' .. timestamp, 
        'current=' .. currentStock .. ',rollback=' .. rollbackQuantity .. ',new=' .. newStock .. ',max=' .. maxStock)
    return 0
end

-- 防止回滚后库存超过系统限制
if newStock > 1000000 then
    redis.call('HSET', 'debug:stock_operations', stockKey .. ':rollback_system_limit:' .. timestamp, 
        'current=' .. currentStock .. ',rollback=' .. rollbackQuantity .. ',new=' .. newStock .. ',limit=1000000')
    return 0
end

-- 执行库存回滚
redis.call('SET', stockKey, newStock)

-- 设置过期时间（如果指定）
if expireTime and expireTime > 0 then
    redis.call('EXPIRE', stockKey, expireTime)
end

-- 记录成功操作
redis.call('HSET', 'debug:stock_operations', stockKey .. ':rollback_success:' .. timestamp, 
    'before=' .. currentStock .. ',after=' .. newStock .. ',rollback=' .. rollbackQuantity)

return 1