package com.example.flywakeming;

import java.io.InputStream;

import org.json.JSONException;
import org.json.JSONObject;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.GrammarListener;
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
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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

public class OneShotDemo extends Activity implements OnClickListener{
	private String TAG = "ivw";
	private Toast mToast;
	private TextView textView;
	// 语音唤醒对象
	private VoiceWakeuper mIvw;
	// 语音识别对象
	private SpeechRecognizer mAsr;
	// 唤醒结果内容
	private String resultString;
	// 识别结果内容
	private String recoString;
	private int curThresh = 1450;
	// 本地语法id
	private String mLocalGrammarID="1";

	// 本地语法文件
	private String mLocalGrammar = null;
	// 本地语法构建路径	
	private String grmPath = Environment.getExternalStorageDirectory().getAbsolutePath()
			+ "/msc/test";
	// 引擎类型
	private String mEngineType = SpeechConstant.TYPE_LOCAL;
	//构建语法标志位
	private boolean gramflag=true;
	private boolean dataflag=false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.oneshot_activity);
		requestPermissions();
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
		mAsr = SpeechRecognizer.createRecognizer(this, null);
		// 初始化语法文件
		mLocalGrammar = readFile(this, "call.bnf", "utf-8");

		initgrammar();
		if(gramflag){
			initdata();
		}
		//&&mIvw == null
		if(dataflag){
			mIvw.stopListening();
			Log.e("111","mRecognizerListener");
			mAsr.startListening(mRecognizerListener);
			dataflag=false;
		}
	}
	
	private void initUI() {
		findViewById(R.id.btn_oneshot).setOnClickListener(OneShotDemo.this);
		findViewById(R.id.btn_stop).setOnClickListener(OneShotDemo.this);
		findViewById(R.id.btn_grammar).setOnClickListener(OneShotDemo.this);
		mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
		textView = (TextView) findViewById(R.id.txt_show_msg);
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
		// 设置资源路径

		mAsr.setParameter(ResourceUtil.ASR_RES_PATH, getResourcePath());
		ret = mAsr.buildGrammar("bnf", mLocalGrammar, grammarListener);


		//ret = mAsr.startListening(mRecognizerListener);



		//Log.e("gram",mLocalGrammarID);
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
	//		mIvw.setParameter(SpeechConstant.KEEP_ALIVE,"1");

//
//				mIvw.setParameter(SpeechConstant.IVW_SHOT_WORD, "0");

			// 设置唤醒录音保存路径，保存最近一分钟的音频
			mIvw.setParameter( SpeechConstant.IVW_AUDIO_PATH, Environment.getExternalStorageDirectory().getPath()+"/msc/ivw.wav" );
			mIvw.setParameter( SpeechConstant.AUDIO_FORMAT, "wav" );
			mIvw.startListening(mWakeuperListener);
			/*
			if (!TextUtils.isEmpty(mLocalGrammarID)) {
				// 设置本地识别资源
				mIvw.setParameter(ResourceUtil.ASR_RES_PATH,
						getResourcePath());
				// 设置语法构建路径
				mIvw.setParameter(ResourceUtil.GRM_BUILD_PATH, grmPath);
				// 设置本地识别使用语法id
			   Log.e("gram",mLocalGrammarID);
				//mIvw.setParameter(SpeechConstant.LOCAL_GRAMMAR,
				//		mLocalGrammarID);
				mIvw.setParameter(SpeechConstant.LOCAL_GRAMMAR,
						"call");
				mIvw.startListening(mWakeuperListener);
			} else {
				showTip("请先构建语法");
				Log.e("gram","请先构建语法");
			}*/
		} else {
			showTip("唤醒未初始化");
		}

	}
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			/*
		case R.id.btn_oneshot:
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
				mIvw.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
				// 设置唤醒资源路径
				mIvw.setParameter(ResourceUtil.IVW_RES_PATH, resPath);
				/**
				 * 唤醒门限值，根据资源携带的唤醒词个数按照“id:门限;id:门限”的格式传入
				 * 示例demo默认设置第一个唤醒词，建议开发者根据定制资源中唤醒词个数进行设置
				 *//*
				mIvw.setParameter(SpeechConstant.IVW_THRESHOLD, "0:"
						+ curThresh);
				// 设置唤醒+识别模式
				mIvw.setParameter(SpeechConstant.IVW_SST, "oneshot");
				// 设置返回结果格式
				mIvw.setParameter(SpeechConstant.RESULT_TYPE, "json");
//				
//				mIvw.setParameter(SpeechConstant.IVW_SHOT_WORD, "0");
				
				// 设置唤醒录音保存路径，保存最近一分钟的音频
				mIvw.setParameter( SpeechConstant.IVW_AUDIO_PATH, Environment.getExternalStorageDirectory().getPath()+"/msc/ivw.wav" );
				mIvw.setParameter( SpeechConstant.AUDIO_FORMAT, "wav" );

					if (!TextUtils.isEmpty(mLocalGrammarID)) {
						// 设置本地识别资源
						mIvw.setParameter(ResourceUtil.ASR_RES_PATH,
								getResourcePath());
						// 设置语法构建路径
						mIvw.setParameter(ResourceUtil.GRM_BUILD_PATH, grmPath);
						// 设置本地识别使用语法id
						mIvw.setParameter(SpeechConstant.LOCAL_GRAMMAR,
								mLocalGrammarID);
						mIvw.startListening(mWakeuperListener);
					} else {
						showTip("请先构建语法");
					}
			} else {
				showTip("唤醒未初始化");
			}
			break;*/
		/*
		case R.id.btn_grammar:
			int ret = 0;
				mAsr.setParameter(SpeechConstant.PARAMS, null);
				mAsr.setParameter(SpeechConstant.TEXT_ENCODING, "utf-8");
				// 设置引擎类型
				mAsr.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
				// 设置语法构建路径
				mAsr.setParameter(ResourceUtil.GRM_BUILD_PATH, grmPath);
				// 设置资源路径
				mAsr.setParameter(ResourceUtil.ASR_RES_PATH, getResourcePath());
				ret = mAsr.buildGrammar("bnf", mLocalGrammar, grammarListener);
				if (ret != ErrorCode.SUCCESS) {
					showTip("语法构建失败,错误码：" + ret+",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
				}
			break;
		*/
		case R.id.btn_stop:
			mIvw = VoiceWakeuper.getWakeuper();
			if (mIvw != null) {
				mIvw.stopListening();
			} else {
				showTip("唤醒未初始化");
			}
			break;

		default:
			break;
		}
	}
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
			} else {
				Log.d(TAG, "recognizer result : null");
			}
		}

		@Override
		public void onEndOfSpeech() {
			// 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
			showTip("结束说话");
		}

		@Override
		public void onBeginOfSpeech() {
			// 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
			showTip("开始说话");
		}

		@Override
		public void onError(SpeechError error) {
			showTip("onError Code："	+ error.getErrorCode());
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
			// 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
			// 若使用本地能力，会话id为null
			//	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
			//		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
			//		Log.d(TAG, "session id =" + sid);
			//	}
		}

	};


	private WakeuperListener mWakeuperListener = new WakeuperListener() {

		@Override
		public void onResult(WakeuperResult result) {
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
			textView.setText(resultString);

			dataflag=true;

			Log.e("dataflag","dataflag");
			gramflag=false;

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
			if (SpeechEvent.EVENT_IVW_RESULT == eventType) {
				RecognizerResult reslut = ((RecognizerResult)obj.get(SpeechEvent.KEY_EVENT_IVW_RESULT));
				recoString += JsonParser.parseGrammarResult(reslut.getResultString());
				textView.setText(recoString);
			}
		}

		@Override
		public void onVolumeChanged(int volume) {
			// TODO Auto-generated method stub
			
		}

	};
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
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
