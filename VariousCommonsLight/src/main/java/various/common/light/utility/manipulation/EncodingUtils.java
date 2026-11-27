/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini)
 * =========================================================================================
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package various.common.light.utility.manipulation;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

public class EncodingUtils {

	public Decoder decoderBase64;
	public Encoder encoderBase64;
	
	public static String stringEncoding = "UTF-8";
	
	public static EncodingUtils staticInstance = new EncodingUtils();
	
	public EncodingUtils() {
		decoderBase64 = Base64.getDecoder();
		encoderBase64 = Base64.getEncoder();
	}
	
	public String encodeBase64(byte[] source) throws UnsupportedEncodingException {
		return new String(encoderBase64.encode(source), stringEncoding);
	}
	
	public String decodeBase64(byte[] source) throws UnsupportedEncodingException {
		String encodedSrc = new String(source, stringEncoding);
		return new String(decodeBase64bytes(encodedSrc), stringEncoding);
	}

	public byte[] decodeBase64bytes(byte[] source) throws UnsupportedEncodingException {
		String encodedSrc = new String(source, stringEncoding);
		return decodeBase64bytes(encodedSrc);
	}

	public String decodeBase64(String source) throws UnsupportedEncodingException {
		return new String(decodeBase64bytes(source), stringEncoding);
	}

	public byte[] decodeBase64bytes(String source) throws UnsupportedEncodingException{
		byte[] sourceBytes = source.getBytes(stringEncoding);
		return decoderBase64.decode(sourceBytes);
	}
}
