-- 请求路径恢复并立即发号：先按 max(当前值, MySQL 高水位) 校准，再原子自增并刷新 TTL。
-- 返回 {sequence, calibrationBefore, calibrationAfter}，供恢复日志记录校准前后值和序列差值。
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
local sequence = redis.call('INCR', KEYS[1])
redis.call('PEXPIRE', KEYS[1], ARGV[2])
return {sequence, before, after}
