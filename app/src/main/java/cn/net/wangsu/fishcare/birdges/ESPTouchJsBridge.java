package cn.net.wangsu.fishcare.birdges;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.espressif.iot.esptouch.EsptouchTask;
import com.espressif.iot.esptouch.IEsptouchListener;
import com.espressif.iot.esptouch.IEsptouchResult;
import com.espressif.iot.esptouch.IEsptouchTask;
import com.espressif.iot.esptouch.demo_activity.EspWifiAdminSimple;
import com.espressif.iot.esptouch.task.__IEsptouchTask;

import org.json.JSONArray;

import java.util.List;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.DHInterface.StandardFeature;
import io.dcloud.common.util.JSUtil;

/**
 * Created by lorytech on 2018/1/18.
 */

public class ESPTouchJsBridge extends StandardFeature {

    @Override
    public void onStart(Context pContext, Bundle pSavedInstanceState, String[] pRuntimeArgs) {
        mWifiAdmin = new EspWifiAdminSimple(pContext);
        /**
         * 如果需要在应用启动时进行初始化，可以继承这个方法，并在properties.xml文件的service节点添加扩展插件的注册即可触发onStart方法
         * */
    }

    private EspWifiAdminSimple mWifiAdmin;

    private String mTvApSsid = "";

    private String TAG = "FishCare";

    private IWebview wifiWebView = null;
    private String wifiSetCallBackID = null;

    public void getWifi(IWebview pWebview, JSONArray array) {
        // 原生代码中获取JS层传递的参数，
        // 参数的获取顺序与JS层传递的顺序一致
        String CallBackID = array.optString(0);
//            JSONArray newArray = new JSONArray();
//            newArray.put(array.optString(1));
//            newArray.put(array.optString(2));
//            newArray.put(array.optString(3));
//            newArray.put(array.optString(4));
        // 调用方法将原生代码的执行结果返回给js层并触发相应的JS层回调函数
        if (mWifiAdmin == null) {
            mWifiAdmin = new EspWifiAdminSimple(pWebview.getContext());
        }
        String apSsid = mWifiAdmin.getWifiConnectedSsid();
        if (apSsid != null) {
            mTvApSsid = (apSsid);
        } else {
            mTvApSsid = ("");
        }
        Log.i(TAG, mTvApSsid);
        // check whether the wifi is connected
        boolean isApSsidEmpty = TextUtils.isEmpty(apSsid);

        JSONArray newArray = new JSONArray();
        newArray.put(mTvApSsid);
        newArray.put(isApSsidEmpty);
        JSUtil.execCallback(pWebview, CallBackID, newArray, JSUtil.OK, false);
    }

    public void setTerminalWifi(IWebview pWebview, JSONArray array) {
        // 原生代码中获取JS层传递的参数，
        // 参数的获取顺序与JS层传递的顺序一致
        this.wifiWebView = pWebview;
        this.wifiSetCallBackID = array.optString(0);
        // 调用方法将原生代码的执行结果返回给js层并触发相应的JS层回调函数
        if (mWifiAdmin == null) {
            mWifiAdmin = new EspWifiAdminSimple(pWebview.getContext());
        }
        String apBssid = mWifiAdmin.getWifiConnectedBssid();
        EsptouchAsyncTask3 esptouchAsyncTask3=new EsptouchAsyncTask3();
        esptouchAsyncTask3.setContext(pWebview.getContext());
        esptouchAsyncTask3.execute(array.optString(1), apBssid, array.optString(2), array.optString(3));
    }

    private IEsptouchListener myListener = new IEsptouchListener() {
        @Override
        public void onEsptouchResultAdded(final IEsptouchResult result) {
//            onEsptoucResultAddedPerform(result);
            if (wifiSetCallBackID != null) {
                if (result.isSuc()) {
                    JSONArray newArray = new JSONArray();
                    String regex = "(.{2})";
                    String mac= result.getBssid().replaceAll (regex, "$1:");
                    mac=mac.substring(0,mac.length()-1);
                    newArray.put(mac);
                    newArray.put(result.getInetAddress());
                    JSUtil.execCallback(wifiWebView, wifiSetCallBackID, newArray, JSUtil.OK, false);
                } else {
                    JSONArray newArray = new JSONArray();
                    if (result.isCancelled()) {

                        newArray.put("用户取消了操作。");
                    } else {
                        newArray.put("未知错误。");
                    }
                    JSUtil.execCallback(wifiWebView, wifiSetCallBackID, newArray, JSUtil.ERROR, false);
                }

            }
        }
    };

    private class EsptouchAsyncTask3 extends AsyncTask<String, Void, List<IEsptouchResult>> {

        private ProgressDialog mProgressDialog;

        private IEsptouchTask mEsptouchTask;
        // without the lock, if the user tap confirm and cancel quickly enough,
        // the bug will arise. the reason is follows:
        // 0. task is starting created, but not finished
        // 1. the task is cancel for the task hasn't been created, it do nothing
        // 2. task is created
        // 3. Oops, the task should be cancelled, but it is running
        private final Object mLock = new Object();

        private Context currentContext;

        public void setContext(Context context) {
            currentContext = context;
        }

        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(currentContext);
            mProgressDialog
                    .setMessage("正在配置设备，请稍等...");
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    synchronized (mLock) {
                        if (__IEsptouchTask.DEBUG) {
                            Log.i(TAG, "progress dialog is canceled");
                        }
                        if (mEsptouchTask != null) {
                            mEsptouchTask.interrupt();
                        }
                    }
                }
            });
            mProgressDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                    "Waiting...", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            mProgressDialog.show();
            mProgressDialog.getButton(DialogInterface.BUTTON_POSITIVE)
                    .setEnabled(false);
        }

        @Override
        protected List<IEsptouchResult> doInBackground(String... params) {
            int taskResultCount = -1;
            synchronized (mLock) {
                // !!!NOTICE
                String apSsid = mWifiAdmin.getWifiConnectedSsidAscii(params[0]);
                String apBssid = params[1];
                String apPassword = params[2];
                String taskResultCountStr = params[3];
                taskResultCount = Integer.parseInt(taskResultCountStr);
                mEsptouchTask = new EsptouchTask(apSsid, apBssid, apPassword, currentContext);
                mEsptouchTask.setEsptouchListener(myListener);
            }
            List<IEsptouchResult> resultList = mEsptouchTask.executeForResults(taskResultCount);
            return resultList;
        }

        @Override
        protected void onPostExecute(List<IEsptouchResult> result) {
            mProgressDialog.getButton(DialogInterface.BUTTON_POSITIVE)
                    .setEnabled(true);
            mProgressDialog.getButton(DialogInterface.BUTTON_POSITIVE).setText(
                    "Confirm");
            IEsptouchResult firstResult = result.get(0);
            // check whether the task is cancelled and no results received
            if (!firstResult.isCancelled()) {
                int count = 0;
                // max results to be displayed, if it is more than maxDisplayCount,
                // just show the count of redundant ones
                final int maxDisplayCount = 5;
                // the task received some results including cancelled while
                // executing before receiving enough results
                if (firstResult.isSuc()) {
                    StringBuilder sb = new StringBuilder();
                    for (IEsptouchResult resultInList : result) {
                        sb.append("Esptouch success, bssid = "
                                + resultInList.getBssid()
                                + ",InetAddress = "
                                + resultInList.getInetAddress()
                                .getHostAddress() + "\n");
                        count++;
                        if (count >= maxDisplayCount) {
                            break;
                        }
                    }
                    if (count < result.size()) {
                        sb.append("\nthere's " + (result.size() - count)
                                + " more result(s) without showing\n");
                    }
                    mProgressDialog.setMessage(sb.toString());
                } else {
                    mProgressDialog.setMessage("Esptouch fail");
                }
            }
        }
    }
}
