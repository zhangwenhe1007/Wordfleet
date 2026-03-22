-- KEYS
-- 1 meta key             room:{id}:meta
-- 2 banned set           room:{id}:banned
-- 3 scores zset          room:{id}:scores
-- 4 streaks hash         room:{id}:streaks
-- 5 lockout key prefix   room:{id}:lockout:
--
-- ARGV
-- 1 userId
-- 2 nowMillis
-- 3 word
-- 4 points
-- 5 lockoutMillisOnInvalid
-- 6 lockoutMillisOnBanned

local metaKey = KEYS[1]
local bannedKey = KEYS[2]
local scoresKey = KEYS[3]
local streaksKey = KEYS[4]
local lockoutPrefix = KEYS[5]

local userId = ARGV[1]
local nowMillis = tonumber(ARGV[2])
local word = ARGV[3]
local points = tonumber(ARGV[4])
local lockoutInvalid = tonumber(ARGV[5])
local lockoutBanned = tonumber(ARGV[6])

local turnPlayer = redis.call('HGET', metaKey, 'turnPlayerId')
if turnPlayer ~= userId then
  return {err='NOT_YOUR_TURN'}
end

local turnEnds = tonumber(redis.call('HGET', metaKey, 'turnEndsAt') or '0')
if nowMillis > turnEnds then
  return {err='TURN_EXPIRED'}
end

local lockoutKey = lockoutPrefix .. userId
local lockoutTTL = redis.call('PTTL', lockoutKey)
if lockoutTTL and lockoutTTL > 0 then
  return {err='LOCKED_OUT'}
end

if redis.call('SISMEMBER', bannedKey, word) == 1 then
  redis.call('ZINCRBY', scoresKey, -2, userId)
  redis.call('HSET', streaksKey, userId, 0)
  redis.call('PSETEX', lockoutKey, lockoutBanned, '1')
  return {ok='BANNED_WORD'}
end

redis.call('SADD', bannedKey, word)
redis.call('ZINCRBY', scoresKey, points, userId)
redis.call('HINCRBY', streaksKey, userId, 1)
return {ok='ACCEPTED', points=tostring(points)}
