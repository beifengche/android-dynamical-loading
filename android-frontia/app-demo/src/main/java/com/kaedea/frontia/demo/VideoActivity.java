/*
 * Copyright (c) 2016. Kaede
 */

package com.kaedea.frontia.demo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kaedea.frontia.demo.tencent.AssetsVideoRequest;

import edu.gemini.tinyplayer.R;
import moe.studio.plugin.video_behavior.ITencentVideo;
import moe.studio.plugin.video_behavior.TencentVideoPlugin;
import moe.studio.frontia.Frontia;
import moe.studio.frontia.ext.PluginListener;

public class VideoActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String TAG = "plugin.video.demo";

    int index = 0;
    private String[] mVideoId = {
            "t001469z2ma", "y0015vw2o7f",
            "y0015vw2o7f", "j0015lqcpcu",
            "e0015jga0wp", "j0137p2txbs",
            "y0016j6llrg",  // 付费id,使用这个vid 的时候需要设置清晰度不能为mp4和msd
            "a0012p8g8cr",  // 动漫
            "9Wzab3vNJ8b",  // 试看
            "q0013te787c",
            "t0016jjsrri",  // 横有黑边
            "y0012j6s11e",  // 竖有黑边，西游降魔
            "100003600",    // 直播 深圳卫视
            "100002500",
            "r0016w5wxcw"
    };

    private Button mBtnInitSdk;
    private Button mBtnInitSdkOnline;
    private Button mBtnPlayNext;
    private TextView mTvOutput;
    private LinearLayout mVideoContainer;

    private Handler mHandler;
    private ITencentVideo mTencentVideo;
    private StringBuilder mOutput = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        mHandler = new Handler(Looper.myLooper());
        mVideoContainer = (LinearLayout) this.findViewById(R.id.player);
        mBtnInitSdk = (Button) this.findViewById(R.id.btn_init_sdk);
        mBtnInitSdkOnline = (Button) this.findViewById(R.id.btn_init_sdk_online);
        mBtnPlayNext = (Button) this.findViewById(R.id.btn_play_next);
        mTvOutput = (TextView) findViewById(R.id.output);

        setListener();
        output("Output");
    }

    private void setListener() {
        mBtnInitSdk.setOnClickListener(this);
        mBtnInitSdkOnline.setOnClickListener(this);
        mBtnPlayNext.setOnClickListener(this);
    }

    private void output(String msg) {
        mTvOutput.setText(mOutput.append(msg).append("\n").toString());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_init_sdk: // 加载Assets内部插件

                mBtnInitSdk.setEnabled(false);
                mBtnInitSdkOnline.setEnabled(false);
                AssetsVideoRequest assetsVideoRequest = new AssetsVideoRequest();
                assetsVideoRequest.setListener(new PluginListener.ListenerImpl<ITencentVideo, TencentVideoPlugin, AssetsVideoRequest>() {
                    @Override
                    public void onPreUpdate(AssetsVideoRequest request) {
                        output("释放插件中…");
                    }

                    @Override
                    public void onPostUpdate(AssetsVideoRequest request) {
                        output("释放插件完成");
                    }

                    @Override
                    public void onPreLoad(AssetsVideoRequest request) {
                        output("加载插件中…");
                    }

                    @Override
                    public void onPostLoad(AssetsVideoRequest request, TencentVideoPlugin plugin) {
                        output("加载插件中完成");
                    }

                    @Override
                    public void onGetBehavior(AssetsVideoRequest request, ITencentVideo behavior) {
                        mTencentVideo = behavior;
                        mTencentVideo.attach(VideoActivity.this);
                        mVideoContainer.addView(mTencentVideo.getVideoView(),
                                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT));

                        mBtnPlayNext.setEnabled(true);
                        output("加载成功");
                    }
                });

                Frontia.instance().addAsync(assetsVideoRequest,
                        Frontia.Mode.MODE_UPDATE | Frontia.Mode.MODE_LOAD);
                break;

            case R.id.btn_init_sdk_online: // 加载在线插件
//                mBtnInitSdkOnline.setText("下载插件中…");
//                mBtnInitSdk.setEnabled(false);
//                mBtnInitSdkOnline.setEnabled(false);
//                OnlineVideoRequest videoRequest = new OnlineVideoRequest();
//                videoRequest.setListener(new PluginListener.ListenerImpl() {
//                    public void onProgress(int sate, PluginRequest request, final float progress) {
//                        mHandler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                mBtnInitSdkOnline.setText("下载插件中… " + (int) (progress * 100) + "%");
//                            }
//                        });
//                    }
//
//                    public void onPostLoad(int sate, PluginRequest request) {
//                        if (request.getState() == LOA_SUCCESS) {
//                            try {
//                                mTencentVideo = (ITencentVideo) Frontia.instance()
//                                        .getBehavior(ITencentVideo.class, request.getPlugin(), VideoActivity.this);
//                                mTencentVideo.attach(VideoActivity.this);
//                                mVideoContainer.addView(mTencentVideo.getVideoView(),
//                                        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
//                                                ViewGroup.LayoutParams.MATCH_PARENT));
//
//                                mBtnPlayNext.setEnabled(true);
//                                mBtnInitSdkOnline.setText("加载成功");
//
//                            } catch (LoadError e) {
//                                e.printStackTrace();
//                                Toast.makeText(VideoActivity.this, "插件加载失败", Toast.LENGTH_LONG).show();
//                                mBtnInitSdk.setEnabled(true);
//                                mBtnInitSdkOnline.setEnabled(true);
//                            }
//
//                        } else {
//                            List<Exception> exceptions = request.getExceptions();
//                            if (exceptions != null) {
//                                mBtnInitSdkOnline.setText(exceptions.get(exceptions.size() - 1)
//                                        .getLocalizedMessage());
//                            }
//
//                            Toast.makeText(VideoActivity.this, "插件加载失败", Toast.LENGTH_LONG).show();
//                            mBtnInitSdk.setEnabled(true);
//                            mBtnInitSdkOnline.setEnabled(true);
//                        }
//                    }
//                });
//
//                Frontia.instance().addAsync(videoRequest,
//                        Frontia.Mode.MODE_UPDATE | Frontia.Mode.MODE_LOAD);
                break;

            case R.id.btn_play_next:
                if (mTencentVideo != null) {
                    output("Play AV " + mVideoId[index % mVideoId.length]);
                    mTencentVideo.play(mVideoId[index % mVideoId.length], 2);
                    index++;
                }

                break;
        }
    }
}
