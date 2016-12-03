package moe.studio.plugin.video_behavior;

import android.content.Context;
import moe.studio.frontia.bridge.plugin.BaseBehaviour;

/**
 * Created by kaede on 2016/4/8.
 */
public interface IToast extends BaseBehaviour {
	public void toast(Context context,String msg);
}
