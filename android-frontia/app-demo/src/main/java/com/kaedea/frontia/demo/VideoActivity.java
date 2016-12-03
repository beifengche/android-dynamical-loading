package com.kaedea.frontia.demo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.List;

import edu.gemini.tinyplayer.R;
import moe.studio.frontia.Frontia;
import moe.studio.frontia.core.PluginManager;
import moe.studio.frontia.core.PluginRequest;
import moe.studio.frontia.error.IllegalPluginException;
import moe.studio.frontia.update.PluginController;
import moe.studio.plugin.video_behavior.ITencentVideo;

public class VideoActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = "VideoActivity";

    int index = 0;
    private String[] mVideoId = {
            "t001469z2ma", "y0015vw2o7f",
            "y0015vw2o7f", "j0015lqcpcu",
            "e0015jga0wp", "j0137p2txbs",
            "y0016j6llrg",//付费id,使用这个vid 的时候需要设置清晰度不能为mp4和msd
            "a0012p8g8cr",// 动漫
            "9Wzab3vNJ8b", // 试看
            "q0013te787c",
            "t0016jjsrri",   //横有黑边
            "y0012j6s11e",  // 竖有黑边，西游降魔
            "100003600", // 直播 深圳卫视
            "100002500",
            "r0016w5wxcw"
    };
    private ITencentVideo mTencentVideo;
    private LinearLayout mVideoContainer;
    private Button mBtnInitSdk;
    private Button mBtnInitSdkOnline;
    private Button mBtnPlayNext;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        mVideoContainer = (LinearLayout) this.findViewById(R.id.player);
        mBtnInitSdk = (Button) this.findViewById(R.id.btn_init_sdk);
        mBtnInitSdkOnline = (Button) this.findViewById(R.id.btn_init_sdk_online);
        mBtnPlayNext = (Button) this.findViewById(R.id.btn_play_next);
        setListener();
        mHandler = new Handler(Looper.myLooper());
    }

    protected void setListener() {
        mBtnInitSdk.setOnClickListener(this);
        mBtnInitSdkOnline.setOnClickListener(this);
        mBtnPlayNext.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_init_sdk: // 加载Assets内部插件
                // 静态调试插件
                /*mTencentVideo = Entry.getTencentVideo(this);
                if (mTencentVideo != null) {
					mVideoContainer.addView(mTencentVideo.getVideoView(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
					mBtnPlayNext.setEnabled(true);
				}*/

                mBtnInitSdk.setText("释放插件中…");
                mBtnInitSdk.setEnabled(false);
                mBtnInitSdkOnline.setEnabled(false);
                AssetsVideoRequest assetsVideoRequest = new AssetsVideoRequest();
                assetsVideoRequest.setListener(new PluginController.UpdateListenerImpl() {

                    public void onPostLoad(int sate, PluginRequest pluginRequest) {
                        if (pluginRequest.getState() == PluginRequest.States.REQUEST_LOAD_PLUGIN_SUCCESS) {
                            try {
                                mTencentVideo = (ITencentVideo) pluginRequest.getPlugin().getPluginBehaviour(VideoActivity.this);
                                mVideoContainer.addView(mTencentVideo.getVideoView(),
                                        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT));

                                mBtnPlayNext.setEnabled(true);
                                mBtnInitSdk.setText("加载成功");

                            } catch (IllegalPluginException e) {
                                e.printStackTrace();
                                Toast.makeText(VideoActivity.this, "插件加载失败", Toast.LENGTH_LONG).show();
                                mBtnInitSdk.setEnabled(true);
                                mBtnInitSdkOnline.setEnabled(true);
                            }
                        }
                    }
                });

                Frontia.instance().addAsync(assetsVideoRequest,
                        PluginManager.Mode.MODE_UPDATE | PluginManager.Mode.MODE_LOAD);
                break;

            case R.id.btn_init_sdk_online: // 加载在线插件
                mBtnInitSdkOnline.setText("下载插件中…");
                mBtnInitSdk.setEnabled(false);
                mBtnInitSdkOnline.setEnabled(false);
                OnlineVideoRequest request = new OnlineVideoRequest();
                request.getController().setListener(new PluginController.UpdateListenerImpl() {
                    public void onProgress(int sate, PluginRequest pluginRequest, final float progress) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mBtnInitSdkOnline.setText("下载插件中… " + (int) (progress * 100) + "%");
                            }
                        });
                    }

                    public void onPostLoad(int sate, PluginRequest pluginRequest) {
                        if (pluginRequest.getState() == PluginRequest.States.REQUEST_LOAD_PLUGIN_SUCCESS) {
                            try {
                                mTencentVideo = (ITencentVideo) pluginRequest.getPlugin().getPluginBehaviour(VideoActivity.this);
                                mVideoContainer.addView(mTencentVideo.getVideoView(),
                                        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT));

                                mBtnPlayNext.setEnabled(true);
                                mBtnInitSdkOnline.setText("加载成功");

                            } catch (IllegalPluginException e) {
                                e.printStackTrace();
                                Toast.makeText(VideoActivity.this, "插件加载失败", Toast.LENGTH_LONG).show();
                                mBtnInitSdk.setEnabled(true);
                                mBtnInitSdkOnline.setEnabled(true);
                            }

                        } else {
                            List<Exception> exceptions = pluginRequest.getExceptions();
                            if (exceptions != null) {
                                mBtnInitSdkOnline.setText(exceptions.get(exceptions.size() - 1)
                                        .getLocalizedMessage());
                            }

                            Toast.makeText(VideoActivity.this, "插件加载失败", Toast.LENGTH_LONG).show();
                            mBtnInitSdk.setEnabled(true);
                            mBtnInitSdkOnline.setEnabled(true);
                        }
                    }
                });

                Frontia.instance().addAsync(request,
                        PluginManager.Mode.MODE_UPDATE | PluginManager.Mode.MODE_LOAD);
                break;

            case R.id.btn_play_next:
                if (mTencentVideo != null) {
                    mTencentVideo.play(mVideoId[index % mVideoId.length], 2);
                    index++;
                }
                break;
        }
    }
}
