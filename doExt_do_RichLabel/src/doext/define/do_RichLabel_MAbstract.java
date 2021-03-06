package doext.define;

import core.object.DoUIModule;
import core.object.DoProperty;
import core.object.DoProperty.PropertyDataType;


public abstract class do_RichLabel_MAbstract extends DoUIModule{

	protected do_RichLabel_MAbstract() throws Exception {
		super();
	}
	
	/**
	 * 初始化
	 */
	@Override
	public void onInit() throws Exception{
        super.onInit();
        //注册属性
		this.registProperty(new DoProperty("maxHeight", PropertyDataType.Number, "", false));
		this.registProperty(new DoProperty("maxLines", PropertyDataType.Number, "", false));
		this.registProperty(new DoProperty("maxWidth", PropertyDataType.Number, "", false));
		this.registProperty(new DoProperty("text", PropertyDataType.String, "", false));
	}
}