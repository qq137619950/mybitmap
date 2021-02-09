package org.imei;

import org.roaringbitmap.RoaringBitmap;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 手机imei是15位，国产手机imei前两位是86，可以忽略不计
 * 提供imei存取的接口
 * @author gwk
 */
public class ImeiDataAdmin {
    private static String prex = "86";

    private ConcurrentMap<String, RoaringBitmap> data;

    public ImeiDataAdmin() {
        // 可以注册为单例，服务启动时候只调用一次，此处简化
        if (this.data == null) {
            this.data = new ConcurrentHashMap<>();
        }

    }

    /**
     * 在label下插入一个imei
     * @param label   标签
     * @param imei    imei
     * @return
     */
    public boolean setImei(String label, String imei) {
        if (!checkImei(imei)) {
            return false;
        }
        long l = Long.parseLong(imei.substring(2));
        if (data.get(label) == null) {
            data.put(label, new RoaringBitmap());
        }
        data.get(label).add(l);
        return true;
    }

    public boolean getIfExists(String label, String imei) {
        if (!checkImei(imei)) {
            return false;
        }
        long l = Long.parseLong(imei.substring(2));
        return data.get(label) != null && data.get(label).contains(l);
    }

    private boolean checkImei(String imei) {
        if (imei.length() != 15) {
            return false;
        }
        return true;
    }
}
