/**
 * @author hengxin
 * @date May 7, 2014
 * @description {@link AtomicityMessage} is used for atomic consistency implementation.
 */
package ics.mobilememo.sharedmemory.atomicity.message;

import consistencyinfrastructure.communication.IPMessage;
import consistencyinfrastructure.data.kvs.Key;
import ics.mobilememo.sharedmemory.data.kvs.VersionValue;

public class AtomicityMessage extends IPMessage
{
	private static final long serialVersionUID = 851435561377468450L;

	private static final String TAG = AtomicityMessage.class.getName();
	
	/**
	 * {@link Key} (representing the simulated register) on which 
	 * the operations PUT/GET/REMOVE are performed
	 * 
	 * its default value is {@link Key#RESERVED_KEY}
	 */
	protected Key key = Key.RESERVED_KEY;
	
	/**
	 * versioned value carried with the message
	 * its default value is {@link VersionValue#RESERVED_VERSIONVALUE}
	 */
	protected VersionValue vval = VersionValue.RESERVED_VERSIONVALUE;	
	
	/**
	 * constructor of {@link AtomicityMesssage}
	 * @param ip {@link IPMessage#sender_ip}
	 * @param key {@link Key} to put/get/remove
	 * @param vval {@link VersionValue} carried with the message
	 */
	public AtomicityMessage(String ip, int cnt, Key key, VersionValue vval)
	{
		super(ip, cnt);
		this.key = key;
		this.vval = vval;
	}

	/**
	 * @return {@link #key}
	 * @see Key
	 */
	public Key getKey()
	{
		return this.key;
	}
	
	/**
	 * @return {@link #vval}
	 * @see VersionValue
	 */
	public VersionValue getVersionValue()
	{
		return this.vval;
	}
	
	/**
	 * extract the {@link VersionValue}s carried with a set of {@link AtomicityMessage}s
	 * 
	 * @param atomicity_messages an array of {@link AtomicityMessage}s
	 * @return an array of {@link VersionValue} carried with the set of @param atomicity_messages
	 */
	public static VersionValue[] extractVersionValues(AtomicityMessage[] atomicity_messages)
	{
		int len = atomicity_messages.length;
		VersionValue[] vvals = new VersionValue[len];
		
		for (int i = 0; i < len; i++)
		{
//			Log.d(TAG, "The length of READ_PHASE_ACKS is: " + len + "; The current under check is : " + i);
//			Log.d(TAG, atomicity_messages[i].toString());
			vvals[i] = atomicity_messages[i].vval;
		}
		
		return vvals;
	}
	
	/**
	 * show {@link #key} and {@link #vval}
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append(super.toString()).append("\t").append(this.key.toString()).append("\t").append(this.vval.toString());
		
		return sb.toString();
	}
}
