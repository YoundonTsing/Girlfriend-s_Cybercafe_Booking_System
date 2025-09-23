-- 简单的Redis测试脚本
local key = KEYS[1]
local value = ARGV[1]

-- 写入调试信息
redis.call('SET', 'simple_test_key', key)
redis.call('SET', 'simple_test_value', value)
redis.call('SET', 'simple_test_executed', 'true')

return 1