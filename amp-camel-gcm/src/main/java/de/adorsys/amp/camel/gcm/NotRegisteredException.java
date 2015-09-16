/**
 * 
 */
package de.adorsys.amp.camel.gcm;

/**
 * @author sso
 *
 */
public class NotRegisteredException extends Exception {
	private final String registrationId;

	public NotRegisteredException(String registrationId) {
		super();
		this.registrationId = registrationId;
	}
	@Override
	public String toString() {
		return super.toString() + " " + registrationId;
	}
}