package test.integration

import groovyx.net.http.HTTPBuilder
import io.github.javaconductor.gserv.GServRunner
import io.github.javaconductor.gserv.utils.Encoder
import org.junit.Ignore
import org.junit.Test

import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.Method.GET

/**
 * Created by javaConductor on 10/5/2014.
 */
class StaticDocAccessSpec {
    def baseDir = "src/integrationTest/resources/test/integration/"

    @Test
    public final void testSubFolderAccess() {
        def port = "11007"
        def dir = baseDir + "staticContentTest"
        def args = ["-p", port,
                    "-s", dir,
                    "-d", "index.html"
        ]
      // start server
        def stopFn = new GServRunner().start(args);
        Thread.sleep(1300)
        
      // request
      def http = new HTTPBuilder("http://localhost:$port/images/pictures.jpg")
      http.request(GET, TEXT) { req ->

            headers.'User-Agent' = 'Mozilla/5.0'
            response.success = { resp, Reader reader ->
                assert resp.status == 200
                //stop the server
                stopFn()
            }
            // called only for a 404 (not found) status code:
            response.'404' = { resp ->
                assert false, "Not found."
                println 'Not found'
                //stop the server
                stopFn()
            }
            response.'500' = { resp ->
                assert false, "Internal Error."
                println '500 Error'
                //stop the server
                stopFn()
            }
        }
    }


    /**
     * We need to useResourceDocs(true) and get the file from the classpath
 **/
@Ignore
    public final void testSubFolderAccessInClasspath() {
        def port = "11006"
        def dir = baseDir + "staticContentTest"
        def args = ["-p", port,
                    "-s", dir,
                    "-i", dir + "/instance/StaticContentTest.groovy",
                    "-d", "index.html"
        ]

    /// start server
    def stopFn = new GServRunner().start(args);
    def cnt = 2
      ///make request
      def http = new HTTPBuilder("http://localhost:$port/images/pictures.jpg")
        http.request(GET, TEXT) { req ->

            headers.'User-Agent' = 'Mozilla/5.0'
            response.success = { resp, Reader reader ->
                assert resp.status == 200

              --cnt;
                //stop the server
                if(!cnt)
                  stopFn()
            }
            // called only for a 404 (not found) status code:
            response.'404' = { resp ->
                assert false, "Not found."
                println 'Not found'
                //stop the server
              --cnt;
              //stop the server
              if(!cnt)
                stopFn()
            }
            response.'500' = { resp ->
                assert false, "Internal Error."
                println '500 Error'
              --cnt;
              //stop the server
              if(!cnt)
                stopFn()
            }
        }

      ///make request
      http = new HTTPBuilder("http://localhost:$port/static/pictures.jpg")
      http.request(GET, TEXT) { req ->

        headers.'User-Agent' = 'Mozilla/5.0'
        response.success = { resp, Reader reader ->
          assert resp.status == 200

          --cnt;
          //stop the server
          if(!cnt)
            stopFn()
        }
        // called only for a 404 (not found) status code:
        response.'404' = { resp ->
          assert false, "Not found."
          println 'Not found'
          --cnt;
          //stop the server
          if(!cnt)
            stopFn()
        }
        response.'500' = { resp ->
          assert false, "Internal Error."
          println '500 Error'
          --cnt;
          //stop the server
          if(!cnt)
            stopFn()
        }
      }
  }//test
}
