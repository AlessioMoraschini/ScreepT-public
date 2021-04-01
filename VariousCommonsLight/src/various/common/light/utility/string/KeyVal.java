package various.common.light.utility.string;

public class KeyVal {

	public String key,value;

	public KeyVal() {
		key = "";
		value = "";
	}

	public KeyVal(String key, String value) {
		super();
		this.key = key;
		this.value = value;
	}

	@Override
	public String toString() {
		return "KeyVal [key=" + key + ", value=" + value + "]";
	}

}
