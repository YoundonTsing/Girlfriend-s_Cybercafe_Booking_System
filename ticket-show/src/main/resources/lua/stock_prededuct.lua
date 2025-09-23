-- 预减库存Lua脚本
-- 功能：原子性预减库存，避免超卖
-- 参数：
--   KEYS[1]: 库存缓存key
--   ARGV[1]: 扣减数量
--   ARGV[2]: 过期时间（秒）
-- 返回值：
--   1: 成功
--   0: 库存不足
--   -1: 库存不存在或参数错误

local stockKey = KEYS[1]
local deductQuantity = tonumber(ARGV[1])
local expireTime = tonumber(ARGV[2])

-- 获取当前时间戳用于调试
local timestamp = redis.call('TIME')[1]

-- 写入调试信息
redis.call('HSET', 'debug:stock_operations', stockKey .. ':prededuct:' .. timestamp, 
    'deduct=' .. (deductQuantity or 'nil') .. ',expire=' .. (expireTime or 'nil') .. ',time=' .. timestamp)

-- 参数验证
if not deductQuantity or deductQuantity <= 0 then
    redis.call('HSET', 'debug:stock_operations', stockKey .. ':prededuct_error:' .. timestamp, 
        'error=invalid_deduct_amount,value=' .. (ARGV[1] or 'nil'))
    return -1
end

-- 检查扣减数量是否为整数且在合理范围内
if deductQuantity ~= math.floor(deductQuantity) then
    redis.call('HSET', 'debug:stock_operations', stockKey .. ':prededuct_error:' .. timestamp, 
        'error=non_integer_deduct_amount,value=' .. deductQuantity)
    return -1
end

-- 防止异常大数量扣减（单次扣减不能超过10000）
if deductQuantity > 10000 then
    redis.call('HSET', 'debug:stock_operations', stockKey .. ':prededuct_error:' .. timestamp, 
        'error=excessive_deduct_amount,value=' .. deductQuantity)
    return -1
end

-- 验证过期时间参数
if expireTime and (expireTime < 0 or expireTime > 86400 * 30) then -- 最大30天
    redis.call('HSET', 'debug:stock_operations', stockKey .. ':prededuct_error:' .. timestamp, 
        'error=invalid_expire_time,value=' .. expireTime)
    return -1
end

-- 检查库存是否存在
local currentStock = redis.call('GET', stockKey)
if not currentStock then
    redis.call('HSET', 'debug:stock_operations', stockKey .. ':prededuct_error:' .. timestamp, 
        'error=stock_not_exists')
    return -1
end

-- 转换为数字并验证
currentStock = tonumber(currentStock)
if not currentStock then
    redis.call('HSET', 'debug:stock_operations', stockKey .. ':prededuct_error:' .. timestamp, 
        'error=non_numeric_stock_value,value=' .. (redis.call('GET', stockKey) or 'nil'))
    return -1
end

-- 检查库存值是否为负数
if currentStock < 0 then
    redis.call('HSET', 'debug:stock_operations', stockKey .. ':prededuct_error:' .. timestamp, 
        'error=negative_stock_value,value=' .. currentStock)
    return -1
end

-- 检查库存值是否为整数
if currentStock ~= math.floor(currentStock) then
    redis.call('HSET', 'debug:stock_operations', stockKey .. ':prededuct_error:' .. timestamp, 
        'error=non_integer_stock_value,value=' .. currentStock)
    return -1
end

-- 防止异常大库存值（库存不能超过1000000）
if currentStock > 1000000 then
    redis.call('HSET', 'debug:stock_operations', stockKey .. ':prededuct_error:' .. timestamp, 
        'error=excessive_stock_value,value=' .. currentStock)
    return -1
end

-- 检查库存是否充足
if currentStock < deductQuantity then
    redis.call('HSET', 'debug:stock_operations', stockKey .. ':prededuct_insufficient:' .. timestamp, 
        'current=' .. currentStock .. ',requested=' .. deductQuantity)
    return 0
end

-- 执行库存扣减
local newStock = currentStock - deductQuantity

-- 二次检查：确保扣减后库存不为负数（防止并发问题）
if newStock < 0 then
    redis.call('HSET', 'debug:stock_operations', stockKey .. ':prededuct_concurrent_error:' .. timestamp, 
        'current=' .. currentStock .. ',deduct=' .. deductQuantity .. ',result=' .. newStock)
    return 0  -- 返回库存不足
end

redis.call('SET', stockKey, newStock)

-- 设置过期时间（如果指定）
if expireTime and expireTime > 0 then
    redis.call('EXPIRE', stockKey, expireTime)
end

-- 记录成功操作
redis.call('HSET', 'debug:stock_operations', stockKey .. ':prededuct_success:' .. timestamp, 
    'before=' .. currentStock .. ',after=' .. newStock .. ',deducted=' .. deductQuantity)

return 1