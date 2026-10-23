-- 只校准不消耗流水号：取 max(当前值, MySQL 高水位) 写回并刷新 TTL，返回校准前后值。
local raw = redis.call('GET', KEYS[1])
local before = tonumber(raw)
if before == nil or before < 0 or before % 1 ~= 0 then
    before = 0
end
local target = tonumber(ARGV[1])
if target == nil or target < 0 then
    return redis.error_reply('invalid high water mark')
end
local after = before
if target > after then
    after = target
end
redis.call('SET', KEYS[1], after)
redis.call('PEXPIRE', KEYS[1], ARGV[2])
return {before, after}
