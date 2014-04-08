package ch.shamu.jsendnrdp.domain;

public enum State {

	OK(0), WARNING(1), CRITICAL(2), UNKNOWN(3);

	private final int code; // the nagios specific code for this state

	State(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}
}
