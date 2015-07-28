package io.github.javaconductor.gserv.test

import io.github.javaconductor.gserv.converters.FormData
import io.github.javaconductor.gserv.plugins.cors.CORSConfig
import io.github.javaconductor.gserv.plugins.cors.CORSMode
import io.github.javaconductor.gserv.utils.ByteUtils
import io.github.javaconductor.gserv.utils.FormDataUtils
import spock.lang.Ignore
import spock.lang.Specification

/**
 * Created by lcollins on 7/27/2015.
 */
class FormDataSpec extends Specification {

    public void "should parse 3 parts"() {

        given:
        def boundary = "-----------------------------287032381131322"
        def testdata = """-----------------------------287032381131322
Content-Disposition: form-data; name="datafile1"; filename="r.txt"
Content-Type: text/plain

R file Contents
-----------------------------287032381131322
Content-Disposition: form-data; name="datafile2"; filename="g.txt"
Content-Type: text/plain

G file contents
-----------------------------287032381131322
Content-Disposition: form-data; name="datafile3"; filename="b.txt"
Content-Type: text/plain

B File content
-----------------------------287032381131322--"""
        when:
        def parts = new ByteUtils().splitBytes(testdata.bytes, boundary)

        then:
        parts.size() == 3
    }


    public void "should parse file values"() {

        given:
        def boundary = "-----------------------------287032381131322"
        def testdata = """-----------------------------287032381131322
Content-Disposition: form-data; name="datafile1"; filename="r.txt"
Content-Type: text/plain

R file Contents
-----------------------------287032381131322
Content-Disposition: form-data; name="datafile2"; filename="g.txt"
Content-Type: text/plain

G file contents
-----------------------------287032381131322
Content-Disposition: form-data; name="datafile3"; filename="b.txt"
Content-Type: text/plain

B File content
-----------------------------287032381131322--"""
        when:

        FormData fdata = new FormDataUtils().getFormData(testdata.bytes, "Content-Type: multipart/form-data; boundary=$boundary")

        then:
        fdata.files.size() == 3
    }

    public void "should parse form data value"() {

        given:
        def boundary = "-----------------------------287032381131322"
        def testdata = """-----------------------------287032381131322
Content-Disposition: form-data; name="datafile1"; filename="r.txt"
Content-Type: text/plain

R file Contents
-----------------------------287032381131322
Content-Disposition: form-data; name="forVariable1"

Variable content
-----------------------------287032381131322
Content-Disposition: form-data; name="datafile2"; filename="g.txt"
Content-Type: text/plain

G file contents
-----------------------------287032381131322
Content-Disposition: form-data; name="datafile3"; filename="b.txt"
Content-Type: text/plain

B File content
-----------------------------287032381131322--"""
        when:

        FormData fdata = new FormDataUtils().getFormData(testdata.bytes, "Content-Type: multipart/form-data; boundary=$boundary")

        then:
        fdata.files.size() == 3
        fdata.values.size() == 1
    }

    public void "should parse multipart mixed request"() {

        given:
        def boundary = "--AaB03x"
        def testdata = """--AaB03x
Content-Disposition: form-data; name="submit-name"

Larry
--AaB03x
Content-Disposition: form-data; name="files"
Content-Type: multipart/mixed; boundary=--BbC04y

--BbC04y
Content-Disposition: file; filename="file1.txt"
Content-Type: text/plain

... contents of file1.txt ...
--BbC04y
Content-Disposition: file; filename="file2.gif"
Content-Type: image/gif
Content-Transfer-Encoding: binary

...contents of file2.gif...
--BbC04y--
--AaB03x--"""
        when:

        FormData fdata = new FormDataUtils().getFormData(testdata.bytes, "Content-Type: multipart/form-data; boundary=$boundary")

        then:
        fdata.files.size() == 2
        fdata.values.size() == 1
        fdata.files[0].name == "file1.txt"
        fdata.files[1].name == "file2.gif"
    }

}
