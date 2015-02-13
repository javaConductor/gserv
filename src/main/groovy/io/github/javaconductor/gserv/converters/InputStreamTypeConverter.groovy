/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Lee Collins
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.github.javaconductor.gserv.converters

import groovy.json.JsonSlurper
import io.github.javaconductor.gserv.exceptions.ConversionException
import org.apache.commons.io.IOUtils

/**
 * Created with IntelliJ IDEA.
 * User: lcollins
 * Date: 1/5/14
 * Time: 4:10 AM
 * To change this template use File | Settings | File Templates.
 */
class InputStreamTypeConverter {
    def converters
    def typeConverters

    def InputStreamTypeConverter() {
        to = converters = ["text": readText, "json": readJson, "xml": readXml, "type": typeConverter]
        typeConverters = [:]
    }

    /**
     * @param c Target class of conversion
     * @param inputStream
     * @return stream data as class 'c'
     */
    def typeConverter = { Class c, InputStream inputStream ->
        def convertFn = typeConverters[c.name]
        if (!convertFn) {
            throw new ConversionException("No converter for class: ${c.name}.")
        }
        convertFn(inputStream)
    }

    /**
     *  Add a converter function 'fn' to be referenced by 'name'
     *
     * @param name
     * @param fn
     * @return
     */
    def add(String name, fn) {
        converters[name] = fn
    }

    /**
     *  Add a class converter function 'fn' to be referenced by 'name'
     *
     * @param aClass
     * @param fn
     * @return
     */
    def add(Class aClass, Closure fn) {
        typeConverters[aClass.name] = fn
    }

    /**
     *  Builtin converter for JSON
     *
     * @param istream The inputStream
     *
     * @return converted text (Map)
     */
    def readJson = { istream ->
        def js = new JsonSlurper()
        def ret = js.parse(new InputStreamReader(istream))
        return ret;
    }

    /**
     *  Builtin converter for XML
     *
     * @param istream The inputStream
     *
     * @return converted text (GPathResult)
     */
    def readXml = { istream ->
        def xs = new XmlSlurper()
        xs.parse(istream)
    }

    /**
     *  Builtin converter for String as byte stream
     *
     * @param istream The inputStream
     *
     * @return converted text
     */
    def readText = { istream ->
        IOUtils.toString(istream);
    }

    /**
     *  Invokes converter 'name' on input stream 'istream'
     */
//    def to = { name, istream -> converters[name](istream) }
    def to

}
