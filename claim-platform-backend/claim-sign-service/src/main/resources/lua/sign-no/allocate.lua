-- 正常发号：计数器缺失返回 -1，计数器非法返回 -2，否则原子自增并刷新 TTL。
local raw = redis.call('GET', KEYS[1])
if not raw then
    return -1
end
local current = tonumber(raw)
if current == nil or current < 0 or current % 1 ~= 0 then
    return -2
end
local sequence = redis.call('INCR', KEYS[1])
redis.call('PEXPIRE', KEYS[1], ARGV[1])
return sequence
