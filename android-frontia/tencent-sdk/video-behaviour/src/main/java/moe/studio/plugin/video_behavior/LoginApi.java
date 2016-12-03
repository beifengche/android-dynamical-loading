package moe.studio.plugin.video_behavior;

import android.content.Context;
import moe.studio.frontia.bridge.host.hostapi.BaseApi;

/**
 * Created by kaede on 2015/12/7.
 */
public abstract class LoginApi extends BaseApi {

	abstract public boolean isLogined();

	abstract public void goToLogin(Context context);

}
