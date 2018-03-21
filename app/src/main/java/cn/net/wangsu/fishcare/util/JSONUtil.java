package cn.net.wangsu.fishcare.util;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by lorytech on 2018/3/21.
 */

public class JSONUtil {

    public static Map<String, String> toMap(String jsonString) throws JSONException {

        JSONObject jsonObject = new JSONObject(jsonString);

        Map<String, String> result = new HashMap<String, String>();
        Iterator<?> iterator = jsonObject.keys();
        String key = null;
        String value = null;

        while (iterator.hasNext()) {

            key = (String) iterator.next();
            value = jsonObject.getString(key);
            result.put(key, value);

        }
        return result;
    }
}
