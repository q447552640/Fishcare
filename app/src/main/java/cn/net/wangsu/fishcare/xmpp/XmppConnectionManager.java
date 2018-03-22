package cn.net.wangsu.fishcare.xmpp;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;


/**
 * XMPP服务器连接工具类.
 *
 * @author shimiso
 */
public class XmppConnectionManager {
    private XMPPTCPConnection mConnection;
    private static XmppConnectionManager xmppConnectionManager;

    private String XMPP_DOMAIN = "app.aquaexcel.cn";
    private String XMPP_HOST = "app.aquaexcel.cn";

    private XmppConnectionManager() {

    }

    public static XmppConnectionManager getInstance() {
        if (xmppConnectionManager == null) {
            xmppConnectionManager = new XmppConnectionManager();
        }
        return xmppConnectionManager;
    }

    // init
    public XMPPTCPConnection init(LoginConfig loginConfig) {
        try {
            XMPPTCPConnectionConfiguration configuration =
                    XMPPTCPConnectionConfiguration.builder().setXmppDomain(XMPP_DOMAIN) // 设置域名
                            .setHostAddress(InetAddress.getByName(XMPP_HOST)) // 设置主机
                            .setPort(5222) // 设置端口
                            .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled) // 禁用SSL连接
                            .setCompressionEnabled(false) // 禁用SSL连接
                            //.setSendPresence(false) // 设置为离线状态
                            .setDebuggerEnabled(true) // 开启调试模式
                            .setSendPresence(true)
                            .build();
            // 设置需要经过同意才可以添加好友
            Roster.setDefaultSubscriptionMode(Roster.SubscriptionMode.accept_all);
            XMPPTCPConnection connection = new XMPPTCPConnection(configuration);
            connection.connect();// 连接, 可设置监听
            ReconnectionManager reconnectionManager = ReconnectionManager.getInstanceFor(connection);
            reconnectionManager.enableAutomaticReconnection();
            this.mConnection = connection;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        } catch (InterruptedException | IOException | XMPPException | SmackException e) {
            e.printStackTrace();
        }
        return this.mConnection;
    }

    /**
     * 返回一个有效的xmpp连接,如果无效则返回空.
     *
     * @return
     * @author shimiso
     * @update 2012-7-4 下午6:54:31
     */
    public XMPPTCPConnection getConnection() {
        if (mConnection == null) {
            throw new RuntimeException("请先初始化XMPPConnection连接");
        }
        return mConnection;
    }

    /**
     * 销毁xmpp连接.
     *
     * @author shimiso
     * @update 2012-7-4 下午6:55:03
     */
    public void disconnect() {
        if (mConnection != null) {
            mConnection.disconnect();
        }
    }

}
