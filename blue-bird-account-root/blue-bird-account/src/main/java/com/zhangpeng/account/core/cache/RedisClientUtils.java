package com.zhangpeng.account.core.cache;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.SortingParams;
import redis.clients.jedis.Tuple;

import java.util.*;

@Component
public class RedisClientUtils implements InitializingBean, ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(RedisClientUtils.class);

    private static JedisPool jedisPool;
    private static ApplicationContext context;
    private static final String LOCK_SUCCESS = "OK";
    private static final String SET_IF_NOT_EXIST = "NX";
    private static final String SET_WITH_EXPIRE_TIME = "PX";
    private static final Long RELEASE_SUCCESS = 1L;

    @Override
    public void afterPropertiesSet() {
        jedisPool = context.getBean(JedisPool.class);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    /**
     * 尝试获取分布式锁
     *
     * @param lockKey    锁
     * @param requestId  请求标识
     * @param expireTime 超期时间
     * @return 是否获取成功
     */
    public static boolean tryGetDistributedLock(String lockKey, String requestId, int expireTime) {

        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null || lockKey == null) return false;

        try {
            String result = shardedJedis.set(lockKey, requestId, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, expireTime);
            if (LOCK_SUCCESS.equals(result)) {
                return true;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return false;
    }


    /**
     * 校验IP
     *
     * @param key   令牌Key
     * @param count 令牌數量
     * @param timer 時間
     * @return 是否释放成功
     */
    public static boolean checkLimitIp(String key, String count, String timer) {
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) return false;
        try {
            String script = "local res = redis.call('INCR', KEYS[1]); if res == 1 then return redis.call('EXPIRE', KEYS[1], ARGV[2]) else if res <= tonumber(ARGV[1]) then return 1 else return 0 end end";
            Object result = shardedJedis.eval(script, Collections.singletonList(key), Arrays.asList(count, timer));

            if (RELEASE_SUCCESS.equals(result)) {
                return true;
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return false;
    }

    /**
     * 獲取Hash数据，并追加时间
     *
     * @param key   令牌Key
     * @param field 对应字段
     * @param timer 延时时间
     * @return
     */
    public static String hget(String key, String field, Integer timer) {
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) return null;

        try {
            String script = "if redis.call('EXISTS', KEYS[1]) > 0 then local res = redis.call('hget', KEYS[1], KEYS[2]); if(res==nil) then return '' else redis.call('EXPIRE', KEYS[1], ARGV[1]); return res end else return '' end";
            Object obj = shardedJedis.eval(script, Arrays.asList(key, field), Collections.singletonList(String.valueOf(timer)));
            if (!ObjectUtils.isEmpty(obj)) {
                return obj.toString();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return null;
    }

    /**
     * 獲取Hash数据，并追加时间
     *
     * @param key   令牌Key
     * @param args  对应字段
     * @param timer 延时时间
     * @return
     */
    public static boolean hset(String key, List<String> args, Integer timer) {
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) return false;

        try {
            String script = "local key for i,j in ipairs(ARGV)do if i%2 == 0 then redis.call('hset', KEYS[1], key,j) else key = j end end return redis.call('EXPIRE', KEYS[1], KEYS[2])";
            Object result = shardedJedis.eval(script, Arrays.asList(key, String.valueOf(timer)), args);
            if (RELEASE_SUCCESS.equals(result)) {
                return true;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return false;
    }

    /**
     * 獲取Hash数据，并追加时间
     *
     * @param key  令牌Key
     * @param args 对应字段
     * @return
     */
    public static boolean hset(String key, List<String> args) {
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) return false;

        try {
            String script = "local key for i,j in ipairs(ARGV)do if i%2 == 0 then redis.call('hset', KEYS[1], key,j) else key = j end end return 1";
            Object result = shardedJedis.eval(script, Arrays.asList(key), args);
            if (RELEASE_SUCCESS.equals(result)) {
                return true;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return false;
    }

    /**
     * 释放分布式锁
     *
     * @param lockKey   锁
     * @param requestId 请求标识
     * @return 是否释放成功
     */
    public static boolean releaseDistributedLock(String lockKey, String requestId) {
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null || lockKey == null) return false;

        try {
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            Object result = shardedJedis.eval(script, Collections.singletonList(lockKey), Collections.singletonList(requestId));

            if (RELEASE_SUCCESS.equals(result)) {
                return true;
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return false;
    }

    private static Jedis getRedisClient() {
        try {
            return jedisPool.getResource();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public static void disconnect() {
        Jedis jedis = getRedisClient();
        jedis.disconnect();
    }

    /**
     * 设置单个值
     *
     * @param key
     * @param value
     * @return
     */
    public static String set(String key, String value) {
        String result = null;

        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.set(key, value);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    /**
     * 设置单个值
     *
     * @param key
     * @param value
     * @return
     */
    public static String set(String key, String value, int seconds) {
        String result = null;

        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.setex(key, seconds, value);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static void setList(String key, List<Object> list) {
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return;
        }

        try {
            shardedJedis.set(key.getBytes(), ObjectTranscoder.serialize(list));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
    }

    /**
     * 设置 map
     *
     * @param key 键
     * @param map 集合
     */
    public static void setMap(String key, Map<String, Object> map) {
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return;
        }

        try {
            shardedJedis.set(key.getBytes(), ObjectTranscoder.serialize(map));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getMap(String key) {
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null || !shardedJedis.exists(key)) {
            return null;
        }

        byte[] in = shardedJedis.get(key.getBytes());
        Map<String, Object> maps = null;

        try {
            maps = (Map<String, Object>) ObjectTranscoder.deserialize(in);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }

        return maps;
    }

    /**
     * 获取list
     *
     * @param key
     * @return list
     */
    @SuppressWarnings("unchecked")
    public static List<Object> getList(String key) {
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null || !shardedJedis.exists(key)) {
            return null;
        }

        byte[] in = shardedJedis.get(key.getBytes());
        List<Object> list = null;

        try {
            list = (List<Object>) ObjectTranscoder.deserialize(in);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }

        return list;
    }

    /**
     * 获取单个值
     *
     * @param key
     * @return
     */
    public static String get(String key) {
        String result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.get(key);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Boolean exists(String key) {
        Boolean result = false;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.exists(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static String type(String key) {
        String result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.type(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    /**
     * 在某段时间后失效
     *
     * @param key
     * @param seconds 单位秒
     * @return
     */
    public static Long expire(String key, int seconds) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            if (shardedJedis.exists(key))
                result = shardedJedis.expire(key, seconds);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    /**
     * 在某个时间点失效
     *
     * @param key
     * @param unixTime
     * @return
     */
    public static Long expireAt(String key, long unixTime) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.expireAt(key, unixTime);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long ttl(String key) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.ttl(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static boolean setbit(String key, long offset, boolean value) {
        Jedis shardedJedis = getRedisClient();
        boolean result = false;
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.setbit(key, offset, value);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static boolean getbit(String key, long offset) {
        Jedis shardedJedis = getRedisClient();
        boolean result = false;
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.getbit(key, offset);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static long setrange(String key, long offset, String value) {
        Jedis shardedJedis = getRedisClient();
        long result = 0;
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.setrange(key, offset, value);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static String getrange(String key, long startOffset, long endOffset) {
        Jedis shardedJedis = getRedisClient();
        String result = null;
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.getrange(key, startOffset, endOffset);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static String getSet(String key, String value) {
        String result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.getSet(key, value);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long setnx(String key, String value) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.setnx(key, value);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long setnx(String key, String value, int seconds) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.setnx(key, value);
            shardedJedis.expire(key, seconds);  //设定时间
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static String setex(String key, String value, int seconds) {
        String result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.setex(key, seconds, value);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long decrBy(String key, long integer) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.decrBy(key, integer);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long decr(String key) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.decr(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long incrBy(String key, long integer) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.incrBy(key, integer);
        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long incr(String key) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.incr(key);
            if (result == 1) {
                expire(key, RedisTimer.DAY);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long append(String key, String value) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.append(key, value);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static String substr(String key, int start, int end) {
        String result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.substr(key, start, end);

        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long hset(String key, String field, String value) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.hset(key, field, value);

        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static String hget(String key, String field) {
        String result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.hget(key, field);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long hsetnx(String key, String field, String value) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.hsetnx(key, field, value);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static String hmset(String key, Map<String, String> hash) {
        String result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.hmset(key, hash);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static List<String> hmget(String key, String... fields) {
        List<String> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.hmget(key, fields);

        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long hincrBy(String key, String field, long value) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.hincrBy(key, field, value);

        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Boolean hexists(String key, String field) {
        Boolean result = false;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.hexists(key, field);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long del(String key) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.del(key);

        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long hdel(String key, String... field) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.hdel(key, field);

        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long hlen(String key) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.hlen(key);

        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static List<String> hvals(String key) {
        List<String> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.hvals(key);

        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Map<String, String> hgetAll(String key) {
        Map<String, String> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.hgetAll(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long persist(String key) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.persist(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    // ================list ====== l表示 list或 left, r表示right====================
    public static Long rpush(String key, String string) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.rpush(key, string);

        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long lpush(String key, String... string) {

        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }
        try {
            result = shardedJedis.lpush(key, string);

        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long llen(String key) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.llen(key);

        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static List<String> lrange(String key, long start, long end) {
        List<String> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.lrange(key, start, end);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static String ltrim(String key, long start, long end) {
        String result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.ltrim(key, start, end);

        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static String lindex(String key, long index) {
        String result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.lindex(key, index);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static String lset(String key, long index, String value) {
        String result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.lset(key, index, value);

        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long lrem(String key, long count, String value) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.lrem(key, count, value);

        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static String lpop(String key) {
        String result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.lpop(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static String rpop(String key) {
        String result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.rpop(key);

        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    // return 1 add a not exist value ,
    // return 0 add a exist value
    public static Long sadd(String key, String... member) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.sadd(key, member);

        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Set<String> smembers(String key) {
        Set<String> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.smembers(key);

        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long srem(String key, String... member) {
        Jedis shardedJedis = getRedisClient();

        Long result = null;
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.srem(key, member);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static String spop(String key) {
        Jedis shardedJedis = getRedisClient();
        String result = null;
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.spop(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long scard(String key) {
        Jedis shardedJedis = getRedisClient();
        Long result = null;
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.scard(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Boolean sismember(String key, String member) {
        Jedis shardedJedis = getRedisClient();
        Boolean result = null;
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.sismember(key, member);
        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static String srandmember(String key) {
        Jedis shardedJedis = getRedisClient();
        String result = null;
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.srandmember(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long zadd(String key, double score, String member) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.zadd(key, score, member);
        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long zadd(String key, Map<String, Double> scoreMembers) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.zadd(key, scoreMembers);
        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Set<String> zrange(String key, int start, int end) {
        Set<String> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.zrange(key, start, end);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long zrem(String key, String member) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.zrem(key, member);
        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Double zincrby(String key, double score, String member) {
        Double result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {

            result = shardedJedis.zincrby(key, score, member);

        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long zrank(String key, String member) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.zrank(key, member);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long zrevrank(String key, String member) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {

            result = shardedJedis.zrevrank(key, member);

        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Set<String> zrevrange(String key, int start, int end) {
        Set<String> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {

            result = shardedJedis.zrevrange(key, start, end);

        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Set<Tuple> zrangeWithScores(String key, int start, int end) {
        Set<Tuple> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.zrangeWithScores(key, start, end);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Set<Tuple> zrevrangeWithScores(String key, int start, int end) {
        Set<Tuple> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {

            result = shardedJedis.zrevrangeWithScores(key, start, end);

        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long zcard(String key) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {

            result = shardedJedis.zcard(key);

        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Double zscore(String key, String member) {
        Double result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {

            result = shardedJedis.zscore(key, member);

        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static List<String> sort(String key) {
        List<String> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {

            result = shardedJedis.sort(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static List<String> sort(String key, SortingParams sortingParameters) {
        List<String> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {

            result = shardedJedis.sort(key, sortingParameters);

        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long zcount(String key, double min, double max) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {

            result = shardedJedis.zcount(key, min, max);

        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Set<String> zrangeByScore(String key, double min, double max) {
        Set<String> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.zrangeByScore(key, min, max);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Set<String> zrevrangeByScore(String key, double max, double min) {
        Set<String> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {

            result = shardedJedis.zrevrangeByScore(key, max, min);

        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Set<String> zrangeByScore(String key, double min, double max,
                                            int offset, int count) {
        Set<String> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {

            result = shardedJedis.zrangeByScore(key, min, max, offset, count);

        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Set<String> zrevrangeByScore(String key, double max, double min,
                                               int offset, int count) {
        Set<String> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {

            result = shardedJedis
                    .zrevrangeByScore(key, max, min, offset, count);

        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Set<Tuple> zrangeByScoreWithScores(String key, double min, double max) {
        Set<Tuple> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.zrangeByScoreWithScores(key, min, max);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Set<Tuple> zrevrangeByScoreWithScores(String key, double max,
                                                        double min) {
        Set<Tuple> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {

            result = shardedJedis.zrevrangeByScoreWithScores(key, max, min);

        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Set<Tuple> zrangeByScoreWithScores(String key, double min,
                                                     double max, int offset, int count) {
        Set<Tuple> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.zrangeByScoreWithScores(key, min, max,
                    offset, count);
        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Set<Tuple> zrevrangeByScoreWithScores(String key, double max,
                                                        double min, int offset, int count) {
        Set<Tuple> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.zrevrangeByScoreWithScores(key, max, min,
                    offset, count);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long zremrangeByRank(String key, int start, int end) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.zremrangeByRank(key, start, end);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long zremrangeByScore(String key, double start, double end) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.zremrangeByScore(key, start, end);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static String set(byte[] key, byte[] value) {
        String result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.set(key, value);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static byte[] get(byte[] key) {
        byte[] result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.get(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Boolean exists(byte[] key) {
        Boolean result = false;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.exists(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static String type(byte[] key) {
        String result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.type(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long expire(byte[] key, int seconds) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.expire(key, seconds);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long expireAt(byte[] key, long unixTime) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.expireAt(key, unixTime);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long ttl(byte[] key) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.ttl(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static byte[] getSet(byte[] key, byte[] value) {
        byte[] result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.getSet(key, value);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long setnx(byte[] key, byte[] value) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.setnx(key, value);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static String setex(byte[] key, int seconds, byte[] value) {
        String result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.setex(key, seconds, value);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long decrBy(byte[] key, long integer) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.decrBy(key, integer);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long decr(byte[] key) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.decr(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long incrBy(byte[] key, long integer) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.incrBy(key, integer);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long incr(byte[] key) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.incr(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long append(byte[] key, byte[] value) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.append(key, value);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static byte[] substr(byte[] key, int start, int end) {
        byte[] result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.substr(key, start, end);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long hset(byte[] key, byte[] field, byte[] value) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.hset(key, field, value);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static byte[] hget(byte[] key, byte[] field) {
        byte[] result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.hget(key, field);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long hsetnx(byte[] key, byte[] field, byte[] value) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.hsetnx(key, field, value);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static String hmset(byte[] key, Map<byte[], byte[]> hash) {
        String result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.hmset(key, hash);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static List<byte[]> hmget(byte[] key, byte[]... fields) {
        List<byte[]> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.hmget(key, fields);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long hincrBy(byte[] key, byte[] field, long value) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.hincrBy(key, field, value);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Boolean hexists(byte[] key, byte[] field) {
        Boolean result = false;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.hexists(key, field);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long hdel(byte[] key, byte[] field) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.hdel(key, field);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long hlen(byte[] key) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.hlen(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Set<byte[]> hkeys(byte[] key) {
        Set<byte[]> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.hkeys(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Set<String> hkeys(String key) {
        Set<String> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.hkeys(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Collection<byte[]> hvals(byte[] key) {
        Collection<byte[]> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.hvals(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Map<byte[], byte[]> hgetAll(byte[] key) {
        Map<byte[], byte[]> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.hgetAll(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long rpush(byte[] key, byte[] string) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.rpush(key, string);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long lpush(byte[] key, byte[] string) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.lpush(key, string);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long llen(byte[] key) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.llen(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static List<byte[]> lrange(byte[] key, int start, int end) {
        List<byte[]> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.lrange(key, start, end);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static String ltrim(byte[] key, int start, int end) {
        String result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.ltrim(key, start, end);
        } catch (Exception e) {
            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static byte[] lindex(byte[] key, int index) {
        byte[] result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.lindex(key, index);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static String lset(byte[] key, int index, byte[] value) {
        String result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.lset(key, index, value);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long lrem(byte[] key, int count, byte[] value) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.lrem(key, count, value);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static byte[] lpop(byte[] key) {
        byte[] result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.lpop(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static byte[] rpop(byte[] key) {
        byte[] result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.rpop(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long sadd(byte[] key, byte[] member) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.sadd(key, member);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Set<byte[]> smembers(byte[] key) {
        Set<byte[]> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.smembers(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long srem(byte[] key, byte[] member) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.srem(key, member);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static byte[] spop(byte[] key) {
        byte[] result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.spop(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long scard(byte[] key) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.scard(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Boolean sismember(byte[] key, byte[] member) {
        Boolean result = false;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.sismember(key, member);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static byte[] srandmember(byte[] key) {
        byte[] result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.srandmember(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long zadd(byte[] key, double score, byte[] member) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.zadd(key, score, member);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Set<byte[]> zrange(byte[] key, int start, int end) {
        Set<byte[]> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.zrange(key, start, end);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long zrem(byte[] key, byte[] member) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.zrem(key, member);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Double zincrby(byte[] key, double score, byte[] member) {
        Double result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.zincrby(key, score, member);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long zrank(byte[] key, byte[] member) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.zrank(key, member);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long zrevrank(byte[] key, byte[] member) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.zrevrank(key, member);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Set<byte[]> zrevrange(byte[] key, int start, int end) {
        Set<byte[]> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.zrevrange(key, start, end);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Set<Tuple> zrangeWithScores(byte[] key, int start, int end) {
        Set<Tuple> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.zrangeWithScores(key, start, end);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Set<Tuple> zrevrangeWithScores(byte[] key, int start, int end) {
        Set<Tuple> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.zrevrangeWithScores(key, start, end);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long zcard(byte[] key) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.zcard(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Double zscore(byte[] key, byte[] member) {
        Double result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.zscore(key, member);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static List<byte[]> sort(byte[] key) {
        List<byte[]> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.sort(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static List<byte[]> sort(byte[] key, SortingParams sortingParameters) {
        List<byte[]> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.sort(key, sortingParameters);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long zcount(byte[] key, double min, double max) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.zcount(key, min, max);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Set<byte[]> zrangeByScore(byte[] key, double min, double max) {
        Set<byte[]> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.zrangeByScore(key, min, max);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Set<byte[]> zrangeByScore(byte[] key, double min, double max,
                                            int offset, int count) {
        Set<byte[]> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.zrangeByScore(key, min, max, offset, count);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max) {
        Set<Tuple> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.zrangeByScoreWithScores(key, min, max);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Set<Tuple> zrangeByScoreWithScores(byte[] key, double min,
                                                     double max, int offset, int count) {
        Set<Tuple> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {

            result = shardedJedis.zrangeByScoreWithScores(key, min, max,
                    offset, count);

        } catch (Exception e) {

            log.error(e.getMessage(), e);

        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Set<byte[]> zrevrangeByScore(byte[] key, double max, double min) {
        Set<byte[]> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.zrevrangeByScore(key, max, min);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Set<byte[]> zrevrangeByScore(byte[] key, double max, double min,
                                               int offset, int count) {
        Set<byte[]> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis
                    .zrevrangeByScore(key, max, min, offset, count);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Set<Tuple> zrevrangeByScoreWithScores(byte[] key, double max,
                                                        double min) {
        Set<Tuple> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.zrevrangeByScoreWithScores(key, max, min);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Set<Tuple> zrevrangeByScoreWithScores(byte[] key, double max,
                                                        double min, int offset, int count) {
        Set<Tuple> result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.zrevrangeByScoreWithScores(key, max, min,
                    offset, count);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long zremrangeByRank(byte[] key, int start, int end) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.zremrangeByRank(key, start, end);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static Long zremrangeByScore(byte[] key, double start, double end) {
        Long result = null;
        Jedis shardedJedis = getRedisClient();
        if (shardedJedis == null) {
            return result;
        }

        try {
            result = shardedJedis.zremrangeByScore(key, start, end);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            shardedJedis.close();
        }
        return result;
    }

    public static void closeJedis(Jedis jedis) {
        jedis.close();
    }

}
