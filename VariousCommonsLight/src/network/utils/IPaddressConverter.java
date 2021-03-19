/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini) - All Rights Reserved
 * =========================================================================================
 *
 * You should have received a copy of the license with this file.
 * If not, please write to: info@am-design-development.com, or visit : https://www.am-design-development.com
 */
package network.utils;

import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TCP/IP utils
 *
 * @author Alessio Moraschini 2018
 */
public class IPaddressConverter {
	
	private static final String IPADDRESS_PATTERN = 
	        "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";

	private static final Pattern pattern = Pattern.compile(IPADDRESS_PATTERN);
	
	public static String extractIpFromString(String raw) {
		
		final Matcher matcher = pattern.matcher(raw);
		
		if (matcher.find()) {
		    return matcher.group();
		} else{
		    return "";
		}
	}


    /**
     * Convert a TCP/IP address string into a byte array
     * 
     * @param addr String
     * @return byte[]
     */
    public final static byte[] asBytes(String addr) {
      
    	// first remove white spaces and endline characters
    	addr = addr.trim();
    	
    	// Convert the TCP/IP address string to an integer value
    	int ipInt = parseNumericAddress(addr);
    	if ( ipInt == 0)
    		return null;
      
    	// Convert to bytes
      
    	byte[] ipByts = new byte[4];
      
    	ipByts[3] = (byte) (ipInt & 0xFF);
    	ipByts[2] = (byte) ((ipInt >> 8) & 0xFF);
    	ipByts[1] = (byte) ((ipInt >> 16) & 0xFF);
    	ipByts[0] = (byte) ((ipInt >> 24) & 0xFF);
      
    	// Return the TCP/IP bytes
    	return ipByts;
    }
	  
    /**   
	* Check if the specified address is a valid numeric TCP/IP address and return as an integer value
	* 
	* @param ipaddr String
	* @return int
	*/
	private final static int parseNumericAddress(String ipaddr) {
	 
		//  Check if the string is valid
    	if ( ipaddr == null || ipaddr.length() < 7 || ipaddr.length() > 15)
    		return 0;
      
    	//  Check the address string, should be n.n.n.n format
    	StringTokenizer token = new StringTokenizer(ipaddr,".");
    	if ( token.countTokens() != 4)
    		return 0;

    	int ipInt = 0;
    
    	while ( token.hasMoreTokens()) {
      
	    	//  Get the current token and convert to an integer value
	    	String ipNum = token.nextToken();
	      
	      	try {
	        
	    	  	//  Validate the current address part
		        int ipVal = Integer.valueOf(ipNum).intValue();
		        if ( ipVal < 0 || ipVal > 255) 
		        	return 0;
	          
	        	//  Add to the integer address
	        	ipInt = (ipInt << 8) + ipVal;
	      	}
	      	catch (NumberFormatException ex) {
	    	  return 0;
      		}
    	}
    
    	//  Return the integer address
    	return ipInt;
	}
  
	public static String getIpAddress(byte[] rawBytes) {
	    
		if (rawBytes!=null) {
			int i = 4;
			StringBuilder ipAddress = new StringBuilder();
			for (byte raw : rawBytes) {
				ipAddress.append(raw & 0xFF);
				if (--i > 0) {
					ipAddress.append(".");
				}
			}
			return ipAddress.toString();
		}else {
			return "";
		}
	}
	
	
	public static String getMacFromBytes(byte[] mac) {
	  
	    if (mac == null)
	        return "";

	    StringBuilder sb = new StringBuilder(18);
	    for (byte b : mac) {
	        if (sb.length() > 0)
	            sb.append(':');
	        sb.append(String.format("%02x", b));
	    }
	    return sb.toString().toUpperCase();
	}
}
