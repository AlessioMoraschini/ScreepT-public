package various.common.light.exception;

import java.util.UUID;

public class GeneralException extends Exception {
	
	private static final long serialVersionUID = 993324172550124405L;

	private String message;
	private String refId;
	private String code;

	public GeneralException(Throwable e) {
		super(e.getMessage());
		message = e.getMessage();
		this.initCause(e);

		this.refId = getUniqueId();
		this.code = "";
	}

	public GeneralException(Throwable e, String faultString) {
		super(faultString);
		message = faultString;
		this.initCause(e);

		this.refId = getUniqueId();
		this.code = "";
	}

	public GeneralException(Throwable e, String faultString, String code) {
		this(e, faultString);
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getRefId() {
		return refId;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	private String getUniqueId() {
		UUID uniqueId = UUID.randomUUID();
		return uniqueId.toString();
	}
}