/*
 *  The MIT License (MIT)
 *
 *  Copyright (c) 2014-2016 Lee Collins
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package io.github.javaconductor.gserv.utils

import java.security.MessageDigest

/**
 * Created by javaConductor on 5/1/2014.
 */
class Encoder {

	/**
	 *
	 * Returns MD5 of stuff
	 * @param stuff
	 * @return byte[] MD5 of the data
	 */
	static byte[] md5(byte[] stuff) {
		MessageDigest digest = MessageDigest.getInstance("MD5")
		digest.update(stuff)
		digest.digest()
	}

	/**
	 * Does MD5 hash and returns base64 representation of hash.
	 *
	 * @param stuff
	 * @return Base64 of MD5 of the data
	 */
	static String md5WithBase64(byte[] stuff) {
		(md5(stuff).encodeBase64())
	}

}
