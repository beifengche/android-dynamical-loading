package me.kaede.mainapp;


import android.content.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import edu.gemini.tinyplayer.BuildConfig;
import me.kaede.mainapp.bridge.LoginApiImpl;
import moe.studio.frontia.Frontia;
import moe.studio.frontia.bridge.host.HostApiManager;
import moe.studio.frontia.bridge.host.hostapi.BaseApi;
import moe.studio.frontia.core.PluginSetting;
import moe.studio.plugin.video_behavior.LoginApi;

/**
 * Created by kaede on 2015/12/7.
 */
public class Plugin {

	static Map<Class<? extends BaseApi>, Class<? extends BaseApi>> apisMap;

	static {
		apisMap = new HashMap<>();
		apisMap.put(LoginApi.class, LoginApiImpl.class);
		// apisMap.put(UserInfoApi.class, UserInfoApiImpl.class);
	}

	/**
	 * 初始化，启动插件前，要确保已经执行该初始化方法
	 */
	public static void init(Context context) {
		registerApis();
		PluginSetting setting = new PluginSetting.Builder()
				.setDebugMode(BuildConfig.DEBUG)
				.ignoreInstalledPlugin(false)
				.useCustomSignature(PLUGIN_SIGNATURE)
				.build();
		Frontia.instance().init(context, setting);
	}

	private static void registerApis() {
		Set<Class<? extends BaseApi>> keySet = apisMap.keySet();
		for (Class<? extends BaseApi> key :
				keySet) {
			Class<? extends BaseApi> value = apisMap.get(key);
			try {
				HostApiManager.getInstance().putApi(key, value.newInstance());
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	// 验证插件签名用的公钥
	static final String PLUGIN_SIGNATURE = "3082038130820269a0030201020204205afdd8300d" +
			"06092a864886f70d01010b05003071310b300906035504061302434e3111300f0603550408130853" +
			"68616e676861693111300f060355040713085368616e676861693111300f060355040a130842696c" +
			"6962696c693110300e060355040b1307416e64726f6964311730150603550403130e4b6165646520" +
			"416b617473756b69301e170d3136303432363036343734375a170d3431303432303036343734375a" +
			"3071310b300906035504061302434e3111300f060355040813085368616e676861693111300f0603" +
			"55040713085368616e676861693111300f060355040a130842696c6962696c693110300e06035504" +
			"0b1307416e64726f6964311730150603550403130e4b6165646520416b617473756b693082012230" +
			"0d06092a864886f70d01010105000382010f003082010a0282010100c7ba1b3653af76e153d9718e" +
			"0d722772503aec2d0a49b3bb65aa0a877e9c92858dabb317cca0161c43bc93d1552a7bc12cbe3705" +
			"af1750bc698290f6446ec92621c614355521d07946ab0c0a71ae82c9130d5cc524b541d7d4da0e93" +
			"8b355e3d707e67712ac883dd12ce37cb29df5616a684daa057472329623897d9aa7eb65e9fb9bcc3" +
			"f3c4fe20abaebb5a6af4d01a644df5e21654913857bb8de3ca5eae1bf65e9ccd5616305bfb2416d6" +
			"66126a8416d10e7b633f917d345f3f718bd302c967acbcb9d1c1c76feb81f80c04a8e65347260157" +
			"1184533b0621a964f663678e7ea5886c5346729f7495d6d19a6fde593a474bab95c5e0857b705784" +
			"5fd302650203010001a321301f301d0603551d0e04160414ebb732bc8ee5da442a9565cd5b131247" +
			"3cd63680300d06092a864886f70d01010b050003820101000fc903a8d0c78f7f6d34192d329229c3" +
			"75d4e31ef0e007d7d4ddee29c6a9711efab0c933d85a9a8ace412f5c5d3ea74305d7698e3478a5d2" +
			"c11b044811681a5a97773a12b9aec6f8f49d6fc4deac249a2f00da72682391fc2c03ab203d3e00e5" +
			"c695dd56600f52128798ed00ae166e6ae970078d0d5e19e0894cc198f99990fbed877207f113dfa2" +
			"c5c563fcb48923c3f5c628405148a7d4d51db7bb38f2f7e72197c5100b52e9da9bb9b69d9266a583" +
			"5f75d82037e79ff09430c13f0d2a39f749ad4edee1c5a0a8a53beae9f617541e530f4aec26843a99" +
			"61cda049841276d4b7c6146379fbd9244a3333a7dcd1dcd73f0fb1d2ac377d080011afbbb41dd4a3";
}
