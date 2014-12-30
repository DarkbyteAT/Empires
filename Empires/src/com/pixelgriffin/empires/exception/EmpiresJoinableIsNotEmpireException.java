package com.pixelgriffin.empires.exception;

public class EmpiresJoinableIsNotEmpireException extends Exception {
	
	private static final long serialVersionUID = 4158414257077707307L;

	public EmpiresJoinableIsNotEmpireException(String _msg) {
		super(_msg);
	}
	
	public EmpiresJoinableIsNotEmpireException(String _msg, Throwable _throwable) {
		super(_msg, _throwable);
	}
}
