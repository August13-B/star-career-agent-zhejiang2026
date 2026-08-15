package org.example.web.tool;

/**
 * 雪花算法ID生成工具类（缩短位数版 + 高并发版）
 * 输入类别整数，输出分布式ID
 * 修改内容：
 * 1. 时间戳位数减少到32位
 * 2. 序列号位数增加到12位（每毫秒4096个ID，大幅提升并发）
 * 3. 起始时间改为1970年1月1日
 */
public class SnowIdCreater {

    // ==============================Fields===========================================
    /**
     * 开始时间截 (1970-01-01 00:00:00 UTC)
     */
    private static final long TWEPOCH = 0L;

    /**
     * 机器id/类别所占的位数（0-31）
     */
    private static final long WORKER_ID_BITS = 5L;

    /**
     * 序列在id中占的位数 - 提升到12位（每毫秒4096个ID，高并发）
     */
    private static final long SEQUENCE_BITS = 12L;

    /**
     * 机器ID向左移12位
     */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /**
     * 时间截向左移17位(5+12)
     */
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    /**
     * 生成序列的掩码，这里为4095 (0b111111111111=0xfff=4095)
     */
    private static final long SEQUENCE_MASK = -1L ^ (-1L << SEQUENCE_BITS);

    /**
     * 毫秒内序列(0~4095)
     */
    private static long sequence = 0L;

    /**
     * 上次生成ID的时间截
     */
    private static long lastTimestamp = -1L;

    // ==============================Method==========================================

    /**
     * 根据类别生成ID
     * @param category 类别代码 (0-31)
     * @return 分布式ID
     */
    public static synchronized long generateId(int category) {
        if (category < 0 || category > 31) {
            throw new IllegalArgumentException("Category must be between 0 and 31");
        }

        long timestamp = timeGen();

        // 如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过这个时候应当抛出异常
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(
                    String.format("Clock moved backwards. Refusing to generate id for %d milliseconds",
                            lastTimestamp - timestamp));
        }

        // 如果是同一时间生成的，则进行毫秒内序列
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            // 毫秒内序列溢出
            if (sequence == 0) {
                // 阻塞到下一个毫秒,获得新的时间戳
                timestamp = tilNextMillis(lastTimestamp);
            }
        }
        // 时间戳改变，毫秒内序列重置
        else {
            sequence = 0L;
        }

        // 上次生成ID的时间截
        lastTimestamp = timestamp;

        // 移位并通过或运算拼到一起组成64位的ID
        return ((timestamp - TWEPOCH) << TIMESTAMP_LEFT_SHIFT)
                | ((long) category << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 阻塞到下一个毫秒，直到获得新的时间戳
     * @param lastTimestamp 上次生成ID的时间截
     * @return 当前时间戳
     */
    private static long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 返回以毫秒为单位的当前时间
     * @return 当前时间(毫秒)
     */
    private static long timeGen() {
        return System.currentTimeMillis();
    }
}