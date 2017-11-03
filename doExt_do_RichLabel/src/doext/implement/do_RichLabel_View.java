package doext.implement;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.Browser;
import android.text.Html;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils.TruncateAt;
import android.text.method.LinkMovementMethod;
import android.text.method.Touch;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;
import core.DoServiceContainer;
import core.helper.DoIOHelper;
import core.helper.DoImageLoadHelper;
import core.helper.DoImageLoadHelper.OnPostExecuteListener;
import core.helper.DoTextHelper;
import core.helper.DoUIModuleHelper;
import core.interfaces.DoIScriptEngine;
import core.interfaces.DoIUIModuleView;
import core.object.DoInvokeResult;
import core.object.DoUIModule;
import doext.define.do_RichLabel_IMethod;
import doext.define.do_RichLabel_MAbstract;

/**
 * 自定义扩展UIView组件实现类，此类必须继承相应VIEW类，并实现DoIUIModuleView,do1_RichLabel_IMethod接口；
 * #如何调用组件自定义事件？可以通过如下方法触发事件：
 * this.model.getEventCenter().fireEvent(_messageName, jsonResult);
 * 参数解释：@_messageName字符串事件名称，@jsonResult传递事件参数对象； 获取DoInvokeResult对象方式new
 * DoInvokeResult(this.model.getUniqueKey());
 */
@SuppressLint("ClickableViewAccessibility")
public class do_RichLabel_View extends TextView implements DoIUIModuleView, do_RichLabel_IMethod, Html.ImageGetter {

	/**
	 * 每个UIview都会引用一个具体的model实例；
	 */
	private do_RichLabel_MAbstract model;
	private boolean linkHit;
	private LinkedList<String> imageUrl; //获取所有http开头的img标签的src

	private Map<String, SoftReference<Bitmap>> mBitmaps = null;

	public do_RichLabel_View(Context context) {
		super(context);
		mBitmaps = new HashMap<String, SoftReference<Bitmap>>();
		imageUrl = new LinkedList<String>();
		this.setAutoLinkMask(Linkify.WEB_URLS);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		linkHit = false;
		boolean res = super.onTouchEvent(event);
		if (!linkHit) {
			return false;
		}
		return res;

	}

	@Override
	public boolean hasFocusable() {
		return false;
	}

	/**
	 * 初始化加载view准备,_doUIModule是对应当前UIView的model实例
	 */
	@Override
	public void loadView(DoUIModule _doUIModule) throws Exception {
		this.model = (do_RichLabel_MAbstract) _doUIModule;
		this.setMaxLines(1);
		this.setEllipsize(TruncateAt.END);
	}

	/**
	 * 动态修改属性值时会被调用，方法返回值为true表示赋值有效，并执行onPropertiesChanged，否则不进行赋值；
	 * 
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public boolean onPropertiesChanging(Map<String, String> _changedValues) {
		return true;
	}

	/**
	 * 属性赋值成功后被调用，可以根据组件定义相关属性值修改UIView可视化操作；
	 * 
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public void onPropertiesChanged(Map<String, String> _changedValues) {
		DoUIModuleHelper.handleBasicViewProperChanged(this.model, _changedValues);
		if (_changedValues.containsKey("text")) {
			String html = _changedValues.get("text");
			try {
				CharSequence text = fromHtml(html);
				this.setText(text);
			} catch (Exception e) {
			}
			this.setMovementMethod(do_RichLabel_View.LocalLinkMovementMethod.getInstance());
		}
		if (_changedValues.containsKey("maxWidth")) {
			int _maxWidth = (int) (DoTextHelper.strToDouble(_changedValues.get("maxWidth"), 100) * this.model.getXZoom());
			this.setMaxWidth(_maxWidth);
		}

		if (_changedValues.containsKey("maxHeight")) {
			int _maxHeight = (int) (DoTextHelper.strToDouble(_changedValues.get("maxHeight"), 100) * this.model.getYZoom());
			this.setMaxHeight(_maxHeight);
		}

		if (_changedValues.containsKey("maxLines")) {
			int _maxLines = DoTextHelper.strToInt(_changedValues.get("maxLines"), 1);
			if (_maxLines <= 0) {
				this.setMaxLines(Integer.MAX_VALUE);
			} else {
				this.setMaxLines(_maxLines);
			}
		}
	}

	/**
	 * 同步方法，JS脚本调用该组件对象方法时会被调用，可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V），获取参数值使用API提供DoJsonHelper类；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public boolean invokeSyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		//...do something
		return false;
	}

	/**
	 * 异步方法（通常都处理些耗时操作，避免UI线程阻塞），JS脚本调用该组件对象方法时会被调用， 可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V），获取参数值使用API提供DoJsonHelper类；
	 * @_scriptEngine 当前page JS上下文环境
	 * @_callbackFuncName 回调函数名 #如何执行异步方法回调？可以通过如下方法：
	 *                    _scriptEngine.callback(_callbackFuncName,
	 *                    _invokeResult);
	 *                    参数解释：@_callbackFuncName回调函数名，@_invokeResult传递回调函数参数对象；
	 *                    获取DoInvokeResult对象方式new
	 *                    DoInvokeResult(this.model.getUniqueKey());
	 */
	@Override
	public boolean invokeAsyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) {
		//...do something
		return false;
	}

	/**
	 * 释放资源处理，前端JS脚本调用closePage或执行removeui时会被调用；
	 */
	@Override
	public void onDispose() {
		imageUrl.clear();
		if (null != mBitmaps) {
			Iterator<String> _it = mBitmaps.keySet().iterator();
			while (_it.hasNext()) {
				String _key = _it.next();
				SoftReference<Bitmap> _mBitmapSoft = mBitmaps.get(_key);
				if (_mBitmapSoft != null) {
					Bitmap _mBitmap = _mBitmapSoft.get();
					if (null != _mBitmap && !_mBitmap.isRecycled()) {
						_mBitmap.recycle();
						_mBitmap = null;
					}
					_mBitmapSoft.clear();
				}
			}
			mBitmaps.clear();
		}
	}

	/**
	 * 重绘组件，构造组件时由系统框架自动调用；
	 * 或者由前端JS脚本调用组件onRedraw方法时被调用（注：通常是需要动态改变组件（X、Y、Width、Height）属性时手动调用）
	 */
	@Override
	public void onRedraw() {
		this.setLayoutParams(DoUIModuleHelper.getLayoutParams(this.model));
	}

	/**
	 * 获取当前model实例
	 */
	@Override
	public DoUIModule getModel() {
		return model;
	}

	private void setLinkClickable(final SpannableStringBuilder clickableHtmlBuilder, final URLSpan urlSpan, final Element a) {
		int start = clickableHtmlBuilder.getSpanStart(urlSpan);
		int end = clickableHtmlBuilder.getSpanEnd(urlSpan);
		//int flags = clickableHtmlBuilder.getSpanFlags(urlSpan);
		ClickableSpan clickableSpan = new ClickableSpan() {
			public void onClick(View widget) {
				if (urlSpan.getURL().startsWith("#")) {
					DoInvokeResult jsonResult = new DoInvokeResult(model.getUniqueKey());
					String text = a.text();
					Attributes attributes = a.attributes();
					JSONObject node = new JSONObject();
					try {
						node.put("text", text);
						for (Attribute att : attributes) {
							node.put(att.getKey(), att.getValue());
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
					jsonResult.setResultNode(node);
					model.getEventCenter().fireEvent("linkTouch", jsonResult);
				} else {
					Uri uri = Uri.parse(urlSpan.getURL());
					Context context = widget.getContext();
					Intent intent = new Intent(Intent.ACTION_VIEW, uri);
					intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
					try {
						context.startActivity(intent);
					} catch (ActivityNotFoundException e) {
						DoServiceContainer.getLogEngine().writeInfo("RichLabel", "无效的URL地址：href=" + urlSpan.getURL());
					}
				}
			}
		};
		clickableHtmlBuilder.setSpan(clickableSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	}

	private CharSequence fromHtml(String html) throws Exception {
		Document document = Jsoup.parse(html);
		Elements aElements = document.select("a");
		Elements imgElements = document.select("img");

		for (int i = 0; i < imgElements.size(); i++) {
			Element element = imgElements.get(i);
			if (element.hasAttr("src")) {
				String imageSrc = element.attr("src");
				if (null != DoIOHelper.getHttpUrlPath(imageSrc)) {
					imageUrl.add(imageSrc);
				}
			}
		}

		Spanned spannedHtml = Html.fromHtml(html, this, null);
		SpannableStringBuilder clickableHtmlBuilder = new SpannableStringBuilder(spannedHtml);
		URLSpan[] urls = clickableHtmlBuilder.getSpans(0, spannedHtml.length(), URLSpan.class);
		for (int i = 0; i < urls.length; i++) {
			URLSpan span = urls[i];
			Element a = aElements.get(i);
			setLinkClickable(clickableHtmlBuilder, span, a);
		}
		return clickableHtmlBuilder;
	}

	public static class LocalLinkMovementMethod extends LinkMovementMethod {
		static LocalLinkMovementMethod sInstance;

		public static LocalLinkMovementMethod getInstance() {
			if (sInstance == null)
				sInstance = new LocalLinkMovementMethod();

			return sInstance;
		}

		@Override
		public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
			int action = event.getAction();

			if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
				int x = (int) event.getX();
				int y = (int) event.getY();

				x -= widget.getTotalPaddingLeft();
				y -= widget.getTotalPaddingTop();

				x += widget.getScrollX();
				y += widget.getScrollY();

				Layout layout = widget.getLayout();
				int line = layout.getLineForVertical(y);
				int off = layout.getOffsetForHorizontal(line, x);

				ClickableSpan[] link = buffer.getSpans(off, off, ClickableSpan.class);

				if (link.length != 0) {
					if (action == MotionEvent.ACTION_UP) {
						link[0].onClick(widget);
					} else if (action == MotionEvent.ACTION_DOWN) {
						Selection.setSelection(buffer, buffer.getSpanStart(link[0]), buffer.getSpanEnd(link[0]));
					}

					if (widget instanceof do_RichLabel_View) {
						((do_RichLabel_View) widget).linkHit = true;
					}
					return true;
				} else {
					Selection.removeSelection(buffer);
					Touch.onTouchEvent(widget, buffer, event);
					return false;
				}
			}
			return Touch.onTouchEvent(widget, buffer, event);
		}
	}

	@Override
	public Drawable getDrawable(final String _source) {
		try {
			SoftReference<Bitmap> _mBitmapSoft = mBitmaps.get(_source);
			if (null != _mBitmapSoft) {
				Bitmap _mBitmap = _mBitmapSoft.get();
				return calculateBitmap(_mBitmap);
			}

			if (null != DoIOHelper.getHttpUrlPath(_source)) {
				DoImageLoadHelper.getInstance().loadURL(_source, "never", -1, -1, new OnPostExecuteListener() {
					@Override
					public void onResultExecute(Bitmap bitmap, String url) {
						//url.equals(source)判断source等于最后请求结果URL并显示，忽略掉中间线程结果；
						if ((bitmap != null && url.equals(_source))) {
							if (bitmap != null) {
								mBitmaps.put(_source, new SoftReference<Bitmap>(bitmap));
								imageUrl.remove(_source);
								try {
									if (imageUrl.isEmpty()) {
										String _html = model.getPropertyValue("text");
										setText(Html.fromHtml(_html, do_RichLabel_View.this, null));
									}
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}
					}
				});
			} else {
				if (_source != null && !"".equals(_source)) {
					String _path = DoIOHelper.getLocalFileFullPath(model.getCurrentPage().getCurrentApp(), _source);
					Bitmap _mBitmap = DoImageLoadHelper.getInstance().loadLocal(_path, -1, -1);
					return calculateBitmap(_mBitmap);
				}
			}
		} catch (Exception e) {
			DoServiceContainer.getLogEngine().writeError("do_RichLabel_View <img>", e);
		}
		return null;
	}

	@SuppressWarnings("deprecation")
	private BitmapDrawable calculateBitmap(Bitmap _sourceBmp) {
		if (_sourceBmp != null) {
			BitmapDrawable _mDrawable = new BitmapDrawable(_sourceBmp);

			double _v_w = model.getRealWidth();
			//进行缩放处理，防止图片变形
			double _b_w = _sourceBmp.getWidth();
			double _b_h = _sourceBmp.getHeight();
			if (LayoutParams.WRAP_CONTENT == _v_w) { //如果宽 =-1，直接返回原图大小
				_mDrawable.setBounds(0, 0, (int) _b_w, (int) _b_h);
				return _mDrawable;
			}

			double _height = _b_h;
			if (_b_w > _v_w) { //图片的宽度比view宽度大
				double _w_scale = _b_w / _v_w;
				_height = _b_h * _w_scale;
			} else {//图片的宽度比view宽度小
				double _w_scale = _v_w / _b_w;
				_height = _b_h * _w_scale;
			}
			_mDrawable.setBounds(0, 0, (int) _v_w, (int) _height);
			return _mDrawable;
		}
		return null;
	}

}