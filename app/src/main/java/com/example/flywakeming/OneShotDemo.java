package com.example.flywakeming;

import java.io.File;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.GrammarListener;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvent;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.VoiceWakeuper;
import com.iflytek.cloud.WakeuperListener;
import com.iflytek.cloud.WakeuperResult;
import com.iflytek.cloud.util.ResourceUtil;
import com.iflytek.cloud.util.ResourceUtil.RESOURCE_TYPE;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SeekBar.OnSeekBarChangeListener;

import androidx.core.app.ActivityCompat;

public class OneShotDemo extends Activity{
	private MediaPlayer mediaPlayer;
	private MediaPlayer questionPlayer;
	//初始化音频管理器
	private AudioManager mAudioManager;
	private String TAG = "ivw";
	private Toast mToast;
	private TextView textView;
	// 语音唤醒对象
	private VoiceWakeuper mIvw;
	// 语音识别对象
	private SpeechRecognizer mAsr;
	// 唤醒结果内容
	private String resultString;

	private int curThresh = 1450;
	// 识别结果内容
	private String recoString;
	// 本地语法id
	private String mLocalGrammarID=null;

	// 本地语法文件
	private String mLocalGrammar = null;
	// 本地语法构建路径	
	private String grmPath = Environment.getExternalStorageDirectory().getAbsolutePath()
			+ "/msc/test";
	// 引擎类型
	private String mEngineType = SpeechConstant.TYPE_LOCAL;
	//第几个景点
	private int posnumber=1;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.oneshot_activity);
		requestPermissions();

		//播放问题语音
		questionPlayer = new MediaPlayer();

		mediaPlayer = new MediaPlayer();
		//初始化音频管理器
		mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		try {
			String filename = "/storage/emulated/0/景点";
			filename=filename+posnumber+".mp3";
			//File file = new File(Environment.getExternalStorageDirectory(), "景点1.mp3");
			mediaPlayer.setDataSource(filename); // 指定音频文件的路径/storage/emulated/0/music.mp3
			//mediaPlayer.setLooping(true);//设置为循环播放
			mediaPlayer.prepare(); // 让MediaPlayer进入到准备状态

			Log.e("11",filename);
		} catch (Exception e) {
			e.printStackTrace();
		}


		StringBuffer param = new StringBuffer();
		param.append("appid="+getString(R.string.app_id));
		param.append(",");
		// 设置使用v5+
		param.append(SpeechConstant.ENGINE_MODE+"="+SpeechConstant.MODE_MSC);
		SpeechUtility.createUtility(this, param.toString());
		initUI();
		
		// 初始化唤醒对象
		mIvw = VoiceWakeuper.createWakeuper(this, null);
		// 初始化识别对象---唤醒+识别,用来构建语法
		//mAsr = SpeechRecognizer.createRecognizer(this, mInitListener);
		mAsr = SpeechRecognizer.createRecognizer(this, mInitListener);
		if(mAsr==null){
			Log.e(TAG,"masr is null");
		}
		// 初始化语法文件
		mLocalGrammar = readFile(this, "dynasty1.bnf", "utf-8");

		initgrammar();

		initdata();
		mediaPlayer.start(); // 开始播放
		mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				new Thread(new ThreadShow()).start();
				Log.e("44","laile");
			}
		});
	}
/*
	@Override
	public void run() {
		//while (true) {
		Log.e("44","laile");
			try {
				Thread.sleep(10000);//线程暂停10秒，单位毫秒
				Message message=new Message();
				message.what=1;
				handler.sendMessage(message);//发送消息
			} catch (InterruptedException e) {
// TODO Auto-generated catch block
				e.printStackTrace();
			}
		//}
	}
	Handler handler = new Handler() {
		public void handleMessage(Message msg) {
		//要做的事情 
			String filename = "/storage/emulated/0/dynasty";
			filename=filename+posnumber+".mp3";
			try {
				if(mediaPlayer.isPlaying())
				{
					mediaPlayer.pause();
				}
				questionPlayer.setDataSource(filename); // 指定音频文件的路径/storage/emulated/0/music.mp3
				//mediaPlayer.setLooping(true);//设置为循环播放
				questionPlayer.prepare(); // 让MediaPlayer进入到准备状态
				questionPlayer.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
			questionPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
				@Override
				public void onCompletion(MediaPlayer mp) {
					setgram();
				}
			});
			super.handleMessage(msg);
		}
	};*/
	// handler类接收数据
@SuppressLint("HandlerLeak")
Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.what == 1) {
				Log.e("44444","laile");
				String filename = "/storage/emulated/0/dynasty";
				filename=filename+posnumber+".mp3";
				try {
					if(mediaPlayer.isPlaying())
					{
						mediaPlayer.pause();
					}
					questionPlayer.setDataSource(filename); // 指定音频文件的路径/storage/emulated/0/music.mp3
					//mediaPlayer.setLooping(true);//设置为循环播放
					questionPlayer.prepare(); // 让MediaPlayer进入到准备状态
					questionPlayer.start();
				} catch (Exception e) {
					e.printStackTrace();
				}
				questionPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
					@Override
					public void onCompletion(MediaPlayer mp) {
						setgram();
					}
				});
			}
		};
	};

	// 线程类
	class ThreadShow implements Runnable {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			//while (true) {
				try {
					Thread.sleep(6000);
					Message msg = new Message();
					msg.what = 1;
					handler.sendMessage(msg);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			//}
		}
	}

	private void initUI() {
		textView = (TextView) findViewById(R.id.txt_show_msg);
		mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
	}
	
	GrammarListener grammarListener = new GrammarListener() {
		@Override
		public void onBuildFinish(String grammarId, SpeechError error) {
			if (error == null) {
				mLocalGrammarID = grammarId;
				showTip("语法构建成功：" + grammarId);
				Log.e("gram","mLocalGrammarID");
			} else {
				showTip("语法构建失败,错误码：" + error.getErrorCode()+",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
			}
		}
	};

	private void initgrammar() {
		int ret = 0;
		mAsr.setParameter(SpeechConstant.PARAMS, null);
		mAsr.setParameter(SpeechConstant.TEXT_ENCODING, "utf-8");
		// 设置引擎类型
		mAsr.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
		// 设置语法构建路径
		mAsr.setParameter(ResourceUtil.GRM_BUILD_PATH, grmPath);
		// 设置本地识别使用语法id
		mAsr.setParameter(SpeechConstant.LOCAL_GRAMMAR, "dynasty1");
		// 设置识别的门限值
		mAsr.setParameter(SpeechConstant.MIXED_THRESHOLD, "30");
		// 设置资源路径
		// 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
		mAsr.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
		mAsr.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/asr.wav");
		mAsr.setParameter(ResourceUtil.ASR_RES_PATH, getResourcePath());
		ret = mAsr.buildGrammar("bnf", mLocalGrammar, grammarListener);
		if (ret != ErrorCode.SUCCESS) {
			showTip("语法构建失败,错误码：" + ret + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
		}

	}
	private void initdata(){
		// 非空判断，防止因空指针使程序崩溃
		mIvw = VoiceWakeuper.getWakeuper();
		if (mIvw != null) {
			resultString = "";
			recoString = "";
			textView.setText(resultString);

			final String resPath = ResourceUtil.generateResourcePath(this, RESOURCE_TYPE.assets, "ivw/"+getString(R.string.app_id)+".jet");
			// 清空参数
			mIvw.setParameter(SpeechConstant.PARAMS, null);
			// 设置识别引擎
			//mIvw.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
			// 设置唤醒资源路径
			mIvw.setParameter(ResourceUtil.IVW_RES_PATH, resPath);
			/**
			 * 唤醒门限值，根据资源携带的唤醒词个数按照“id:门限;id:门限”的格式传入
			 * 示例demo默认设置第一个唤醒词，建议开发者根据定制资源中唤醒词个数进行设置
			 */
			mIvw.setParameter(SpeechConstant.IVW_THRESHOLD, "0:"
					+ curThresh);
			// 设置唤醒+识别模式
			//mIvw.setParameter(SpeechConstant.IVW_SST, "oneshot");
			mIvw.setParameter(SpeechConstant.IVW_SST, "wakeup");
			// 设置返回结果格式
			//mIvw.setParameter(SpeechConstant.RESULT_TYPE, "json");
			// 设置持续进行唤醒
			mIvw.setParameter(SpeechConstant.KEEP_ALIVE,"1");

//
//			mIvw.setParameter(SpeechConstant.IVW_SHOT_WORD, "0");

			// 设置唤醒录音保存路径，保存最近一分钟的音频
			mIvw.setParameter( SpeechConstant.IVW_AUDIO_PATH, Environment.getExternalStorageDirectory().getPath()+"/msc/ivw.wav" );
			mIvw.setParameter( SpeechConstant.AUDIO_FORMAT, "wav" );
			mIvw.startListening(mWakeuperListener);
		} else {
			showTip("唤醒未初始化");
		}

	}

	/**
	 * 初始化监听器。
	 */
	private InitListener mInitListener = new InitListener() {

		@Override
		public void onInit(int code) {
			Log.d(TAG, "SpeechRecognizer init() code = " + code);
			if (code != ErrorCode.SUCCESS) {
				showTip("初始化失败,错误码："+code+",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
			}
		}
	};

	/**
	 * 识别监听器。
	 */
	private RecognizerListener mRecognizerListener = new RecognizerListener() {

		@Override
		public void onVolumeChanged(int volume, byte[] data) {
			showTip("当前正在说话，音量大小：" + volume);
			Log.d(TAG, "返回音频数据："+data.length);
		}

		@Override
		public void onResult(final RecognizerResult result, boolean isLast) {
			if (null != result && !TextUtils.isEmpty(result.getResultString())) {
				Log.d(TAG, "recognizer result：" + result.getResultString());
				//recoString = JsonParser.parseGrammarResult(result.getResultString());
				int recoint = JsonParser.parseGrammarResultcontact(result.getResultString());
				Log.d(TAG, " "+recoint);
				if(recoint>30)
					textView.setText("答对了");
				else {
					textView.setText("您好，没听到你说的答案，不好意思");
					Log.e("TAG", "recognizer result : null");
				}
			}
		}

		@Override
		public void onEndOfSpeech() {
			// 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
			showTip("结束说话");
			//mediaPlayer.start(); // 开始播放
		}

		@Override
		public void onBeginOfSpeech() {
			// 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
			showTip("开始说话");
		}

		@Override
		public void onError(SpeechError error) {
			textView.setText("答错了");
			showTip("onError Code："	+ error.getErrorCode());
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
			// 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
			// 若使用本地能力，会话id为null

		}

	};
	/**
	 * 识别监听器。
	 */
	private RecognizerListener quesRecognizerListener = new RecognizerListener() {

		@Override
		public void onVolumeChanged(int volume, byte[] data) {
			showTip("当前正在说话，音量大小：" + volume);
			Log.d(TAG, "返回音频数据："+data.length);
		}

		@Override
		public void onResult(final RecognizerResult result, boolean isLast) {
			if (null != result && !TextUtils.isEmpty(result.getResultString())) {
				Log.d(TAG, "recognizer result：" + result.getResultString());
				//recoString = JsonParser.parseGrammarResult(result.getResultString());
				int contact = JsonParser.parseGrammarResultcontact(result.getResultString());
				int callCmd = JsonParser.parseGrammarResultcallCmd(result.getResultString());
				Log.d(TAG, " "+contact);
				Log.d(TAG, " "+callCmd);
				if(contact>30)
					//减少音量
					mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER,AudioManager.FX_FOCUS_NAVIGATION_UP);
				if(callCmd>30)
					//增加电量
					mAudioManager.adjustStreamVolume (AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE,AudioManager.FX_FOCUS_NAVIGATION_UP);
			}
		}

		@Override
		public void onEndOfSpeech() {
			// 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
			showTip("结束说话");
			//mediaPlayer.start(); // 开始播放
		}

		@Override
		public void onBeginOfSpeech() {
			// 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
			showTip("开始说话");
		}

		@Override
		public void onError(SpeechError error) {
			textView.setText("不好意思,没有听懂你的意思");
			showTip("onError Code："	+ error.getErrorCode());
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
			// 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
			// 若使用本地能力，会话id为null

		}

	};
	public void setgram(){
		String dynasty="dynasty"+posnumber;
		mLocalGrammar = readFile(OneShotDemo.this, dynasty+".bnf", "utf-8");
		mAsr.setParameter(SpeechConstant.LOCAL_GRAMMAR, dynasty);
		mAsr.buildGrammar("bnf", mLocalGrammar, grammarListener);

		mAsr.startListening(mRecognizerListener);
	}
	//private int i=0;
	private WakeuperListener mWakeuperListener = new WakeuperListener() {

		@Override
		public void onResult(WakeuperResult result) {
			mediaPlayer.pause(); // 暂停播放
			try {
				String text = result.getResultString();
				JSONObject object;
				object = new JSONObject(text);
				StringBuffer buffer = new StringBuffer();
				buffer.append("【RAW】 "+text);
				buffer.append("\n");
				buffer.append("【操作类型】"+ object.optString("sst"));
				buffer.append("\n");
				buffer.append("【唤醒词id】"+ object.optString("id"));
				buffer.append("\n");
				buffer.append("【得分】" + object.optString("score"));
				buffer.append("\n");
				buffer.append("【前端点】" + object.optString("bos"));
				buffer.append("\n");
				buffer.append("【尾端点】" + object.optString("eos"));
				resultString =buffer.toString();
			} catch (JSONException e) {
				resultString = "结果解析出错";
				e.printStackTrace();
			}
			//为了解决多次唤醒后，一次识别出现多次同一唤醒词
			if(mAsr!=null)
			{
				mAsr.stopListening();
			}
			textView.setText(resultString);
			//Log.e("111","mRecognizerListener");
/*
			i++;
			if(i>1&&i<=10)
			{
				String dynasty="dynasty"+i;
				mLocalGrammar = readFile(OneShotDemo.this, dynasty+".bnf", "utf-8");
				mAsr.setParameter(SpeechConstant.LOCAL_GRAMMAR, dynasty);
			}
			else {
				mLocalGrammar = readFile(OneShotDemo.this, "dynasty1.bnf", "utf-8");
				mAsr.setParameter(SpeechConstant.LOCAL_GRAMMAR, "dynasty1");
			}

 */
			mLocalGrammar = readFile(OneShotDemo.this, "voiceadjust.bnf", "utf-8");
			mAsr.setParameter(SpeechConstant.LOCAL_GRAMMAR, "voiceadjust");
			mAsr.buildGrammar("bnf", mLocalGrammar, grammarListener);

			mAsr.startListening(quesRecognizerListener);


		}

		@Override
		public void onError(SpeechError error) {
			showTip(error.getPlainDescription(true));
		}


		@Override
		public void onBeginOfSpeech() {
			showTip("开始说话");
		}

		@Override
		public void onEvent(int eventType, int isLast, int arg2, Bundle obj) {
			Log.d(TAG, "eventType:"+eventType+ "arg1:"+isLast + "arg2:" + arg2);
			// 识别结果
			//if (SpeechEvent.EVENT_IVW_RESULT == eventType) {
			//	RecognizerResult reslut = ((RecognizerResult)obj.get(SpeechEvent.KEY_EVENT_IVW_RESULT));
			//	recoString += JsonParser.parseGrammarResult(reslut.getResultString());
			//	textView.setText(recoString);
			//}
		}

		@Override
		public void onVolumeChanged(int volume) {
			// TODO Auto-generated method stub
		}

	};

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mediaPlayer != null) {
			mediaPlayer.stop();
			mediaPlayer.release();
		}
		Log.d(TAG, "onDestroy OneShotDemo");
		mIvw = VoiceWakeuper.getWakeuper();

		if (mIvw != null) {
			mIvw.destroy();
		} else {
			showTip("唤醒未初始化");
		}
		if( null != mAsr ){
			// 退出时释放连接
			mAsr.cancel();
			mAsr.destroy();
		}
	}
	
	/**
	 * 读取asset目录下文件。
	 * 
	 * @return content
	 */
	public static String readFile(Context mContext, String file, String code) {
		int len = 0;
		byte[] buf = null;
		String result = "";
		try {
			InputStream in = mContext.getAssets().open(file);
			len = in.available();
			buf = new byte[len];
			in.read(buf, 0, len);

			result = new String(buf, code);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	// 获取识别资源路径
	private String getResourcePath() {
		StringBuffer tempBuffer = new StringBuffer();
		// 识别通用资源
		tempBuffer.append(ResourceUtil.generateResourcePath(this, 
				RESOURCE_TYPE.assets, "asr/common.jet"));
		return tempBuffer.toString();
	}
	
	private void showTip(final String str) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mToast.setText(str);
				mToast.show();
			}
		});
	}


	private void requestPermissions(){
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				int permission = ActivityCompat.checkSelfPermission(this,
						Manifest.permission.WRITE_EXTERNAL_STORAGE);
				if(permission!= PackageManager.PERMISSION_GRANTED) {
					ActivityCompat.requestPermissions(this,new String[] {
							Manifest.permission.WRITE_EXTERNAL_STORAGE,
							Manifest.permission.LOCATION_HARDWARE,Manifest.permission.READ_PHONE_STATE,
							Manifest.permission.WRITE_SETTINGS,Manifest.permission.READ_EXTERNAL_STORAGE,
							Manifest.permission.RECORD_AUDIO,Manifest.permission.READ_CONTACTS},0x0010);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}
}
